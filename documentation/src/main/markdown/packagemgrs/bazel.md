# Bazel support

## Bazel V2 Detector Overview

[detect_product_short] provides robust support for Bazel projects using a new V2 detector. The V2 detector discovers dependencies specified in the following workspace rules:

- *maven_jar*
- *maven_install* (rules_jvm_external)
- *haskell_cabal_library*
- *http_archive* (and related rules)

It also discovers library dependencies that have a GitHub released artifact location (URL) specified in an *http_archive*, *go_repository*, or *git_repository* workspace rule.

### How V2 Works
- **Automatic Pipeline Selection:** The V2 detector probes your Bazel project to determine which supported dependency types are present and automatically runs the correct extraction pipelines.
- **Pipeline Logic:** The logic for extracting dependencies from *maven_install*, *maven_jar*, and *haskell_cabal_library* is unchanged from previous versions. Only the *http_archive* pipeline has been enhanced in V2:
  - For bzlmod projects, V2 uses `bazel mod show_repo` to robustly extract dependency URLs from external repositories.
  - For WORKSPACE-based projects, V2 uses the same XML parsing approach as before.
- **Multiple Rule Support:** You can specify which workspace rules to use via the `detect.bazel.workspace.rules` property, or let the detector auto-detect them.

## Usage

The Bazel V2 detector runs automatically if a Bazel build target is provided using the `--detect.bazel.target` property. The Bazel executable must be available on your `$PATH`.

### Example Command
```sh
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.bazel.target='//myproject:mytarget'
```

### Controlling Workspace Rules
By default, the detector probes your project to determine which supported workspace rules are present. To override this, set the `detect.bazel.workspace.rules` property (comma-separated list, e.g. `MAVEN_INSTALL,HTTP_ARCHIVE`).

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
- Ensure the Bazel executable is on your $PATH.
- If dependencies are missing, check that the correct workspace rules are enabled.
- For bzlmod projects, ensure you are using a supported Bazel version.
- For further help, contact support.

## Notes
- The Bazel V2 detector is the default for all supported Bazel projects.
