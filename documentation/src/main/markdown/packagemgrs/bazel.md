# Bazel support

## Related properties

[Detector properties](../properties/detectors/bazel.md)

## Overview

[detect_product_short] provides limited support for Bazel projects.

[detect_product_short] discovers dependencies specified in *maven_jar*, *maven_install*, *haskell_cabal_library* workspace rules.
It also discovers library dependencies that have a github released artifact location (URL) specified in an *http_archive* workspace rule (see below for details).

The [detect_product_short] Bazel tool attempts to run on your project if you provide a Bazel build target using the Bazel target property.

The Bazel tool also requires a bazel executable on $PATH.

By default, [detect_product_short] determines the set of supported workspace rules that your project uses by parsing the WORKSPACE file,
and executes Bazel commands to discover dependencies for each supported workspace rule it finds.
Alternatively, you can directly control the set of workspace rules [detect_product_short] uses by setting the Bazel workspace rule property.
Refer to [Properties](../properties/detectors/bazel.md) for details.

## Processing for the *maven_install* workspace rule

The Bazel tool runs a bazel cquery on the given target to produce output from which it can parse artifact details such as group, artifact, and version for dependencies.

[detect_product_short]'s Bazel detector uses commands very similar to the following
to discover *maven_install* dependencies.
```
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

## Processing for the *maven_jar* workspace rule

The Bazel tool runs a bazel query on the given target to get a list of jar dependencies. On each jar dependency, the Bazel tool runs another bazel query to get its artifact details: group, artifact, and version.

The following is an example using the equivalent commands that [detect_product_short] runs, but from the command line, showing how [detect_product_short]'s Bazel detector currently identifies components.
First, it gets a list of dependencies:
```
$ bazel cquery 'filter("@.*:jar", deps(//:ProjectRunner))'
INFO: Invocation ID: dfe8718d-b4db-4bd9-b9b9-57842cca3fb4
@org_apache_commons_commons_io//jar:jar
@com_google_guava_guava//jar:jar
Loading: 0 packages loaded
```
Then, it gets details for each dependency. It prepends //external: to the dependency name for this command.
```
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

## Processing for the *haskell_cabal_library* workspace rule

Requires Bazel 2.1.0 or later.

[detect_product_short]'s Bazel detector runs a bazel cquery on the given target to produce output from which it can
extract artifact project and version for dependencies.

The Bazel detector uses a command very similar to the following
to discover *haskell_cabal_library* dependencies.
```
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

It then uses Gson to parse the JSON output into a parse tree,
and extracts the name and version from the corresponding rule attributes.


## Processing for the *http_archive* workspace rule

[detect_product_short] discovers library dependencies that have a github released artifact location (URL) specified in an *http_archive* workspace rule.

One of the URLs provided in the *http_archve* rule must match one of the following two forms:

* https://github.com/{organization}/{repo}/archive/{version}.{extension}
* https://github.com/{organization}/{repo}/releases/download/{version}/{filename}.{extension}

Supported extensions: .tar.gz, .zip, .gz

The Bazel tool runs a bazel query on the given target to get a list of library dependencies. On each dependency in the output, the Bazel tool runs another bazel query to get its artifact details; specifically the github released artifact locations (URLs) within http_archive, go_repository, and git_repository rules.

The following is an example using the equivalent commands that [detect_product_short] runs, but from the command line, showing how [detect_product_short]'s Bazel detector currently identifies components.
First, it gets a list of dependencies:
```
# bazel query 'kind(.*library, deps(//:bd_bazel))'
Starting local Bazel server and connecting to it...
@bazel_tools//third_party/def_parser:def_parser_lib
@bazel_tools//tools/cpp:malloc
@com_github_gflags_gflags//:gflags
@glog//:glog
Loading: 11 packages loaded
```
Then, it gets details for each http_archive dependency. It prepends //external: to the dependency name and runs a command similar to this:
```
# bazel query 'kind(.*, //external:com_github_gflags_gflags)' --output xml
<?xml version="1.1" encoding="UTF-8" standalone="no"?>
<query version="2">
    <rule class="http_archive" location="/home/steve/src/bd_bazel/WORKSPACE:4:13" name="//external:com_github_gflags_gflags">
        <string name="name" value="com_github_gflags_gflags"/>
        <list name="urls">
            <string value="https://github.com/gflags/gflags/archive/v2.2.2.tar.gz"/>
        </list>
        <string name="sha256" value="34af2f15cf7367513b352bdcd2493ab14ce43692d2dcd9dfc499492966c64dcf"/>
        <string name="strip_prefix" value="gflags-2.2.2"/>
    </rule>
</query>
Loading: 0 packages loaded
```
Finally, for each http_archive, go_repository, and git_repository rule in the output, [detect_product_short] parses the github dependency name (organization/repo) and version from each github released artifact location (in the url field or urls list, whichever is present).

## Examples

### mvn_install rule example

The following example will (if you add your [bd_product_short] connection details
to the [detect_product_short] command line) run the Bazel tool on the
*//tests/integration:ArtifactExclusionsTest* target in the
rules_jvm_external project and discover dependencies defined with the
maven_install repository rule:

````
git clone https://github.com/bazelbuild/rules_jvm_external
cd rules_jvm_external/
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.bazel.target='//tests/integration:ArtifactExclusionsTest'
````

### haskell_cabal_library rule example

The following example will (if you add your [bd_product_short] connection details
to the [detect_product_short] command line) run the Bazel tool on the
*//cat_hs/lib/args:args* target in the
rules_haskell/examples project and discover dependencies defined with the
haskell_cabal_library repository rule:

````
git clone https://github.com/tweag/rules_haskell.git
cd rules_haskell/examples
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.bazel.target='//cat_hs/lib/args:args'
````
