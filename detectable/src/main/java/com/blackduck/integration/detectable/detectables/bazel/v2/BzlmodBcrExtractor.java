package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseShowRepoToUrlCandidates;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.ShowRepoExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.parse.GithubUrlParser;
import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Extracts BCR (Bazel Central Registry) module dependencies using {@code bazel mod graph --output json}
 * (Bazel 7.1+, BZLMOD mode only) and produces a {@link DependencyGraph} that preserves the true
 * direct/transitive tree structure.
 *
 * <p>This is the dedicated BCR extraction path used when BZLMOD mode is detected and the Bazel
 * version is >= 7.1. It replaces the flat HTTP_ARCHIVE pipeline for BCR-managed modules.
 * Maven (rules_jvm_external) and Haskell pipelines run separately and remain flat.
 *
 * <p><b>Overview of the extraction flow:</b>
 * <ol>
 *   <li>Run {@code bazel mod graph --output json} to get the full module dependency tree.</li>
 *   <li>Parse the JSON into a {@link ModuleGraph} that holds direct keys
 *       and parent→children edges.</li>
 *   <li>For each unique module, run {@code bazel mod show_repo @<name>} (batched where possible)
 *       to get its source URL, then parse the GitHub URL and create a {@link Dependency}.</li>
 *   <li>Build the graph: direct modules go under root; transitives go under their parents.</li>
 * </ol>
 *
 * <p><b>What is NOT covered here:</b> non-BCR http_archive repos (private or custom rules) that
 * do not appear in {@code bazel mod graph} output. Those are logged at WARN so users know what
 * is missing.
 */
public class BzlmodBcrExtractor {
    private static final Logger logger = LoggerFactory.getLogger(BzlmodBcrExtractor.class);

    // Prefix used in GitHub archive URLs to indicate a tag ref (e.g., refs/tags/v1.2.3)
    private static final String REFS_TAGS_PREFIX = "refs/tags/";

    // Pattern for the target-scoped library query (same as the HTTP_ARCHIVE pipeline)
    private static final String LIBRARY_RULE_PATTERN = ".*library";
    // Infrastructure / toolchain repo exclusion is centralized in BazelInfrastructureModules.

    private final BazelCommandExecutor bazelCmd;
    private final BazelVersion bazelVersion;
    // The Bazel target being scanned (e.g., //java/src/...:client-combined).
    // Used to scope the BCR BOM to only modules the target actually fetches.
    private final String bazelTarget;
    // User-supplied query options (detect.bazel.query.options). Applied to the target-scope library
    // query so it is consistent with the HTTP_ARCHIVE pipeline and HttpFamilyProber, which both honor
    // these options. Keeping the args identical also lets BazelCommandExecutor memoize/reuse the result.
    private final List<String> queryOptions;
    // Stateless helpers — created by default constructors; can be injected for testing
    private final GithubUrlParser githubUrlParser;
    private final IntermediateStepParseShowRepoToUrlCandidates urlCandidateParser;
    // Shared show_repo execution mechanics (batching, block splitting, per-candidate fallback)
    private final ShowRepoExecutor showRepoExecutor;
    // Populated during extractGraph(); used by callers to avoid re-adding BCR deps flat via other pipelines.
    private final Set<ExternalId> resolvedExternalIds = new LinkedHashSet<>();

    /**
     * Backward-compatible constructor with no user query options (used by unit tests).
     */
    public BzlmodBcrExtractor(BazelCommandExecutor bazelCmd, BazelVersion bazelVersion, String bazelTarget) {
        this(bazelCmd, bazelVersion, bazelTarget, Collections.emptyList());
    }

    public BzlmodBcrExtractor(BazelCommandExecutor bazelCmd, BazelVersion bazelVersion, String bazelTarget, List<String> queryOptions) {
        this(bazelCmd, bazelVersion, bazelTarget, queryOptions,
            new GithubUrlParser(),
            new IntermediateStepParseShowRepoToUrlCandidates(),
            new ShowRepoExecutor(bazelCmd));
    }

    /**
     * Full-injection constructor. Accepts all collaborators explicitly, enabling unit tests that
     * exercise classification and URL-parsing logic without spawning a real Bazel process.
     * Production code uses the shorter constructors above, which supply default implementations.
     */
    public BzlmodBcrExtractor(BazelCommandExecutor bazelCmd, BazelVersion bazelVersion, String bazelTarget,
                               List<String> queryOptions,
                               GithubUrlParser githubUrlParser,
                               IntermediateStepParseShowRepoToUrlCandidates urlCandidateParser,
                               ShowRepoExecutor showRepoExecutor) {
        this.bazelCmd = bazelCmd;
        this.bazelVersion = bazelVersion;
        this.bazelTarget = bazelTarget;
        this.queryOptions = queryOptions != null ? queryOptions : Collections.emptyList();
        this.githubUrlParser = githubUrlParser;
        this.urlCandidateParser = urlCandidateParser;
        this.showRepoExecutor = showRepoExecutor;
    }

    /**
     * Returns the set of ExternalIds resolved by the last {@link #extractGraph()} call.
     * Callers use this to suppress re-adding BCR-classified deps as flat root children
     * from other pipelines (e.g., HTTP_ARCHIVE), which would overwrite the direct/transitive
     * classification the BCR extractor established.
     */
    public Set<ExternalId> getResolvedExternalIds() {
        return Collections.unmodifiableSet(resolvedExternalIds);
    }

    /**
     * Main entry point. Runs the full BCR extraction and returns a {@link DependencyGraph}
     * with direct/transitive edges populated. Returns an empty graph on failure — callers
     * should check the log output to understand what went wrong.
     */
    public DependencyGraph extractGraph() {
        // Step 1 — get the full module dependency tree
        logger.debug("BZLMOD BCR: running 'bazel mod graph --output json' to discover module dependency tree");
        List<String> modGraphArgs = BazelQueryBuilder.mod().graph().withOutputJson().build();
        Optional<String> modGraphOutput = bazelCmd.executeModCommandToString(modGraphArgs);

        if (!modGraphOutput.isPresent()) {
            logger.warn("Bazel module graph returned no output; dependency extraction will be skipped");
            return new BasicDependencyGraph();
        }

        BzlmodGraphJsonParser parser = new BzlmodGraphJsonParser();
        ModuleGraph tree = parser.parseModuleGraph(modGraphOutput.get());

        Set<String> allKeys = tree.getAllModuleKeys();
        if (allKeys.isEmpty()) {
            logger.warn("Bazel module graph contained no recognizable entries; dependency extraction will be skipped");
            return new BasicDependencyGraph();
        }
        logger.debug("BZLMOD BCR: module graph has {} direct dep(s) and {} total unique module(s)",
            tree.directModuleKeys.size(), allKeys.size());

        // Step 1b — load the repo mapping once.
        // BzlmodRepoMappingResolver handles two label-format problems in one shot:
        //   (a) canonical suffix instability: "+" pre-7.5, "~" in 7.5+ — detected from mapping values
        //   (b) repo_name aliases: @com_google_protobuf → protobuf via the forward map
        // If the command fails the resolver degrades gracefully (see BzlmodRepoMappingResolver).
        BzlmodRepoMappingResolver resolver = BzlmodRepoMappingResolver.load(bazelCmd);

        // Step 1c — narrow the mod graph to only modules the target actually fetches.
        // This keeps the BCR path consistent with all other Bazel pipelines which are target-scoped
        // via 'bazel query deps(//target)'. We reuse the exact same library query (same args, including
        // detect.bazel.query.options) that HttpFamilyProber runs during probing; BazelCommandExecutor
        // memoizes read-only query results for the extraction, so this is served in-process rather than
        // re-executing Bazel.
        //
        // The whitelist check bridges two different Bazel naming planes:
        //   bazel query output  → labels like  @@abseil-cpp~//absl/strings:strings
        //   bazel mod graph     → keys  like   abseil-cpp@20240116.2
        // resolveLabel() (called inside getTargetScopedModuleNames) reduces a query label to a plain
        // BCR module name — by stripping the canonical suffix (@@name~ → name) or resolving a
        // repo_name alias via the dump_repo_mapping forward map (@alias → module_name).
        // extractName() reduces a mod graph key to the same plain module name by dropping @version.
        // The filter is therefore: resolveLabel(query_label) ∈ { extractName(mod_graph_key) }.
        // This holds for standard BCR modules because Bazel derives canonical repo names directly
        // from module names (module name + version-specific suffix). It does NOT hold for module
        // extension sub-repos (@@mod++ext+subrepo), which have no mod graph entry — resolveLabel
        // returns empty for those and they are absent from targetModuleNames, falling through to
        // separate pipelines (MAVEN_INSTALL for JVM deps managed by rules_jvm_external, etc.).
        Set<String> targetModuleNames = getTargetScopedModuleNames(resolver);
        if (!targetModuleNames.isEmpty()) {
            Set<String> filtered = new LinkedHashSet<>();
            for (String key : allKeys) {
                if (targetModuleNames.contains(BzlmodGraphJsonParser.extractName(key))) {
                    filtered.add(key);
                }
            }
            logger.debug("BZLMOD BCR: target-scoped filter: {} of {} module(s) in scope for '{}', {} pruned",
                filtered.size(), allKeys.size(), bazelTarget, allKeys.size() - filtered.size());
            allKeys = filtered;
        } else {
            logger.warn("Target-specific dependency filter unavailable — all {} module(s) in the project will be reported",
                allKeys.size());
        }

        // Step 1d — remove excluded modules (toolchain infrastructure, build rules, etc.) before
        // any show_repo call. This must happen here — after target-scoped filtering — so excluded
        // modules never reach the batch or per-module show_repo calls.
        //
        // WHY THIS MATTERS FOR CORRECTNESS AND PERFORMANCE:
        // show_repo is batched: a single bad argument (e.g. @@platforms+ for a no-suffix embedded
        // module) causes Bazel to reject the ENTIRE batch with exit code 2 and no stdout.
        // Every module in the batch then falls back to per-module calls — eliminating the
        // performance benefit of batching for the entire scan.
        //
        // Modules like 'platforms' are already in EXCLUDED_REPO_PREFIXES by design (they are
        // Bazel toolchain constraint infrastructure, not software components). The exclusion
        // was applied at the target-scope query check (Step 1c) but previously not here.
        // This step makes the exclusion consistent end-to-end.
        Set<String> keysForResolution = new LinkedHashSet<>();
        for (String key : allKeys) {
            String name = BzlmodGraphJsonParser.extractName(key);
            if (isExcludedModuleName(name)) {
                logger.debug("BZLMOD BCR: skipping '{}' — build infrastructure, not a software component", key);
            } else {
                keysForResolution.add(key);
            }
        }

        // Step 2 — map each module key to a Dependency via show_repo
        Map<String, Dependency> moduleKeyToDep = resolveModules(keysForResolution, resolver);
        int excludedCount   = allKeys.size() - keysForResolution.size();
        int unresolvedCount = keysForResolution.size() - moduleKeyToDep.size();
        logger.debug("BZLMOD BCR: show_repo summary — {} fetched, {} failed, {} skipped (build infrastructure)",
            moduleKeyToDep.size(), unresolvedCount, excludedCount);

        // Step 3 — build the graph preserving the direct/transitive tree structure
        return buildGraph(tree, moduleKeyToDep);
    }

    // -------------------------------------------------------------------------
    // Step 2: resolve module keys to Dependencies
    // -------------------------------------------------------------------------

    /**
     * Resolves each module key to a {@link Dependency} by calling {@code bazel mod show_repo}
     * (via {@link ShowRepoExecutor#resolveWithFallback}) and parsing the GitHub URL from the output.
     *
     * <p>Batch+fallback orchestration is delegated to {@link ShowRepoExecutor#resolveWithFallback},
     * which tries a single batched call first and retries only missing modules per-module.
     *
     * <p>Excluded modules (infrastructure / toolchain repos) must be filtered from
     * {@code moduleKeys} by the caller before invoking this method — see Step 1d in
     * {@link #extractGraph()}.
     *
     * <p>If a module cannot be resolved (e.g. it uses {@code git_override} or
     * {@code local_path_override} with a non-standard canonical name), it is skipped with a
     * WARN. The HTTP_ARCHIVE pipeline may still capture it via {@code bazel query}.
     */
    private Map<String, Dependency> resolveModules(Set<String> moduleKeys, BzlmodRepoMappingResolver resolver) {
        Map<String, String> showRepoOutputByKey = showRepoExecutor.resolveWithFallback(moduleKeys, resolver);

        Map<String, Dependency> result = new LinkedHashMap<>();
        for (String moduleKey : moduleKeys) {
            String showRepoOutput = showRepoOutputByKey.get(moduleKey);
            if (showRepoOutput == null || showRepoOutput.trim().isEmpty()) {
                // Module could not be resolved — likely uses git_override or local_path_override
                // with a non-standard canonical name, or is not a standard BCR module.
                // The HTTP_ARCHIVE pipeline may still capture it via bazel query.
                logger.warn("Module '{}' could not be resolved — it may use a local override. " +
                    "It will not be included in these scan results. " +
                    "It may still be found via other scan methods.",
                    moduleKey);
                continue;
            }
            Dependency dep = urlOutputToDependency(moduleKey, showRepoOutput);
            if (dep != null) {
                result.put(moduleKey, dep);
                resolvedExternalIds.add(dep.getExternalId());
            }
        }
        return result;
    }

    /**
     * Converts the {@code bazel mod show_repo} output for one module into a Forge.GITHUB
     * {@link Dependency}. Returns {@code null} if no parseable GitHub URL is found (with WARN).
     *
     * <p>URL extraction delegates to {@link IntermediateStepParseShowRepoToUrlCandidates}
     * (reusing the same logic as the existing HTTP_ARCHIVE pipeline).
     * Version is taken from the GitHub URL if available; falls back to the module key version.
     */
    private Dependency urlOutputToDependency(String moduleKey, String showRepoOutput) {
        List<String> urlCandidates;
        try {
            urlCandidates = urlCandidateParser.process(Collections.singletonList(showRepoOutput));
        } catch (Exception e) {
            logger.warn("BZLMOD BCR: URL extraction failed for '{}': {}", moduleKey, e.getMessage());
            return null;
        }

        String moduleVersion = BzlmodGraphJsonParser.extractVersion(moduleKey);

        for (String urlCandidate : urlCandidates) {
            try {
                String organization = githubUrlParser.parseOrganization(urlCandidate);
                String repo = githubUrlParser.parseRepo(urlCandidate);
                String parsedVersion = githubUrlParser.parseVersion(urlCandidate);
                // Normalize refs/tags/v1.2.3 → v1.2.3 (same logic as FinalStepTransformGithubUrl)
                if (parsedVersion != null && parsedVersion.startsWith(REFS_TAGS_PREFIX)) {
                    parsedVersion = parsedVersion.substring(REFS_TAGS_PREFIX.length());
                }
                // Prefer the URL-parsed version; fall back to the version in the module key
                String resolvedVersion = (parsedVersion != null && !parsedVersion.isEmpty())
                    ? parsedVersion
                    : moduleVersion;
                logger.debug("BZLMOD BCR: resolved '{}' → github:{}/{} version:{}", moduleKey, organization, repo, resolvedVersion);
                return Dependency.FACTORY.createNameVersionDependency(Forge.GITHUB, organization + "/" + repo, resolvedVersion);
            } catch (MalformedURLException e) {
                // Not a GitHub URL — try the next candidate
            }
        }

        // No GitHub URL found — log all raw URLs so users can investigate
        if (!urlCandidates.isEmpty()) {
            logger.warn("Module '{}' was found but its source URL is not a supported GitHub URL — it will not appear in the scan results. " +
                "Raw URL(s): {}. Consider running a signature scan for this component.",
                moduleKey, urlCandidates);
        } else {
            logger.warn("Module '{}' was found but no source URL could be extracted — it will not appear in the scan results.", moduleKey);
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Step 3: build DependencyGraph with tree edges
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link DependencyGraph} from the resolved module map and the parsed tree structure.
     *
     * <p>Modules in {@code tree.directModuleKeys} are added as root children. Their transitive
     * deps are linked via {@code addChildWithParent}. Diamond dependencies are handled correctly:
     * every parent→child edge is added (so the graph reflects all paths), but subtree recursion
     * is guarded by a visited set so each module's own children are visited exactly once.
     *
     * <p>Direct module keys are pre-populated in the visited set so that if a direct dep also
     * appears as a transitive dep of another direct dep, the two traversals do not duplicate work.
     */
    private DependencyGraph buildGraph(ModuleGraph tree,
                                       Map<String, Dependency> moduleKeyToDep) {
        DependencyGraph graph = new BasicDependencyGraph();

        // Pre-populate with direct keys to prevent double-traversal when a direct dep also
        // appears as a transitive dep of another direct dep. The outer loop still processes
        // each direct dep explicitly (adding it to root and recursing into its subtree once).
        Set<String> recursed = new HashSet<>(tree.directModuleKeys);
        int directCount = 0;

        for (String directKey : tree.directModuleKeys) {
            Dependency dep = moduleKeyToDep.get(directKey);
            if (dep == null) {
                logger.debug("BZLMOD BCR: no resolved dep for direct module '{}', skipping", directKey);
                continue;
            }
            graph.addChildToRoot(dep);
            directCount++;
            recurseChildren(graph, directKey, dep, tree, moduleKeyToDep, recursed);
        }

        logger.debug("BZLMOD BCR: dependency tree — {} direct, {} transitive Bazel module(s)",
            directCount, moduleKeyToDep.size() - directCount);
        logger.debug("BZLMOD BCR: structured direct/transitive classification complete.");
        return graph;
    }

    // -------------------------------------------------------------------------
    // Target-scoped filtering helpers
    // -------------------------------------------------------------------------

    /**
     * Runs {@code bazel query 'kind(.*library, deps(bazelTarget))'} and returns the set of
     * bare BCR module names that the target actually depends on. The result is used to filter
     * the full project-scoped mod graph down to only target-relevant modules.
     *
     * <p>Returns an empty set when the query fails or produces no results — callers fall back
     * to the full project-scoped mod graph with a WARN log.
     *
     * <p>Label resolution is delegated to {@link BzlmodRepoMappingResolver#resolveLabel}, which handles:
     * <ul>
     *   <li>{@code @@name~//...} or {@code @@name+//...} (canonical BCR) → strip suffix → module name</li>
     *   <li>{@code @alias//...} (apparent name, e.g. from {@code repo_name} override) → map lookup → module name</li>
     *   <li>{@code @@module++ext+subrepo//...} (module extension sub-repo) → skipped (empty Optional)</li>
     * </ul>
     *
     * <p>The same query (same args, including {@code detect.bazel.query.options}) is issued by
     * {@code HttpFamilyProber} during probing; because {@link BazelCommandExecutor} memoizes
     * read-only query results for the extraction, this call is served from that in-process cache
     * rather than re-executing Bazel.
     */
    private Set<String> getTargetScopedModuleNames(BzlmodRepoMappingResolver resolver) {
        logger.debug("BZLMOD BCR: querying target-scoped repos via 'bazel query kind(.*library, deps({}))'", bazelTarget);
        List<String> queryArgs = BazelQueryBuilder.query()
            .kind(LIBRARY_RULE_PATTERN, BazelQueryBuilder.deps(bazelTarget))
            .withOptions(queryOptions)
            .build();

        Optional<String> queryOutput;
        try {
            queryOutput = bazelCmd.executeQueryToString(queryArgs);
        } catch (ExecutableFailedException e) {
            logger.warn("BZLMOD BCR: target-scoped query failed ({}); will use full project-scoped mod graph", e.getMessage());
            return Collections.emptySet();
        }

        if (!queryOutput.isPresent() || queryOutput.get().trim().isEmpty()) {
            logger.warn("BZLMOD BCR: target-scoped query produced no output; will use full project-scoped mod graph");
            return Collections.emptySet();
        }

        Set<String> moduleNames = new LinkedHashSet<>();
        for (String line : queryOutput.get().split("\\r?\\n")) {
            String label = line.trim();
            if (!BazelLabel.parse(label).isRepoLabel()) {
                continue;
            }
            // Delegate all label-format complexity to the resolver:
            // suffix detection (+ vs ~), repo_name alias resolution, ++ sub-repo exclusion.
            Optional<String> moduleName = resolver.resolveLabel(label);
            if (!moduleName.isPresent() || isExcludedModuleName(moduleName.get())) {
                continue;
            }
            moduleNames.add(moduleName.get());
        }

        logger.debug("BZLMOD BCR: target-scoped query found {} module name(s)", moduleNames.size());
        if (logger.isDebugEnabled()) {
            logger.debug("BZLMOD BCR: target-scoped module names: {}", moduleNames);
        }
        return moduleNames;
    }

    /**
     * Returns true if the repo/module name should be excluded from BCR scope checks.
     * Delegates to the shared {@link BazelInfrastructureModules} source of truth.
     */
    private boolean isExcludedModuleName(String name) {
        return BazelInfrastructureModules.isInfrastructure(name);
    }

    /**
     * Recursively adds child dependencies under {@code parentDep} in the graph.
     * Edges are always added (so multiple parents for a shared dep are all recorded).
     * Recursion into a child's subtree is guarded by {@code recursed} to avoid
     * re-traversing already-visited subtrees (handles diamond dependencies).
     */
    private void recurseChildren(DependencyGraph graph,
                                  String parentKey,
                                  Dependency parentDep,
                                  ModuleGraph tree,
                                  Map<String, Dependency> moduleKeyToDep,
                                  Set<String> recursed) {
        List<String> children = tree.childrenByModuleKey.getOrDefault(parentKey, Collections.<String>emptyList());
        for (String childKey : children) {
            Dependency childDep = moduleKeyToDep.get(childKey);
            if (childDep == null) {
                logger.debug("BZLMOD BCR: no resolved dep for child '{}' (parent: {}), skipping edge", childKey, parentKey);
                continue;
            }
            // Always add the edge — diamonds have multiple parents and all parent→child edges matter
            graph.addChildWithParent(childDep, parentDep);
            // Recurse into this child's subtree only once; subsequent encounters are edges-only
            if (recursed.add(childKey)) {
                recurseChildren(graph, childKey, childDep, tree, moduleKeyToDep, recursed);
            }
        }
    }
}
