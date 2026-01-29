# Bazel support

## Bazel V2 Detector Overview

[detect_product_short] provides robust support for Bazel projects using a new V2 detector. The V2 detector is mode-agnostic and works seamlessly with both **BZLMOD** (MODULE.bazel) and **WORKSPACE**-based projects.

The V2 detector discovers dependencies from the following dependency sources:

- *maven_jar* - Legacy Maven dependencies
- *maven_install* - Modern Maven dependencies via rules_jvm_external
- *haskell_cabal_library* - Haskell Cabal packages
- *http_archive* - Source archives from HTTP, Git, and other sources

It also discovers library dependencies that have a GitHub released artifact location (URL) specified in an *http_archive*, *go_repository*, or *git_repository* rule.

### Key Features
- **Mode-Agnostic:** Automatically detects whether your project uses BZLMOD or WORKSPACE and adapts accordingly
- **Intelligent Mode Detection:** Uses `bazel mod show_repo` to detect BZLMOD support; falls back to WORKSPACE mode for older Bazel versions (< 6.0)
- **Automatic Pipeline Selection:** Probes your Bazel dependency graph to determine which dependency sources are present and automatically runs the correct extraction pipelines
- **No WORKSPACE File Required:** Unlike the legacy detector, V2 works with BZLMOD-only projects that don't have a WORKSPACE file
- **Enhanced HTTP Detection:** 
  - For BZLMOD projects: Uses `bazel mod show_repo` to robustly extract dependency URLs from external repositories
  - For WORKSPACE projects: Uses XML parsing to extract URLs from repository rules
- **Configurable Probing:** Control the depth of dependency graph probing with the `detect.bazel.http.probe.limit` property

## Usage

The Bazel V2 detector runs automatically when enabled via the `detect.tools` property. It requires:
- The `--detect.bazel.target` property to specify the Bazel build target
- The `bazel` executable must be available on your `$PATH`

### Basic Example
```sh
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) \
  --detect.tools=BAZEL \
  --detect.bazel.target='//myproject:mytarget'
```

## Properties

### Required Properties

#### `detect.bazel.target`
- **Description:** The Bazel build target to analyze for dependencies
- **Required:** Yes
- **Example:** `--detect.bazel.target='//tests/integration:ArtifactExclusionsTest'`

### Optional Properties

#### `detect.bazel.dependency.sources`

- This property replaces `detect.bazel.workspace.rules`. Only the name has changed; the behavior and supported values are identical. Existing workflows should switch to `detect.bazel.dependency.sources` without any functional differences.

- **Description:** Manually specify which dependency sources to extract. By default (NONE), the detector automatically probes the dependency graph to determine which sources are present.
- **Default:** `NONE` (auto-detection enabled)
- **Valid Values:** `MAVEN_INSTALL`, `MAVEN_JAR`, `HASKELL_CABAL_LIBRARY`, `HTTP_ARCHIVE`, `ALL`, `NONE`
- **Example:** `--detect.bazel.dependency.sources=MAVEN_INSTALL,HTTP_ARCHIVE`
- **Note:** This property works for both BZLMOD and WORKSPACE projects.
- **When to Use:** Set this property if you know which dependency sources are present in your target to skip the probing step. This can improve performance, especially in CI/CD environments. Use `ALL` to extract from all supported sources without probing.

#### `detect.bazel.mode`
- **Description:** Manually override Bazel mode detection. By default, the detector automatically determines whether the project uses BZLMOD or WORKSPACE.
- **Default:** Auto-detection
- **Valid Values:** `WORKSPACE`, `BZLMOD`
- **Example:** `--detect.bazel.mode=BZLMOD`
- **When to Use:** Only set this if auto-detection fails or for testing purposes. Incorrect values may cause extraction to fail.

#### `detect.bazel.http.probe.limit`
- **Purpose:** A safety cap on how many external repositories the detector will probe for HTTP-family rules (`http_archive`, `git_repository`, `go_repository`). Each probe runs Bazel subprocesses and can be costly.
- **Default:** `100`
- **Example (advanced use):** `--detect.bazel.http.probe.limit=200`

#### `detect.bazel.cquery.options`
- **Description:** Additional options to pass to Bazel cquery commands
- **Example:** `--detect.bazel.cquery.options='--repository_cache=/path/to/cache'`

## Pipeline Details

### Processing for the *maven_install* workspace rule
The Bazel tool runs a bazel cquery on the given target to produce output from which it can parse artifact details such as group, artifact, and version for dependencies.

[detect_product_short]'s Bazel detector uses commands very similar to the following to discover *maven_install* dependencies:
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

The following is an example using the equivalent commands that [detect_product_short] runs, but from the command line, showing how [detect_product_short]'s Bazel detector currently identifies components. First, it gets a list of dependencies:
```sh
$ bazel cquery 'filter("@.*:jar", deps(//:ProjectRunner))'
INFO: Invocation ID: dfe8718d-b4db-4bd9-b9b9-57842cca3fb4
@org_apache_commons_commons_io//jar:jar
@com_google_guava_guava//jar:jar
Loading: 0 packages loaded
```
Then, it gets details for each dependency. It prepends //external: to the dependency name for this command:
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

[detect_product_short]'s Bazel detector runs a bazel cquery on the given target to produce output from which it can extract artifact project and version for dependencies.

The Bazel detector uses a command very similar to the following to discover *haskell_cabal_library* dependencies:
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
It then uses Gson to parse the JSON output into a parse tree, and extracts the name and version from the corresponding rule attributes.

### Processing for the *http_archive* workspace rule
The V2 detector is agnostic to WORKSPACE or MODULE.bazel files. It probes the Bazel dependency graph to determine if http_archive dependencies are present and then triggers the appropriate pipeline based on the Bazel era:

- **For bzlmod projects:**
  - The detector uses `bazel mod show_repo` to extract repository information and candidate GitHub URLs for external dependencies.
  - This approach is robust and leverages Bazel's module system to accurately identify external dependencies and their sources.
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

#### "Unable to determine Bazel mode automatically"
**Problem:** The detector cannot determine if your project uses BZLMOD or WORKSPACE mode.

**Solution:** Manually specify the mode using `--detect.bazel.mode=WORKSPACE` or `--detect.bazel.mode=BZLMOD`

**Root Cause:** Usually occurs when the `bazel mod show_repo` command fails unexpectedly (not due to old Bazel version).

#### "No supported Bazel dependency sources found"
**Problem:** The automatic graph probing didn't detect any dependency sources.

**Solution:** 
1. Verify your target has dependencies: `bazel query 'deps(//your:target)'`
2. Manually specify dependency sources: `--detect.bazel.dependency.sources=MAVEN_INSTALL,HTTP_ARCHIVE`
3. Check that your Bazel target builds successfully: `bazel build //your:target`

#### Old Bazel Version Warning
**Problem:** You see a warning: "Bazel does not support 'mod' command (likely version < 6.0)"

**Solution:** 
- The detector will assume WORKSPACE mode and continue. This is expected behavior for Bazel versions before 6.0.
- To use BZLMOD features, upgrade to Bazel 6.0 or later.
- To suppress the warning, explicitly set: `--detect.bazel.mode=WORKSPACE`

#### HTTP Dependencies Missing
**Problem:** Some http_archive or git_repository dependencies are not detected.

**Solutions:**
1. Increase the probe limit: `--detect.bazel.http.probe.limit=200`
2. Check the logs for the warning: "Repository probe limit reached"
3. Consider analyzing a more specific target with fewer total dependencies

#### Bazel Executable Not Found
**Problem:** Error indicates Bazel executable cannot be located.

**Solutions:**
1. Ensure Bazel is installed: `bazel version`
2. Verify Bazel is on your PATH: `which bazel`
3. Or specify the path explicitly: `--detect.bazel.path=/path/to/bazel`

#### Duplicate Dependencies Detected
**Problem:** The same dependencies appear multiple times in the results.

**Solution:** You may have both the legacy detector (V1) and V2 running simultaneously. Use `--detect.tools=BAZEL` to run only V2.

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

## Notes

### Bazel V2 vs V1 (Legacy) Detector

- **V2 (Recommended):** Enabled via `--detect.tools=BAZEL`. Supports both BZLMOD and WORKSPACE projects. Does not require a WORKSPACE file.
- **V1 (Legacy):** Part of the detector framework (`--detect.tools=DETECTOR`). Only works with WORKSPACE projects. Requires WORKSPACE file to be present.
- **Default Behavior:** If you set `detect.bazel.target` without specifying tools, you may need to explicitly enable BAZEL tool: `--detect.tools=BAZEL`
- **Recommendation:** Use V2 for all projects. V1 is maintained for backward compatibility but has limitations with modern Bazel projects.

### Mode Detection Behavior

The detector automatically determines your Bazel mode through the following process:

1. **Attempts BZLMOD Detection:** Runs `bazel mod show_repo bazel_tools`
2. **Success → BZLMOD Mode:** If the command succeeds, uses BZLMOD-specific pipelines
3. **"Command not found" → WORKSPACE Mode:** If Bazel version < 6.0, assumes WORKSPACE mode with a warning
4. **Other Errors → Manual Override Required:** If detection fails unexpectedly, you must set `detect.bazel.mode` manually

### Auto-Detection vs Manual Override

**Auto-Detection (Default):**
- Probes the dependency graph to detect which dependency sources are present
- Runs targeted Bazel queries for each potential source type
- More flexible but slower (multiple Bazel command executions)

**Manual Override (`detect.bazel.dependency.sources`):**
- Skips graph probing
- Directly runs pipelines for specified sources
- Faster execution if you know your dependency types
- Useful for CI/CD optimization

**Example for known dependencies:**
```sh
# Skip probing, directly extract Maven and HTTP dependencies
--detect.bazel.dependency.sources=MAVEN_INSTALL,HTTP_ARCHIVE
```

### Performance Considerations

- **HTTP Probe Limit:** Default limit of 100 repositories prevents excessive command execution on large monorepos. Adjust if needed.
- **Target Specificity:** More specific targets (e.g., `//module:specific-target`) are faster than broad targets (e.g., `//:all`)
- **Manual Override:** Setting `detect.bazel.dependency.sources` explicitly can significantly speed up detection

### Supported Bazel Versions

- **Bazel 6.0+:** Full BZLMOD support with optimal detection
- **Bazel 5.x and earlier:** WORKSPACE mode only; BZLMOD features unavailable
- **Minimum Version:** Bazel 2.1.0+ required for haskell_cabal_library support

### Best Practices

1. **Use Specific Targets:** Analyze specific build targets rather than workspace-wide targets for better performance
2. **Enable Debug Logging:** Use `--logging.level.detect=DEBUG` when troubleshooting
3. **Set Mode Explicitly in CI/CD:** For consistent builds, set `--detect.bazel.mode=WORKSPACE` or `BZLMOD` explicitly
4. **Override Pipeline Selection in CI/CD:** If dependency types are known, set `--detect.bazel.dependency.sources` to avoid probing overhead
