# Bazel support

## Related properties

[Detector properties](../properties/detectors/bazel.md)

## Bazel V2 Detector Overview

[detect_product_short] provides support for Bazel projects using the V2 detector. The V2 detector is mode agnostic and works seamlessly with both BZLMOD (MODULE.bazel) and WORKSPACE-based projects.

The V2 detector discovers dependencies from the following dependency sources:

- *maven_jar* - Legacy Maven dependencies
- *maven_install* - Modern Maven dependencies via rules_jvm_external
- *haskell_cabal_library* - Haskell Cabal packages
- *http_archive* - Source archives from HTTP, Git, and other sources

The V2 detector discovers library dependencies that have a GitHub released artifact location (URL) specified in an *http_archive*, *go_repository*, or *git_repository* rule.

### Key Features
- **Mode-Agnostic:** Automatically detects whether your project uses BZLMOD or WORKSPACE and adapts accordingly
- **Intelligent Mode Detection:** Uses `bazel mod graph` to detect BZLMOD support; falls back to WORKSPACE mode for older Bazel versions (< 6.0)
- **Automatic Pipeline Selection:** Probes your Bazel dependency graph to determine which dependency sources are present and automatically runs the correct extraction pipelines
- **Enhanced HTTP Detection:**
  - For BZLMOD projects: Uses `bazel mod show_repo` to robustly extract dependency URLs from external repositories
  - For WORKSPACE projects: Uses XML parsing to extract URLs from repository rules
- **Smart HTTP Detection:** Small/medium targets (≤150 external repos) are fully probed for precise detection. Large projects (>150 repos) automatically enable the HTTP pipeline to ensure completeness without probing overhead.

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
As a result, the Bazel V2 detector's HTTP/BCR probing (which relies on `bazel mod show_repo` in Bzlmod mode) can fail on 6.0–6.3 even if a plain `bazel build` succeeds.
Recommended action: Upgrade Bazel to 6.4+ (preferably 7.x or 8.x), where `bazel mod` is stable and Bzlmod is the default.
</note>

## Usage

The Bazel V2 detector runs automatically when enabled via the `detect.tools` property.

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

### Processing for the *maven_install* workspace rule
The Bazel tool runs a bazel cquery on the given target to produce output from which it can parse artifact details such as group, artifact, and version for dependencies.

[detect_product_short]'s Bazel detector uses commands similar to the following for discovery of *maven_install* dependencies:
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
The Bazel tool runs a bazel query on the given target to get a list of jar dependencies. On each jar dependency, the Bazel tool runs another bazel query to get its artifact details: group, artifact, and version.

The following is an example using command line commands equivalent to [detect_product_short], showing how [detect_product_short]'s Bazel detector currently identifies components.

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

[detect_product_short]'s Bazel detector extracts artifact project and version for dependencies by running a bazel cquery on the given target.

The Bazel detector uses a command similar to the following to discover *haskell_cabal_library* dependencies:
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
The V2 detector is agnostic to WORKSPACE or MODULE.bazel files, probing the Bazel dependency graph to determine if http_archive dependencies are present and then triggers the appropriate pipeline based on the Bazel era:

<note type="note">
Projects with ≤150 external repositories are fully probed to precisely detect HTTP dependencies. Larger projects (>150 repos) skip probing and automatically enable the HTTP extraction pipeline to ensure completeness while avoiding the overhead of probing hundreds of repositories.
</note>

- **For bzlmod projects:**
  - The detector uses `bazel mod show_repo` to extract repository information and candidate GitHub URLs for external dependencies.
  - Leverages Bazel's module system to accurately identify external dependencies and their sources.
  - Example command (run for each external repo):
    ```sh
    $ bazel mod show_repo <repo_name>
    # Output includes repository details, including URLs
    ```
  - The detector parses the output to extract GitHub URLs and version information for each dependency.

- **For WORKSPACE-based projects:**
  - The detector uses `bazel query` and XML parsing to extract URLs from `http_archive`, `go_repository`, and `git_repository` rules. Only URLs matching GitHub release/archive patterns are considered.
  - Example commands:
    ```sh
    # Get a list of external library dependencies
    $ bazel query 'kind(.*library, deps(//:bd_bazel))'
    # For each dependency, get details (example for http_archive):
    $ bazel query 'kind(.*, //external:com_github_gflags_gflags)' --output xml
    ```
  - The detector parses the XML output to extract the GitHub URL and version for each dependency.

This dual approach ensures that the detector works seamlessly for both modern bzlmod-based and traditional WORKSPACE-based Bazel projects, always probing the graph to decide which pipeline to run.

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

## Troubleshooting

### Common Issues and Solutions

#### Unable to determine Bazel mode automatically
**Problem:** The detector cannot determine if your project uses BZLMOD or WORKSPACE mode. Usually occurs when the `bazel mod show_repo` command fails unexpectedly (not due to old Bazel version).

**Possible Solutions:**
- Manually specify the mode using `--detect.bazel.mode=WORKSPACE` or `--detect.bazel.mode=BZLMOD`

#### No supported Bazel dependency sources found
**Problem:** The automatic graph probing did not detect any dependency sources.

**Possible Solutions:**
- Verify your target has dependencies: `bazel query 'deps(//your:target)'`
- Manually specify dependency sources: `--detect.bazel.dependency.sources=MAVEN_INSTALL,HTTP_ARCHIVE`
- Check that your Bazel target builds successfully: `bazel build //your:target`

#### Old Bazel Version Warning
**Problem:** You see a warning like "Bazel does not support 'mod' command (likely version < 6.0)" or "show repo command not found".

Expected behavior for Bazel versions before 6.0: the detector assumes WORKSPACE mode and continues.

**Possible Solutions:**
- To use BZLMOD features, upgrade to Bazel 6.4+ (preferably 7.x or 8.x)
- To suppress the warning, explicitly set: `--detect.bazel.mode=WORKSPACE`

<note type="note">For Bazel 6.0–6.3 with Bzlmod enabled (via --enable_bzlmod), the detector may fail to probe HTTP/BCR repositories because bazel mod show_repo is unavailable or unstable. Upgrade or use WORKSPACE mode as a workaround.</note>

#### HTTP Dependencies Missing
**Problem:** Some http_archive or git_repository dependencies are not detected.

**Possible Solutions:**
- Check the logs to verify the HTTP pipeline was enabled
- Verify the dependencies are actually reachable from your specified target: `bazel query 'deps(//your:target)'`
- For projects where HTTP dependencies are known to be absent, exclude the pipeline explicitly: `--detect.bazel.dependency.sources=MAVEN_INSTALL`

#### Bazel Executable Not Found
**Problem:** Error indicates Bazel executable cannot be located.

**Possible Solutions:**
- Ensure Bazel is installed: `bazel version`
- Verify Bazel is on your PATH: `which bazel`
- Specify the path explicitly: `--detect.bazel.path=/path/to/bazel`

### Debug Mode
For detailed logging to diagnose issues:
```sh
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) \
  --logging.level.detect=DEBUG \
  --detect.bazel.target='//your:target'
```

### Getting Help
- Check the Detect logs in the `runs/` directory for detailed error messages
- Verify your Bazel setup works: `bazel build //your:target`
- For further assistance, contact Black Duck support with your Detect logs

## Migrating from earlier versions of [detect_product_short]

### Upgrading to [detect_product_short] 11.3.0

The Bazel detector was replaced entirely in [detect_product_short] 11.3.0. The following changes are required if you are upgrading from an earlier version.

**Property rename:**

The `detect.bazel.workspace.rules` property has been removed and replaced by `detect.bazel.dependency.sources`. The new property accepts the same source names (MAVEN_INSTALL, MAVEN_JAR, HASKELL_CABAL_LIBRARY, HTTP_ARCHIVE). NONE retains the auto-detect behavior.

<note type="note">The old `detect.bazel.workspace.rules` property is no longer recognized. If it is present in your configuration it will be silently ignored. Replace it with `detect.bazel.dependency.sources`.</note>

**Tool invocation:**

The Bazel V2 detector runs as a tool. Ensure your configuration includes `--detect.tools=BAZEL`. Setting `--detect.tools=DETECTOR` will not run the Bazel detector.

## Notes

### Bazel detector

The Bazel V2 detector is the only supported Bazel detector as of [detect_product_short] 11.3.0. The prior Bazel detector, which was limited to WORKSPACE-only projects and required a WORKSPACE file to be present, has been replaced by the V2 detector. The V2 detector supports both BZLMOD and WORKSPACE projects and does not require a WORKSPACE file.

To run the Bazel V2 detector, set `--detect.tools=BAZEL` and provide a target via `--detect.bazel.target`.

### Mode Detection Behavior

The detector automatically determines your Bazel mode through the following process:

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

- **Smart HTTP Detection:** The detector automatically determines the best strategy based on project size. Small/medium projects (≤150 repos) are fully probed for precise detection. Large projects skip probing and always enable HTTP extraction for guaranteed completeness.
- **Target Specificity:** More specific targets (for example, `//module:specific-target`) are faster than broad targets (for example, `//:all`)
- **Manual Override:** Setting `detect.bazel.dependency.sources` explicitly can significantly speed up detection

### Best Practices

- Use specific build targets rather than workspace-wide targets for better performance
- Use `--logging.level.detect=DEBUG` when troubleshooting
- For consistent CI/CD builds, set `--detect.bazel.mode=WORKSPACE` or `BZLMOD` explicitly
- If dependency types are known, set `--detect.bazel.dependency.sources` to avoid probing overhead
