package com.blackduck.integration.detectable.detectables.bazel.v2;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.BazelCommandExecutor;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseShowRepoToUrlCandidates;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.parse.GithubUrlParser;
import com.blackduck.integration.detectable.detectables.bazel.query.BazelQueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
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
    // Separator between individual repo blocks in batched show_repo output.
    // Canonical form (@@name+) is used — see resolveModules() for why.
    private static final String REPO_BLOCK_SEPARATOR = "## @@";
    // Suffix appended to a module name to form its Bazel canonical repo name (e.g., "protobuf+")
    private static final String CANONICAL_SUFFIX = "+";

    // Constants for target-scoped query (same logic as the HTTP_ARCHIVE pipeline in Pipelines.java)
    private static final String LIBRARY_RULE_PATTERN = ".*library";
    // Strips one or more leading @ signs from a label (handles both @apparent and @@canonical forms)
    private static final String STRIP_LEADING_ATS_REGEX = "^@+";
    // Strips the //path:target suffix to leave only the repo name
    private static final String STRIP_REPO_PATH_REGEX = "//.*";
    // Repo name prefixes that are Bazel toolchain / internal repos — excluded from BCR scope check.
    // Mirrors the exclusion list in HttpFamilyProber and Pipelines.java.
    private static final Set<String> EXCLUDED_REPO_PREFIXES = new HashSet<>(Arrays.asList(
        "bazel_tools", "local_config_", "remotejdk", "platforms",
        "rules_python", "rules_java", "rules_cc",
        "maven", "unpinned_maven", "rules_jvm_external"
    ));

    private final BazelCommandExecutor bazelCmd;
    private final BazelVersion bazelVersion;
    // The Bazel target being scanned (e.g., //java/src/...:client-combined).
    // Used to scope the BCR BOM to only modules the target actually fetches.
    private final String bazelTarget;
    // Stateless helpers — instantiated internally, no external injection needed
    private final GithubUrlParser githubUrlParser;
    private final IntermediateStepParseShowRepoToUrlCandidates urlCandidateParser;
    // Populated during extractGraph(); used by callers to avoid re-adding BCR deps flat via other pipelines.
    private final Set<ExternalId> resolvedExternalIds = new LinkedHashSet<>();

    public BzlmodBcrExtractor(BazelCommandExecutor bazelCmd, BazelVersion bazelVersion, String bazelTarget) {
        this.bazelCmd = bazelCmd;
        this.bazelVersion = bazelVersion;
        this.bazelTarget = bazelTarget;
        this.githubUrlParser = new GithubUrlParser();
        this.urlCandidateParser = new IntermediateStepParseShowRepoToUrlCandidates();
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
        logger.info("BZLMOD BCR: running 'bazel mod graph --output json' to discover module dependency tree");
        List<String> modGraphArgs = BazelQueryBuilder.mod().graph().withOutputJson().build();
        Optional<String> modGraphOutput = bazelCmd.executeModCommandToString(modGraphArgs);

        if (!modGraphOutput.isPresent()) {
            logger.warn("BZLMOD BCR: 'bazel mod graph --output json' produced no output; returning empty graph");
            return new BasicDependencyGraph();
        }

        BzlmodGraphJsonParser parser = new BzlmodGraphJsonParser();
        ModuleGraph tree = parser.parseModuleGraph(modGraphOutput.get());

        Set<String> allKeys = tree.getAllModuleKeys();
        if (allKeys.isEmpty()) {
            logger.warn("BZLMOD BCR: module graph contained no parseable module keys; returning empty graph");
            return new BasicDependencyGraph();
        }
        logger.info("BZLMOD BCR: module graph has {} direct dep(s) and {} total unique module(s)",
            tree.directModuleKeys.size(), allKeys.size());

        // Step 1b — narrow the mod graph to only modules the target actually fetches.
        // This keeps the BCR path consistent with all other Bazel pipelines which are target-scoped
        // via 'bazel query deps(//target)'. We reuse the same library query the HTTP_ARCHIVE pipeline
        // runs, so Bazel serves it from its analysis cache at no extra cost.
        Set<String> targetModuleNames = getTargetScopedModuleNames();
        if (!targetModuleNames.isEmpty()) {
            Set<String> filtered = new LinkedHashSet<>();
            for (String key : allKeys) {
                if (targetModuleNames.contains(BzlmodGraphJsonParser.extractName(key))) {
                    filtered.add(key);
                }
            }
            logger.info("BZLMOD BCR: target-scoped filter: {} of {} module(s) in scope for '{}', {} pruned",
                filtered.size(), allKeys.size(), bazelTarget, allKeys.size() - filtered.size());
            allKeys = filtered;
        } else {
            logger.warn("BZLMOD BCR: target-scoped filter unavailable — reporting full project-scoped mod graph ({} modules)",
                allKeys.size());
        }

        // Step 2 — map each module key to a Dependency via show_repo
        Map<String, Dependency> moduleKeyToDep = resolveModules(allKeys);
        logger.info("BZLMOD BCR extraction: {} module(s) resolved, {} skipped (see WARN above for details)",
            moduleKeyToDep.size(), allKeys.size() - moduleKeyToDep.size());

        // Step 3 — build the graph preserving the direct/transitive tree structure
        return buildGraph(tree, moduleKeyToDep);
    }

    // -------------------------------------------------------------------------
    // Step 2: resolve module keys to Dependencies
    // -------------------------------------------------------------------------

    /**
     * Resolves each module key to a {@link Dependency} by calling
     * {@code bazel mod show_repo @@<name>+} and parsing the GitHub URL from the output.
     *
     * <p>We use the <em>canonical</em> repo name form {@code @@<name>+} rather than the
     * apparent name {@code @<name>}. The distinction matters for pure transitive deps:
     * <ul>
     *   <li>{@code @protobuf} — apparent name, looked up in the <em>root module's</em> repo
     *       mapping. Only works for modules explicitly declared in the root's {@code MODULE.bazel}.
     *       Pure transitives are absent from the root's mapping → "unused modules" error.</li>
     *   <li>{@code @@protobuf+} — canonical name, a globally unique identifier that Bazel
     *       resolves directly without any repo-mapping lookup. Works for every BCR module in
     *       the graph, including pure transitives that the root never declared.</li>
     * </ul>
     *
     * <p>If a module cannot be resolved (e.g. it uses {@code git_override} or
     * {@code local_path_override} with a non-standard canonical name), it is skipped with a
     * WARN. The HTTP_ARCHIVE pipeline may still capture it via {@code bazel query}.
     */
    private Map<String, Dependency> resolveModules(Set<String> moduleKeys) {
        // Build @@<name>+ canonical args and the reverse mapping key → arg for post-processing
        List<String> repoArgs = new ArrayList<>();
        Map<String, String> moduleKeyToRepoArg = new LinkedHashMap<>();
        for (String moduleKey : moduleKeys) {
            String name = BzlmodGraphJsonParser.extractName(moduleKey);
            // @@name+ is the canonical form: bypasses root module's repo mapping restriction,
            // allowing pure transitives to be resolved just like direct deps.
            String repoArg = "@@" + name + CANONICAL_SUFFIX;
            repoArgs.add(repoArg);
            moduleKeyToRepoArg.put(moduleKey, repoArg);
        }

        // Attempt a single batched show_repo call (Bazel 7.1+ supports it; we are always 7.1+ here)
        Map<String, String> showRepoByKey = tryBatchedShowRepo(moduleKeys, repoArgs);
        if (showRepoByKey.isEmpty()) {
            logger.info("BZLMOD BCR: batched show_repo returned no results; falling back to per-module calls");
            showRepoByKey = runPerModuleShowRepo(moduleKeys, moduleKeyToRepoArg);
        } else {
            logger.debug("BZLMOD BCR: batched show_repo resolved {} of {} module(s)", showRepoByKey.size(), moduleKeys.size());
        }

        // Convert show_repo output to Dependencies
        Map<String, Dependency> result = new LinkedHashMap<>();
        for (String moduleKey : moduleKeys) {
            String showRepoOutput = showRepoByKey.get(moduleKey);
            if (showRepoOutput == null || showRepoOutput.trim().isEmpty()) {
                // Module could not be resolved — likely uses git_override or local_path_override
                // with a non-standard canonical name, or is not a standard BCR module.
                // The HTTP_ARCHIVE pipeline may still capture it via bazel query.
                logger.warn("BZLMOD BCR: show_repo produced no output for '{}' — " +
                    "module may use a git_override or local_path_override with a non-standard canonical name. " +
                    "It will not be included in the BCR BOM. " +
                    "It may still appear via the HTTP_ARCHIVE pipeline.",
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
     * Attempts a single batched {@code bazel mod show_repo @@name1+ @@name2+ ...} and maps the
     * split output blocks back to module keys via the {@code ## @@name+:} header in each block.
     * Returns an empty map if the batch fails, returns no stdout, or cannot be split.
     *
     * <p>The canonical {@code @@name+} form is used so that pure transitive modules resolve
     * without a repo-mapping lookup. See {@link #resolveModules} for the full explanation.
     */
    private Map<String, String> tryBatchedShowRepo(Set<String> moduleKeys, List<String> repoArgs) {
        if (repoArgs.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            List<String> batchCmd = BazelQueryBuilder.mod().showRepoRawBatch(repoArgs).build();
            Optional<String> output = bazelCmd.executeModCommandToString(batchCmd);
            if (!output.isPresent() || output.get().trim().isEmpty()) {
                return Collections.emptyMap();
            }

            // Each repo block starts with "## @@<name>+:" — split on that boundary
            String[] parts = output.get().split("(?=" + REPO_BLOCK_SEPARATOR + ")");
            Map<String, String> blockByRepoName = new LinkedHashMap<>();
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty() && trimmed.startsWith(REPO_BLOCK_SEPARATOR)) {
                    // Header is "## @@<name>+:" — extract the module name between "@@" and "+:"
                    int colonIdx = trimmed.indexOf(':');
                    if (colonIdx > REPO_BLOCK_SEPARATOR.length()) {
                        // e.g. "## @@protobuf+:" → rawName = "protobuf+"
                        String rawName = trimmed.substring(REPO_BLOCK_SEPARATOR.length(), colonIdx);
                        // Strip the trailing canonical "+" suffix to get the plain module name
                        String moduleName = rawName.endsWith(CANONICAL_SUFFIX)
                            ? rawName.substring(0, rawName.length() - CANONICAL_SUFFIX.length())
                            : rawName;
                        blockByRepoName.put(moduleName, trimmed);
                    }
                }
            }

            // Map module keys to the block that corresponds to their extracted module name
            Map<String, String> result = new LinkedHashMap<>();
            for (String moduleKey : moduleKeys) {
                String name = BzlmodGraphJsonParser.extractName(moduleKey);
                String block = blockByRepoName.get(name);
                if (block != null) {
                    result.put(moduleKey, block);
                }
            }
            return result;
        } catch (Exception e) {
            logger.debug("BZLMOD BCR: batched show_repo failed with exception: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Runs {@code bazel mod show_repo @@<name>+} individually for each module key.
     * Used as a fallback when the batched call fails or returns no results.
     */
    private Map<String, String> runPerModuleShowRepo(Set<String> moduleKeys, Map<String, String> moduleKeyToRepoArg) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String moduleKey : moduleKeys) {
            String repoArg = moduleKeyToRepoArg.get(moduleKey);
            List<String> showRepoArgs = BazelQueryBuilder.mod().showRepoRaw(repoArg).build();
            Optional<String> output = bazelCmd.executeModCommandToString(showRepoArgs);
            if (output.isPresent() && !output.get().trim().isEmpty()) {
                result.put(moduleKey, output.get());
            } else {
                logger.debug("BZLMOD BCR: per-module show_repo produced no output for '{}'", moduleKey);
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
            logger.warn("BZLMOD BCR: no GitHub URL found for '{}' — raw URL(s): {}. " +
                "This component is not included in the BOM. " +
                "Consider running signature scan on this path or consulting the KB team about forge support.",
                moduleKey, urlCandidates);
        } else {
            logger.warn("BZLMOD BCR: no URLs found in show_repo output for '{}'. " +
                "This component is not included in the BOM.", moduleKey);
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

        logger.info("BZLMOD BCR graph built: {} direct dep(s), {} total module(s) resolved",
            directCount, moduleKeyToDep.size());
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
     * <p>Label formats handled:
     * <ul>
     *   <li>{@code @@name+//...} (canonical BCR) → strip {@code @@} and trailing {@code +} → module name</li>
     *   <li>{@code @apparentname//...} (apparent, no {@code repo_name} override) → strip {@code @} → module name</li>
     *   <li>{@code @@module++ext+subrepo//...} (module extension sub-repo) → skipped (contains {@code ++})</li>
     * </ul>
     *
     * <p>The same query is run by the HTTP_ARCHIVE pipeline, so Bazel's analysis cache serves it
     * without a full re-evaluation — effectively zero extra cost.
     */
    private Set<String> getTargetScopedModuleNames() {
        logger.info("BZLMOD BCR: querying target-scoped repos via 'bazel query kind(.*library, deps({}))'", bazelTarget);
        List<String> queryArgs = BazelQueryBuilder.query()
            .kind(LIBRARY_RULE_PATTERN, BazelQueryBuilder.deps(bazelTarget))
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
            if (!label.startsWith("@")) {
                continue;
            }
            // Strip leading @ or @@ and the //path:target suffix to get the bare repo name
            String name = label.replaceFirst(STRIP_LEADING_ATS_REGEX, "").replaceFirst(STRIP_REPO_PATH_REGEX, "");
            if (name.isEmpty()) {
                continue;
            }
            // Module extension sub-repos use @@ with ++ in the canonical name (e.g., rules_jvm_external++maven+guava).
            // These don't appear as module keys in bazel mod graph — skip them.
            if (name.contains("++")) {
                continue;
            }
            // Strip trailing + to convert canonical BCR form to module name (e.g., "protobuf+" → "protobuf")
            if (name.endsWith(CANONICAL_SUFFIX)) {
                name = name.substring(0, name.length() - 1);
            }
            if (name.isEmpty() || isExcludedModuleName(name)) {
                continue;
            }
            moduleNames.add(name);
        }

        logger.info("BZLMOD BCR: target-scoped filter set has {} candidate module name(s)", moduleNames.size());
        if (logger.isDebugEnabled()) {
            logger.debug("BZLMOD BCR: target-scoped module names: {}", moduleNames);
        }
        return moduleNames;
    }

    /**
     * Returns true if the repo/module name should be excluded from BCR scope checks.
     * Mirrors the exclusion list in HttpFamilyProber and Pipelines.java.
     */
    private boolean isExcludedModuleName(String name) {
        for (String prefix : EXCLUDED_REPO_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
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
