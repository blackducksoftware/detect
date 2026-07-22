# Bazel support

## Related properties

[Bazel tool properties](../properties/detectors/bazel.md)

## Bazel tool Overview

[detect_product_short] provides limited support for Bazel projects using the updated Bazel tool. The Bazel tool is mode-agnostic and works seamlessly with both BZLMOD (MODULE.bazel) and WORKSPACE-based projects.

The Bazel tool discovers dependencies from the following dependency sources:

- *maven_jar* - Legacy Maven dependencies
- *maven_install* - Modern Maven dependencies via rules_jvm_external
- *haskell_cabal_library* - Haskell Cabal packages
- *http_archive* - Source archives from HTTP, Git, and other sources

The Bazel tool discovers library dependencies that have a GitHub released artifact location (URL) specified in an *http_archive*, *go_repository*, or *git_repository* rule.

### Features

- **Mode-Agnostic:** Automatically detects whether your project uses BZLMOD or WORKSPACE and adapts accordingly
- **Mode Detection Strategy:** Uses `bazel mod graph` to detect BZLMOD support; falls back to WORKSPACE mode for older Bazel versions (< 6.0)
- **Automatic Pipeline Selection:** Probes your Bazel dependency graph to determine which dependency sources are present and automatically runs the correct extraction pipelines
- **HTTP Detection Strategy:**
  - For BZLMOD projects: Uses `bazel mod show_repo` to robustly extract dependency URLs from external repositories
  - For WORKSPACE projects: Uses XML parsing to extract URLs from repository rules
- **HTTP Detection Strategy:** Small/medium targets (≤150 external repos) are fully probed for precise detection. Large projects (>150 repos) automatically enable the HTTP pipeline to ensure completeness without probing overhead.

### Supported Bazel Versions

- Bazel 8.x: Fully supported
- Bazel 7.x: Fully supported; Bzlmod is default and `bazel mod` commands are stable
- Bazel 6.4–6.9: Supported; Bzlmod supported with mature `mod` subcommands
- Bazel 6.0–6.3: Limited support for Bzlmod (see note below)
- Bazel 5.x and earlier: WORKSPACE mode only; Bzlmod features unavailable
- Minimum version for Haskell Cabal: Bazel 2.1.0+

<note type="important">
In Bazel 6.x, Bzlmod analysis often requires the `--enable_bzlmod` flag on query/cquery commands.
The `bazel mod` subcommands (for example, `mod show_repo`) were introduced around 6.3 and are not consistently available or stable in 6.0–6.2.
As a result, the bazel tool's HTTP/BCR probing (which relies on `bazel mod show_repo` in Bzlmod mode) can fail on 6.0–6.3 even if a plain `bazel build` succeeds.
Recommended action: Upgrade Bazel to 6.4+ (preferably 7.x or 8.x), where `bazel mod` is stable and Bzlmod is the default.
</note>

## BCR Modules and Dependency Classification

Bazel supports several mechanisms for consuming external dependencies, which differ in the amount of dependency metadata and relationship information available to [detect_product_short] and affects how [detect_product_short] can classify dependencies as direct or transitive.

**WORKSPACE-Based http_archive, git_repository, and go_repository**

External source code is fetched directly into the build environment. These rules provide no dependency relationship metadata beyond the declarations present in the WORKSPACE file. All dependencies must be explicitly declared by the project as Bazel does not maintain a dependency graph for these repository types. The resulting dependency list is inherently flat, with no parent-child relationships available for analysis or reconstruction. As a result, [detect_product_short] can identify these dependencies but reports all of them as direct dependencies.

**Maven Dependencies via rules_jvm_external**

Projects that use `maven_install` from `rules_jvm_external` rely on Maven's dependency resolution model. The complete dependency graph exists internally within the resolver and its lockfile infrastructure, however that information is not exposed through Bazel's module graph APIs.

[detect_product_short] can extract Maven coordinates from the Bazel build configuration, but it cannot accurately determine dependency ancestry. Dependencies obtained through `maven_install` are therefore reported as a flat list rather than a hierarchical dependency tree.

**Bazel Central Registry (BCR) Modules (bazel_dep in MODULE.bazel)**

Dependencies declared with `bazel_dep` in `MODULE.bazel` provide the most complete dependency metadata available within Bazel. Modules published to the [Bazel Central Registry (BCR)](https://registry.bazel.build/) include their own `MODULE.bazel` that declares their own direct dependencies, following the same dependency management model used by Maven, npm, and Cargo. Beginning with Bazel 7.1, Bazel resolves the full transitive graph by reading each module's `MODULE.bazel` recursively and exposes the result via `bazel mod graph --output json`, which includes explicit parent-child relationships between modules. [detect_product_short] consumes this data to accurately classify dependencies as either direct or transitive when generating a Bill of Materials (BOM).

For Bzlmod-based projects running Bazel 7.1 or later, [detect_product_short] first executes `bazel mod graph` to obtain the complete module dependency hierarchy. [detect_product_short] then invokes `bazel mod show_repo` for each module to resolve repository source locations and associated metadata.

Dependencies discovered via `maven_install` and `http_archive` are incorporated into the same BOM, however, because these dependency types do not expose equivalent graph information through Bazel, they are reported as flat dependency sets.

## Combining Bazel Detection with Package Manager Detection for Additional Coverage

As a best practice, projects built with Bazel should not rely solely on the
Bazel detector to produce a complete result. Bazel and package managers operate
at different layers of the software supply chain, so [detect_product_short]
evaluates them independently to ensure comprehensive coverage.

### Bazel Detection vs Package Manager Detection

**The Bazel detector** identifies Bazel-level dependencies: external repositories
and modules that Bazel resolves during the build, such as `http_archive`,
`git_repository`, `go_repository`, `maven_install`, and similar rules. For each
discovered repository, it attempts to extract a URL and match it against the
Black Duck Knowledge Base.

**Package manager detectors** identify ecosystem-level dependencies by parsing
manifest files: `pom.xml`, `package.json`, `requirements.txt`, `go.mod`,
`Cargo.lock`, `Gemfile.lock`, `packages.config`, and others. These detectors
operate on file content regardless of whether the project uses Bazel.

A Bazel external repository represents an archived bundle of source code. This
archive can include package manager metadata for one or more ecosystems. The
Bazel detector identifies the archive itself as a dependency, while package
manager detectors analyze its contents to identify any supported manifest-based
dependencies contained within.

```
Your Bazel target
 └── @protobuf//:lib          ← Bazel detector: matches via GitHub URL → KB entry
      └── java/pom.xml        ← Maven detector: discovers Java deps from manifest
      └── python/setup.py     ← PIP detector: discovers Python deps from manifest
      └── js/package.json     ← NPM detector: discovers JS deps from manifest

 └── @internal_lib//:lib      ← Bazel detector: private URL, no KB match
      └── requirements.txt    ← PIP detector: discovers Python deps inside the archive
      └── go.mod              ← Go detector: discovers Go deps inside the archive
```

In this example, running only the Bazel detector identifies a single component
(`protobuf`), matched through its public GitHub repository. When both Bazel and
package manager detectors are used, the analysis includes `protobuf` along with
additional ecosystem-level dependencies discovered from supported manifests within
the repository. This combined approach also reveals content from `@internal_lib`
that would not be detected by the Bazel detector alone.

It is therefore expected that a Bazel-only scan will identify fewer components
than a combined scan. This difference does not indicate missing Bazel dependencies.
Rather, it reflects the ability of package manager detectors to identify additional
ecosystem-specific components within Bazel-managed repositories. The extent of
coverage depends on the ecosystems supported by [detect_product_short], the
presence of manifest files within retrieved archives, and the configured search
depth.

For a more complete dependency inventory, enable both:

```sh
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) \
  --detect.tools=BAZEL,DETECTOR \
  --detect.bazel.target='//myproject:mytarget' \
  --detect.detector.search.depth=5
```

### Using Bazel Fetch for Additional Coverage

The Bazel detector extracts URLs from standard repository rules (`http_archive`,
`git_repository`, `go_repository`). Environments that use custom repository
macros, private registry infrastructure, or other abstractions that do not
surface standard URLs through Bazel metadata will see reduced coverage from the
Bazel detector alone. The archives are fetched correctly by Bazel but their
URLs are not extractable by [detect_product_short].

In these environments, a `bazel fetch` workflow can provide additional coverage
by materializing external repositories on disk and allowing package manager
detectors to analyze their contents directly:

```sh
# Step 1: Fetch and extract all external dependencies for a target
bazel --output_base=/your/scan/dir/target_name fetch //your:target

# Step 2: Run Detect with DETECTOR against the extracted sources
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) \
  --detect.tools=DETECTOR \
  --detect.source.path=/your/scan/dir/target_name/external/ \
  --detect.detector.search.depth=5 \
  --detect.accuracy.required=NONE
```

This approach enables the identification of ecosystem-level dependencies within
extracted archives for supported ecosystems where manifest files are present.
However, coverage is not guaranteed for all archives. Repositories that do not
contain supported manifest files will not produce additional results, and the
size of fetched sources may vary significantly depending on the target's
dependency graph.

<note type="note">
`--detect.tools=DETECTOR` is used here, not `BAZEL`. The Bazel detector relies
on Bazel metadata and graph information rather than scanning extracted source
trees for manifest files. DETECTOR runs ecosystem detectors directly against
source files on disk.
</note>

<note type="note">
`--detect.detector.search.depth` controls how many directory levels deep
[detect_product_short] searches for manifest files within each extracted archive.
Set this value based on where manifests sit inside your archives. A value of 5
is a safe default for most archive structures.
</note>

<note type="note">
The `bazel fetch` workflow is a complementary strategy, not a replacement for
standard Bazel detection. It is most useful when custom macros or private
registries prevent URL extraction by the Bazel detector. For projects using
standard `http_archive` rules with public URLs, running `--detect.tools=BAZEL,DETECTOR`
directly is the simpler and recommended path.
</note>

For environments where per-target isolation is needed, use `--output_base` to
scope the fetch to a directory of your choice:

```sh
# Each target gets its own output base; only that target's deps land there
bazel --output_base=/scan/firmware_ecu fetch //firmware:ecu_target
bazel --output_base=/scan/firmware_net fetch //firmware:net_target
```

---

## Usage

**Tool invocation:**

The Bazel tool runs automatically when enabled via the `detect.tools` property. Ensure your configuration includes `--detect.tools=BAZEL`.

Requirements:
- The `--detect.bazel.target` property to specify the Bazel build target
- The `bazel` executable must be available on your `$PATH`

### Basic Example
```sh
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) \
  --detect.tools=BAZEL \
  --detect.bazel.target='//myproject:mytarget'
```

## Pipeline Details

The following example shows command-line invocations equivalent to those the Bazel tool uses to identify components.

### Processing for the *maven_install* workspace rule

The Bazel tool runs a Bazel cquery on the given target to produce output from which it can parse artifact details such as group, artifact, and version for dependencies.

[detect_product_short]'s Bazel tool uses commands similar to the following for discovery of *maven_install* dependencies:
```sh
$ bazel cquery --noimplicit_deps 'kind(j.*import, deps(//tests/integration:ArtifactExclusionsTest))' --output build 2>&1 | grep maven_coordinates
tags = ["maven_coordinates=com.google.guava:guava:27.0-jre"],
tags = ["maven_coordinates=org.hamcrest:hamcrest:2.1"],
tags = ["maven_coordinates=org.hamcrest:hamcrest-core:2.1"],
tags = ["maven_coordinates=com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava"],
tags = ["maven_coordinates=org.checkerframework:checker-qual:2.5.2"],
tags = ["maven_coordinates=com.google.guava:failureaccess:1.0"],
tags = ["maven_coordinates=com.google.errorprone:error_prone_annotations:2.2.0"],
tags = ["maven_coordinates=com.google.code.findbugs:jsr305:3.0.2"],
```
Then, it parses the group/artifact/version details from the values of the maven_coordinates tags.

### Processing for the *maven_jar* workspace rule

The Bazel tool runs a Bazel query on the given target to get a list of jar dependencies. On each jar dependency, the Bazel tool runs another Bazel query to get its artifact details: group, artifact, and version.

Get list of dependencies:
```sh
$ bazel cquery 'filter("@.*:jar", deps(//:ProjectRunner))'
INFO: Invocation ID: dfe8718d-b4db-4bd9-b9b9-57842cca3fb4
@org_apache_commons_commons_io//jar:jar
@com_google_guava_guava//jar:jar
Loading: 0 packages loaded
```
Get details for each dependency, prepending //external: to the dependency name for this command:
```sh
$ bazel query 'kind(maven_jar, //external:org_apache_commons_commons_io)' --output xml
INFO: Invocation ID: 0a320967-b2a8-4b36-ab47-e183bc4d4781
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<query version="2">
    <rule class="maven_jar" location="/root/home/steve/examples/java-tutorial/WORKSPACE:6:1" name="//external:org_apache_commons_commons_io">
        <string name="name" value="org_apache_commons_commons_io"/>
        <string name="artifact" value="org.apache.commons:commons-io:1.3.2"/>
    </rule>
</query>
Loading: 0 packages loaded
```
Finally, it parses the group/artifact/version details from the value of the string element using the name of artifact.

### Processing for the *haskell_cabal_library* workspace rule

Requires Bazel 2.1.0 or later.

[detect_product_short]'s Bazel tool extracts artifact project and version for dependencies by running a Bazel cquery on the given target.

The Bazel tool uses a command similar to the following to discover *haskell_cabal_library* dependencies:
```sh
$ bazel cquery --noimplicit_deps 'kind(haskell_cabal_library, deps(//cat_hs/lib/args:args))' --output jsonproto
{
"results": [{
"target": {
"type": "RULE",
"rule": {
...
"attribute": [{
...
}, {
"name": "name",
"type": "STRING",
"stringValue": "hspec",
"explicitlySpecified": true,
"nodep": false
}, {
"name": "version",
"type": "STRING",
"stringValue": "2.7.1",
"explicitlySpecified": true,
"nodep": false
}, {
...
```
It then uses Gson to parse the JSON output into a parse tree, extracting the name and version from the corresponding rule attributes.

### Processing for the *http_archive* workspace rule

The Bazel tool probes the Bazel dependency graph to determine if http_archive dependencies are present and then triggers the appropriate pipeline based on the Bazel era:

<note type="note">
Projects with ≤150 external repositories are fully probed to precisely detect HTTP dependencies. Larger projects (>150 repos) skip probing and automatically enable the HTTP extraction pipeline to ensure completeness while avoiding the overhead of probing hundreds of repositories.
</note>

- **For bzlmod projects:**
  - The tool uses `bazel mod show_repo` to extract repository information and candidate GitHub URLs for external dependencies.
  - Leverages Bazel's module system to accurately identify external dependencies and their sources.
  - Example command (run for each external repo):
    ```sh
    $ bazel mod show_repo <repo_name>
    # Output includes repository details, including URLs
    ```
  - The tool parses the output to extract GitHub URLs and version information for each dependency.

- **For WORKSPACE-based projects:**
  - The tool uses `bazel query` and XML parsing to extract URLs from `http_archive`, `go_repository`, and `git_repository` rules. Only URLs matching GitHub release/archive patterns are considered.
  - Example commands:
    ```sh
    # Get a list of external library dependencies
    $ bazel query 'kind(.*library, deps(//:bd_bazel))'
    # For each dependency, get details (example for http_archive):
    $ bazel query 'kind(.*, //external:com_github_gflags_gflags)' --output xml
    ```
  - The tool parses the XML output to extract the GitHub URL and version for each dependency.

This dual approach ensures that the Bazel tool works seamlessly for both modern bzlmod-based and traditional WORKSPACE-based Bazel projects, always probing the graph to decide which pipeline to run.

### Processing for BCR modules (Bzlmod mode on Bazel 7.1 or later)

When [detect_product_short] runs in Bzlmod mode on Bazel 7.1 or later, it performs a dedicated Bazel Central Registry (BCR) dependency extraction step before executing the standard dependency discovery pipelines.

After BCR extraction completes, the http_archive discovery pipeline still runs to identify additional external dependencies. However, any dependency that was already identified and classified through the BCR module graph is deduplicated using its ExternalId. This ensures that the dependency relationships established by the BCR extractor are preserved, including the distinction between direct and transitive dependencies.

Without this deduplication step, dependencies discovered later through http_archive analysis could be incorrectly reclassified as direct dependencies, resulting in the loss of the dependency hierarchy derived from the Bzlmod module graph.

Custom http_archive definitions, private repositories, and non-BCR source archives absent from `MODULE.bazel` are not part of the BCR classification step. These dependencies are processed by the standard repository-rule analysis pipeline. However, that pipeline can only extract dependencies whose URLs match known GitHub release patterns. Private repositories, custom repository macros, and non-standard registry URLs will produce a WARN-level log entry and will be excluded from the BOM. See [Using Bazel Fetch for Additional Coverage](#using-bazel-fetch-for-additional-coverage) for a workaround in these environments.

**Step 1 — Discover the full module dependency tree:**
```sh
$ bazel mod graph --output json
```
The JSON output contains the complete module graph with parent-child edges. Direct dependencies (declared via `bazel_dep` in `MODULE.bazel`) appear at the root level; their transitive dependencies are nested beneath them.

**Step 2 — Resolve each module to a source URL:**
```sh
# Batched call (Bazel 7.1+, tried first)
$ bazel mod show_repo @module1 @module2 @module3 ...

# Per-module fallback if batch fails
$ bazel mod show_repo @module_name
```
[detect_product_short] parses the `show_repo` output for GitHub URLs using the same extraction logic as the standard http_archive BZLMOD pipeline.

**Step 3 — Build the classified BOM:**
Modules declared via `bazel_dep` appear as direct dependencies. Their transitive dependencies appear nested under their respective parents rather than at the root. The BOM correctly reflects which components your project explicitly chose versus which came along transitively.

A WARN-level log entry is written for modules where no GitHub URL can be extracted (private repos, custom non-BCR rules), and excluded from the BOM. To assist in investigating what was missed, the warning includes the raw URL(s) found.

## Examples

### maven_install
```sh
git clone https://github.com/bazelbuild/rules_jvm_external
cd rules_jvm_external/
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.bazel.target='//tests/integration:ArtifactExclusionsTest'
```

### haskell_cabal_library
```sh
git clone https://github.com/tweag/rules_haskell.git
cd rules_haskell/examples
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.bazel.target='//cat_hs/lib/args:args'
```

### http_archive
```sh
# For a project with http_archive dependencies
git clone https://github.com/example/example-bazel-project.git
cd example-bazel-project
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.bazel.target='//:mytarget'
```

### Getting Help

- Refer to the [Bazel Troubleshooting Guide](../troubleshooting/solutions.md) 
- Check the Detect logs in the `runs/` directory for detailed error messages
- Verify your Bazel setup works: `bazel build //your:target`
- For further assistance, contact Black Duck support with your Detect logs

## Migrating from earlier versions of [detect_product_short]

### Upgrading to [detect_product_short] 11.3.0

The Bazel tool was replaced entirely in [detect_product_short] 11.3.0. The following changes are required if you are upgrading from an earlier version.

**Property:**

The `detect.bazel.workspace.rules` property has been deprecated and replaced by `detect.bazel.dependency.sources`. The new property accepts the same source names (MAVEN_INSTALL, MAVEN_JAR, HASKELL_CABAL_LIBRARY, HTTP_ARCHIVE). NONE retains the auto-detect behavior.

### Mode Detection Behavior

The tool automatically determines your Bazel mode through the following process:

1. **Attempts BZLMOD Detection:** Runs `bazel mod graph`
2. **Success with dependencies → BZLMOD Mode:** Uses BZLMOD-specific pipelines
3. **Empty graph → WORKSPACE Mode:** Common in hybrid repos that declare MODULE.bazel for compatibility but manage dependencies via WORKSPACE
4. **"Command not found" → WORKSPACE Mode:** Bazel version < 6.0; assumes WORKSPACE mode with a warning

### Auto-Detection vs Manual Override

**Auto-Detection (Default):**
- Probes the dependency graph to detect which dependency sources are present
- Runs targeted Bazel queries for each potential source type
- More flexible but requires multiple Bazel command executions

**Manual Override (detect.bazel.dependency.sources):**
- Skips graph probing
- Directly runs pipelines for specified sources
- Faster execution if you know your dependency types
- Useful for CI/CD optimization

Example for known dependencies:
```sh
# Skip probing, directly extract Maven and HTTP dependencies
--detect.bazel.dependency.sources=MAVEN_INSTALL,HTTP_ARCHIVE
```

### Performance Considerations

- **HTTP Detection:** The tool automatically determines the best strategy based on project size. Small/medium projects (≤150 repos) are fully probed for precise detection. Large projects skip probing and always enable HTTP extraction for guaranteed completeness.
- **Target Specificity:** More specific targets (for example, `//module:specific-target`) are faster than broad targets (for example, `//:all`)
- **Manual Override:** Setting `detect.bazel.dependency.sources` explicitly can significantly speed up detection

### Best Practices

- Use specific build targets rather than workspace-wide targets for better performance
- Use `--logging.level.detect=DEBUG` when troubleshooting
- For consistent CI/CD builds, set `--detect.bazel.mode=WORKSPACE` or `BZLMOD` explicitly
- If dependency types are known, set `--detect.bazel.dependency.sources` to avoid probing overhead
