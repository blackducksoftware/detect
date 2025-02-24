<!-- Check the support matrix to determine unsupported releases -->
# Release notes for older versions

## Version 7.14.0

### New features
* Added support for Swift projects built with Swift 5.6 or later.

* Added support for running IaC scans via [company_name] [solution_name]. See [IaC Scan](runningdetect/iacscan.md) for more details. Note: IaC capabilities require Black Duck 2022.7.0 or later.

## Version 7.13.2

* (IDETECT-3291) Resolved an issue where the NuGet Inspector would only be found for the first applicable detector.
* (IDETECT-3289) Resolved an issue where the NuGet Inspector could not handle Implicit Dependencies in a Package Reference. 

## Version 7.13.1

* (IDETECT-3286) Resolved an issue that caused the 7.13.0 .jar to be unsigned. The 7.13.1 .jar is signed.

## Version 7.13.0

### New features
* Added support for a buildless Pipenv detector that parses the Pipfile.lock file (see the [python support page](packagemgrs/python.md) for more details).
* [company_name] [solution_name] now includes pass-through properties when logging configuration at the beginning of a run.
* Added support for Xcode Workspaces (see the [swift support page](packagemgrs/swift.md) for more details).

### Changed features
* Nuget Inspector is now shipped as a self-contained executable and has no runtime requirements. 
* Deprecated property detect.detector.buildless, to be replaced with detect.accuracy.required. See [property description](properties/configuration/detector.md#detector-accuracy-requirements-advanced) for more details.

### Resolved issues
* (IDETECT-3136) Resolved an issue where NPM's `package-lock.json` was prioritized over `npm-shrinkwrap.json`.
* (IDETECT-3184) Resolved an issue that prevented matches for Bazel maven_install components with complex (>3 parts) maven_coordinates values.
* (IDETECT-3207) Resolved an issue that prevented Bazel and Docker Tool issues from being reported in the issues section of the [company_name] [solution_name] log and status file.

### Dependency update

* Upgraded to Spring Boot version 2.6.6 / Spring version 5.3.18.

## Version 7.12.1

### Changed Features

* When signature scanning is skipped due to the minimum scan interval on Black Duck not being met, [company_name] [solution_name] by default will treat the run as a success and will not wait for the skipped scan(s). If the property detect.force.success.on.skip is set to false, [company_name] [solution_name] will instead return exit code 13 when one or more signature scans were skipped.

## Version 7.12.0

### New features

* Verified support for Java 16 and 17.
* Added new properties detect.gradle.excluded.project.paths and detect.gradle.included.project.paths to allow filtering on paths which gradle guarantees to be unique.
* Added support for vendoring Go Mod dependencies using [detect.go.mod.dependency.types.excluded=VENDORED](properties/detectors/go.md#go-mod-dependency-types-excluded) to exclude *test* and *build system* dependencies from Go modules declaring a version prior to `Go 1.16`.
* Added a feature that allows users to configure [company_name] [solution_name] to fail when policies of a certain name are violated.
  See [detect.policy.check.fail.on.names](properties/configuration/project.md#fail-on-policy-names-with-violations) for details. Note: this feature requires Black Duck 2022.2.0 or later.
* Added Rapid Compare Mode which enables returning only the differences in policy violations compared to a previous scan.

### Changed features

* Changed default value of detect.project.clone.categories from ALL to COMPONENT_DATA, CUSTOM_FIELD_DATA, LICENSE_TERM_FULFILLMENT, VERSION_SETTINGS, VULN_DATA.  This avoids the automatic setting of the clone category DEEP_LICENSE introduced in Black Duck 2022.2.0. Users of [company_name] [solution_name] 7.12.0 that wish to pass DEEP_LICENSE or ALL as a value to detect.project.clone.categories must be using Black Duck 2022.2.0 or later.
* Added new property detect.bazel.workspace.rules to replace the now deprecated detect.bazel.dependency.type property.
* For Go Mod projects, successfully executing `go version` is now required. Unsuccessful attempts now result in a run failure.
* The property *detect.go.mod.dependency.types.excluded* now only accepts a single value rather than a list of values.

### Resolved issues

* (IDETECT-3016) Resolved an issue where proxies may block HEAD requests made by [company_name] [solution_name] when attempting to download the Signature Scanner from Black Duck. Because the
  criteria that [company_name] [solution_name] uses to download the Black Duck Signature Scanner is new, the next run will re-download the Signature Scanner.
* (IDETECT-3165) Resolved an issue that could cause the Bitbake detector to fail with error `Graph Node recipe ... does not correspond to any known layer`.

## Version 7.11.1

### Changed features

* Updated [company_name] [solution_name] to package air gap with the latest Nuget Inspectors: IntegrationNugetInspector:3.1.1, BlackduckNugetInspector:1.1.1, NugetDotnet3Inspector:1.1.1,
  NugetDotnet5Inspector:1.1.1.

## Version 7.11.0

### New features

* Added a feature that allows users to set a license for a project version using the
  property [detect.project.version.license](properties/configuration/project.md#project-version-license).
* Added support for identifying dependency relationships between Linux package manager components in images.

### Changed features

* The Go Mod Cli Detector no longer uses the "-u" flag when running **go list -m all**. This results in significantly faster scan times against Go Mod projects.
* Deprecated the `detect.pnpm.dependency.types` property in favor of `detect.pnpm.dependency.types.excluded` for property consistency.

### Resolved issues

* (IDETECT-2925) Resolved an issue that could cause the Bitbake detector to incorrectly identify the layer of a dependency recipe.
* (IDETECT-3080) Fixed an issue where [company_name] [solution_name] would not include multiple versions of the same package in Cargo projects.
* (IDETECT-3012) Resolved an issue that caused [company_name] [solution_name] to incorrectly use BLACKDUCK_USERNAME and BLACKDUCK_PASSWORD.

## Version 7.10.0

### New features

* Added support for the Apache Ivy package manager.
* Build dependencies can now be excluded from BitBake results.

### Changed features

* [company_name] [solution_name] now classifies empty code location warning messages as the DEBUG logging level instead of the previous classification as the WARN logging level.
* BitBake detector: Added support for BitBake 1.52 (Yocto 3.4).
* BitBake detector: Added support for BitBake projects with build directories that reside outside the project directory.
* Deprecated many properties relating to filtering dependency types from the BOM. These property replacements will reduce the number of properties, apply consistency to detector
  properties, add filtering abilities, and overall simplify Detect configuration.
    * Deprecated the following properties:
        * detect.conan.include.build.dependencies
        * detect.pub.deps.exclude.dev
        * detect.go.mod.enable.verification
        * detect.gradle.include.unresolved.configurations
        * detect.lerna.include.private
        * detect.npm.include.dev.dependencies
        * detect.npm.include.peer.dependencies
        * detect.packagist.include.dev.dependencies
        * detect.pear.only.required.deps
        * detect.ruby.include.runtime.dependencies
        * detect.ruby.include.dev.dependencies
        * detect.yarn.prod.only
    * Added the following replacement properties:
        * detect.conan.dependency.types.excluded
        * detect.pub.dependency.types.excluded
        * detect.go.mod.dependency.types.excluded
        * detect.gradle.configuration.types.excluded
        * detect.lerna.package.types.excluded
        * detect.npm.dependency.types.excluded
        * detect.packagist.dependency.types.excluded
        * detect.pear.dependency.types.excluded
        * detect.ruby.dependency.types.excluded
        * detect.yarn.dependency.types.excluded

### Resolved issues

* (IDETECT-2949) Fixed an issue where [company_name] [solution_name] failed to properly parse Go module version names containing '-' characters.
* (IDETECT-2959) Fixed an issue where [company_name] [solution_name] would not fail when running `go mod why` fails.
* (IDETECT-2971) Fixed an issue where [company_name] [solution_name] would not produce unique code location paths for Pnpm projects.
* (IDETECT-2939) Fixed an issue where NPM projects that had no declared dependencies would not exclude peer or dev dependencies.
* (IDETECT-3038) Fixed an issue where [company_name] [solution_name] would fail to parse file dependency declarations for pnpm projects in their pnpm-lock.yaml files.
* (IDETECT-3000) Fixed an issue where [company_name] [solution_name] would error out when a user's source directory and output directory did not share a common root.

## Version 7.9.0

### New features

* Added support for the Xcode Swift Package Manager for Xcode projects using the built-in [Swift Packages](https://developer.apple.com/documentation/swift_packages) feature.
* Added detect.bdio.file.name to specify the name of the output BDIO file.
* Added system architecture DEBUG level logs to assist with support.

### Changed features

* The version of each package manager tool executed by CLI detectors is now logged at DEBUG level.

### Resolved issues

* (IDETECT-2499) Fixed an issue in the Gradle Inspector that caused it to exclude all identically named subprojects except one.
* (IDETECT-2953) Fixed the project and project version links in risk report.
* (IDETECT-2989) Fixed an issue with Go Mod projects where [company_name] [solution_name] included unused transitive dependencies, despite detect.go.mod.enable.verification being set to 'true'.
* (IDETECT-2935) Verified that [company_name] [solution_name] is compatible with Gradle version 7.X.

## Version 7.8.0

### New features

* Added support for the pnpm package manager.
* Added property detect.project.group.name for setting the Project Group.
* [company_name] [solution_name] now falls back to using a previously downloaded Docker Inspector, Project Inspector, and/or NuGet Inspector when https://sig-repo.synopsys.com is unreachable.

## Version 7.7.0

### New features

* Added support for uploading rapid scan config file when a file named '.bd-rapid-scan.yaml' is present in the source directory.
* Added the property detect.project.inspector.arguments for providing additional arguments to the project inspector across all invocations.

### Resolved issues

* (IDETECT-2808, IDETECT-2863) Resolved an issue where [company_name] [solution_name] would incorrectly resolve relative paths when processing signature scan targets.
* (IDETECT-2859) Resolved an issue where [company_name] [solution_name] was using an outdated cookie spec when making a request, resulting in a warning message.

## Version 7.6.0

### New features

* Added the property [detect.follow.symbolic.links](properties/configuration/general.md#follow-symbolic-links) which can be used to enable [company_name] [solution_name] to follow symbolic links
  when searching directories for detectors, when creating exclusions for signature scan, and when creating binary scan targets.
* Added support for Open Container Initiative (OCI) images provided to [company_name] [solution_name] using the *detect.docker.tar* property.
* Added the property [detect.gradle.include.unresolved.configurations](properties/detectors/gradle.md#gradle-include-unresolved-configurations-advanced) for toggling the inclusion
  of [unresolved Gradle configurations](https://docs.gradle.org/7.2/userguide/declaring_dependencies.html#sec:resolvable-consumable-configs).
* Added Project Inspector support for MAVEN and GRADLE when resolving buildless dependencies.
* Added support for NUGET buildless using the Project Inspector.

### Changed features

* The detect.project.clone.categories property now supports ALL and NONE as options.
* The default value for property detect.project.clone.categories has changed to ALL.
* Deprecated detect.bom.aggregate.name, detect.bom.aggregate.remediation.mode, and blackduck.legacy.upload.enabled. In version 8, [company_name] [solution_name] will only operate in SUBPROJECT
  aggregation mode to report the dependency graph with greater accuracy.
* Maven defaults to the legacy buildless parser, Project Inspector must be enabled with detect.maven.buildless.legacy.mode. In version 8, it will default to Project Inspector.
* Deprecated detect.maven.include.plugins as Project Inspector does not support plugins. In version 8, we will only support the Project Inspector Maven implementation which will
  have its own configuration mechanism.
* The air gap zip generation options no longer support individual package managers. Instead, either a FULL air gap can be created, or a NO_DOCKER air gap can be created. This is to
  help support project inspector which spans multiple package managers.

### Resolved issues

* (IDETECT-2834) Resolved an issue where GoMod components missing a version were not being properly filtered causing a NullPointerException.
* (IDETECT-2829) Resolved an issue that caused [company_name] [solution_name] to use the wrong scan cli when in offline mode and ignore a specified local scan cli.
* (IDETECT-2820) Resolved an issue where pypi components in conda projects were not being matched.
* (IDETECT-2773) Resolved an issue where [company_name] [solution_name] was not replacing module paths as specified in go mod replace statements.

## Version 7.5.0

### New features

* Added support for the Dart package manager.

### Changed features

* The following directories are no longer excluded from Signature Scan by default: bin, build, out, packages, target. .synopsys directories are now excluded from both Detector
  search and Signature Scan.
* The Docker Inspector can now be included (using the detect.tools property) when using
  the [rapid scan mode](properties/configuration/blackduck-server.md#detect-scan-mode-advanced).
* Instead of "lite" Docker images that automatically disable all detectors, [company_name] [solution_name] now supports "buildless" Docker images that automatically disable detectors that depend
  on the presence of build tools but leave buildless detectors enabled.

### Resolved issues

* (IDETECT-2830) Resolved an issue that caused the Gradle detector to fail when run in air gap mode.
* (IDETECT-2816) Resolved an issue that caused a "Duplicate key" error when running binary scan on multiple files with the same name.

## Version 7.4.0

### New features

* Added SUBPROJECT remediation mode, invoked using property detect.bom.aggregate.remediation.mode. Use only with Black Duck 2021.8.0 or later.

## Version 7.3.0

### New features

* Added support for the Carthage package manager.

### Changed features

* The Poetry detector is no longer categorized as a PIP detector and is now categorized under detector type POETRY.
* Simplified the property deprecation lifecycle to the following: Use of deprecated properties will result in logged warnings until the next major version release, at which time
  those properties will be removed from Detect (and ignored if used). Properties that were deprecated in Detect 6.x have been removed in this release. Properties deprecated in
  Detect 7.x will be removed in Detect 8.0.0.

## Version 7.2.0

### Changed features

* Improved the readability of Rapid mode results.

### Resolved issues

* (IDETECT-2532) Resolved an issue that could cause multiple versions of Go-Mod dependencies to appear in the BOM.
* (IDETECT-2668) Resolved an issue that caused Go-Mod dependencies with a replacement version to be omitted.
* (IDETECT-2722) Resolved an issue that caused the version to be omitted from Go-Vendr dependencies when the vendor.conf separated dependency name and version with multiple space
  characters.
* (IDETECT-2672) Resolved an issue that could cause the Black Duck access token to appear in the log.
* (IDETECT-2739) Resolved an issue that caused the default to be used when the provided Risk Report path did not exist.

## Version 7.1.0

### New features

* Added ability to specify custom fonts to be used during risk report generation. See [here](results/reports.md#risk-report-generation) for more details.
* There now exist Docker images that can be used to run [company_name] [solution_name] from within a container.
  See [Running Synopsys Detect from within a Docker container](runningdetect/runincontainer.md#running-synopsys-detect-from-within-a-docker-container) for more details.
* Added detect.go.mod.enable.verification for disabling the `go mod why` check that [company_name] [solution_name] uses to filter out unused dependencies.
* Added support for dotnet 5 when running the NuGet inspector.
* Added a new property [detect.npm.include.peer.dependencies](properties/detectors/npm.md#include-npm-peer-dependencies) which allows the users to filter out NPM peer dependencies
  from their BOM.

### Changed features

* The following clone categories were added to the default value for property detect.project.clone.categories: LICENSE_TERM_FULFILLMENT, CUSTOM_FIELD_DATA
* The "Git Cli" detector has been renamed to the "Git" detector.
* Whenever [company_name] [solution_name] runs the following tools, it now logs (at level DEBUG) the tool's version: git, gradle, maven, conan, pip, and python.

### Resolved issues

* (IDETECT-2541) Resolved an issue that caused the CLANG detector to fail when run in non-English locales on Ubuntu and Debian systems.
* (IDETECT-2505) Resolved an issue that caused go mod components with +incompatible version suffixes to not be matched on Black Duck.
* (IDETECT-2629) Resolved an issue that caused go mod projects without source having an empty BOM with the introduction of the detect.go.mod.enable.verification property.
* (IDETECT-2659) Resolved an issue that caused [company_name] [solution_name] to falsely report a missing detector when that detector matched only at a depths > 0 and was included in the value of
  property detect.required.detector.types.
* (IDETECT-2696) Resolved an issue that could cause [company_name] [solution_name] to fail with "IllegalStateException: Duplicate key {codelocation name}" when creating >100 codelocations in one
  run.
* (IDETECT-2659) Resolved an issue that caused Detect to falsely report "One or more required detector types were not found" when the required detector ran based on files found in
  a subdirectory.
* (IDETECT-2541) Resolved an issue that caused the CLANG detector to fail with "Unable to execute any supported package manager" when run with a non-English locale on an alpine
  system.

## Version 7.0.0

### New features

* Added scripts detect7.sh and detect7.ps1 for invoking [company_name] [solution_name] 7.x.x. detect.sh and detect.ps1 will (by default) continue to invoke the latest [company_name] [solution_name] 6 version.
* Added support for Yarn workspaces.
* Added support for the dependency graph SBT plugin. Resolution cache generation is no longer a requirement of the SBT detector.
* Added the properties [detect.excluded.directories](properties/configuration/paths.md#detect-excluded-directories-advanced),
  [detect.excluded.directories.defaults.disabled](properties/configuration/paths.md#detect-excluded-directories-defaults-disabled-advanced),
  and [detect.excluded.directories.search.depth](properties/configuration/signature-scanner.md#detect-excluded-directories-search-depth) to handle exclusions for detector search
  and signature scanning.
* Added ability to specify excluded directory paths
  using [glob patterns](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)).
* Added properties [detect.lerna.excluded.packages](properties/detectors/lerna.md#lerna-packages-excluded-advanced)
  and [detect.lerna.included.packages](properties/detectors/lerna.md#lerna-packages-included-advanced) to exclude and include specific Lerna packages.
* Added critical security risks to the Black Duck Risk Report pdf.
* Added detect.target.type to enhance the docker user experience. When set to IMAGE, some tools are automatically disabled and detect optimizes for an image-based scan.
* Added binary scanning of the container filesystem to the default Docker image scanning workflow. If you are scanning Docker images and your Black Duck server does not have the
  binary scanning feature enabled; use --detect.tools.exluded=BINARY_SCAN to disable the binary scan step.

### Changed features

* The following directories will be excluded from signature scan by default, in addition to node_modules: bin, build, .git, .gradle, out, packages, target. Use
  [detect.excluded.directories.defaults](properties/configuration/paths.md#detect-excluded-directories-defaults-disabled-advanced) to disable these defaults.
* Detect no longer supports the exclusion of individual files during detector search, only directories.
* Gradle detector no longer uses the gradle inspector. Only the init script is required.
* The default BDIO format for communicating dependency graphs to Black Duck has been changed from BDIO1 to BDIO2.
* Risk report generation will download fonts from Artifactory or use the font files in the fonts directory in the air gap zip of detect.

### Resolved issues

* (IDETECT-2462) Resolved an issue where projects were being inaccurately diagnosed as Poetry projects due to the presence of a pyproject.toml file.
* (IDETECT-2527) Resolved an issue in the Go Mod detector to extract and process data even if 'go mod why' command fails to run.
* (IDETECT-2434) Resolved an issue in the CLANG detector on Ubuntu and Debian systems that caused it to omit a package when that package had been installed on the system from
  multiple architectures.
* (IDETECT-2362) The CLANG detector now uses the KB preferred alias namespace feature for improved match accuracy.
* (IDETECT-2413) Resolved an issue to upgrade internal dependencies to support JDK 15.
* (IDETECT-2409) Resolved an issue to allow Gradle detector to support Gradle 6.8.
* (IDETECT-2099) Improved error reporting for exceptions that occur during a Detect run. For each exception, a Detect "issue" is written to the log and to the status.json file.
* (IDETECT-2516) Improved error reporting for the case where environment variable BDS_JAVA_HOME is set incorrectly.

## Version 6.9.1

### Resolved issues

* (IDETECT-2555) Resolved an issue that could cause Detect, when run against Black Duck 2020.10.0, to fail with a message like: "Cannot cast... to...
  VersionBomCodeLocationBomComputedNotificationUserView".

## Version 6.9.0

### New features

* Added ability for detectors to explain why they applied. It will appear in the logs at info level and in the status.json.
* Added the property detect.binary.scan.search.depth to define the directory search depth for the binary scanner.
* The status.json file now features a list of the provided Detect property values.
* When Detect is not configured to connect to Black Duck or run offline, a link to the Detect help is included in an error message.

### Changed features

* Added the timezone to the date format in the default log message format.
* Reverted deprecations for detect.blackduck.signature.scanner.arguments, detect.blackduck.signature.scanner.copyright.search, detect.blackduck.signature.scanner.dry.run,
  detect.blackduck.signature.scanner.individual.file.matching, detect.blackduck.signature.scanner.license.search, detect.blackduck.signature.scanner.local.path,
  detect.blackduck.signature.scanner.paths, detect.blackduck.signature.scanner.snippet.matching, detect.blackduck.signature.scanner.upload.source.mode.

### Resolved issues

* (IDETECT-1986) Resolved an issue where warnings regarding reflective access appear at the start of Detect.
* (IDETECT-2400) Resolved an issue where 'dependencies' would be removed from the value of the detect.gradle.build.command property.
* (IDETECT-2394) Resolved an issue that created inaccurate relationships in the BDIO files when Gemlock files were processed.
* (IDETECT-2404) Resolved an issue where signature scanner arguments passed through detect.blackduck.signature.scanner.arguments that contained space were being improperly parsed.
* (IDETECT-2525) Resolved an issue with the Yarn detector that caused component version information to be missing when the yarn.lock file contained quoted field keys.
* (IDETECT-2254) Resolved an issue with the Yarn detector that caused certain components to be omitted from some Yarn 2 projects.
* (IDETECT-2471) Resolved an issue where a missing Git executable in certain situations causes an exception.

## Version 6.8.0

### New features

* Added support for Conan projects that have the Conan revisions feature enabled.
* Added detect.pip.path for advanced users who wish to specify which pip executable to run.
* Improved the Pip Inspector to attempt to discover files named "requirements.txt" if no requirements files are specified through detect.pip.requirements.path.

### Changed features

* Added detect.timeout to consolidate the functionality of blackduck.timeout and detect.report.timeout.
* Added date of latest scan for a project version to the risk report pdf.
* Deprecated properties blackduck.timeout and detect.report.timeout. They have been consolidated into the new property detect.timeout.
* Deprecated all Detect exclusion properties. Future releases will feature a new property to extend and consolidate these properties.
* Deprecated all Detect signature scanner properties. Future releases will feature an alternative mechanism for providing signature scanner arguments to Detect.
* Deprecated property detect.resolve.tilde.in.paths. Resolving tildes is a shell feature which Detect will no longer support in a future version.
* Deprecated property detect.python.python3. Due to the January 2020 sunset of Python 2, this property (which toggles between searching for a 'python' and 'python3' executable) is
  no longer necessary. See:
  [PEP-394](https://www.python.org/dev/peps/pep-0394.md#recommendation)
* Deprecated properties detect.docker.inspector.air.gap.path, detect.gradle.inspector.air.gap.path, and detect.docker.inspector.air.gap.path as part of an effort to simplify
  Detect.
* Deprecated properties detect.default.project.version.scheme, detect.default.project.version.text, detect.default.project.version.timeformat as part of the effort to simplify
  Detect.
* Deprecated properties blackduck.username and blackduck.password. Authentication should be performed using an API token.

### Resolved issues

* (IDETECT-2216) Resolved an issue that prevented non-ASCII filenames from being correctly transmitted to Black Duck during a binary scan file upload.
* (IDETECT-2227) Resolved an issue where Nuget Inspectors would parse source files for assembly version.
* (IDETECT-2281) Resolved an issue that included golang dependencies that were not linked in the compiled go
  application. [241](https://github.com/blackducksoftware/detect/issues/241)
* (IDETECT-2294) Resolved an issue where Git credentials could be logged when reading the remote URL.
* (IDETECT-2296) Resolved an issue wherein the Pip Inspector would cease parsing a requirements file if it encountered a dependency which it could not resolve.
* (IDETECT-2276) Resolved an issue that caused the CLANG detector to omit components for which multiple architectures are installed.

## Version 6.7.0

### Resolved issues

* (IDETECT-2285) Resolved an issue that could cause Detect to fail to authenticate with Black Duck with the error message "No Bearer token found when authenticating.".
* (IDETECT-2221) Resolved an issue where the Docker Inspector logging level was not set correctly when property logging.level.detect was used.
* (IDETECT-2213) Resolved an issue that could cause the CLANG detector to omit some components on Debian-based Linux systems.
* (IDETECT-2284) Resolved an issue that could cause the CLANG detector to omit some components for projects using the clang/clang++ compiler when source files reference include
  files using non-canonical paths.
* (IDETECT-2216) Resolved an issue that caused non-ASCII characters in binary scan metadata (filename, code location name, project name, and version name) to be converted to '?'
  characters when submitted to Black Duck.
* (IDETECT-2291) Reverted replacement data support. Detect will report exactly what Gradle reports. This reverts IDETECT-2038, IDETECT-2203.
* (IDETECT-2241) Resolved an issue where platform dependent cocoapods will throw an exception when they are not installed.
* (IDETECT-2289) Resolved an issue that could cause Black Duck API token-based authorization to fail with "411 Length Required" HTTP status when communicating with Black Duck
  through a proxy.

## Version 6.6.0

### Changed features

* The Docker Inspector now works on Windows 10 Enterprise.
* Upon connecting to Black Duck, the users' roles and groups, which are only used in DEBUG-level logging, are no longer fetched unless logging level is DEBUG or higher.
* The error messages produced for binary scan file upload failures have been improved.
* The "detectors" field in the status.json file now features status data with more-expressive error codes derived from the runtime class of a detectable result.
* Detect will follow 308 redirects when communicating with Black Duck.

### Resolved issues

* (IDETECT-2038, IDETECT-2203) Resolved an issue where the Gradle Inspector would produce false positives in Gradle because of dependency replacement from the root project.
* (IDETECT-2180) Resolved and issue where the Pip Inspector would fail against requirements.txt files generated by the pip-compile tool.
* (IDETECT-2108) Resolved an issue where Lerna packages where being reported as missing dependencies.
* (IDETECT-2138, IDETECT-2161, IDETECT-2172) Resolved issue where Gradle parse detector would fail due to an inability to resolve classes, referenced in the project's build
  scripts, that were outside of Detect's classpath.
* (IDETECT-2110) Nuget inspectors will correctly return -1 when an error occurs by default.
* (IDETECT-2202) Impact analysis code locations will now appear in the status.json file.

### Known issues

* When running the Docker Inspector on Windows, [company_name] [solution_name] may fail to clean up all its working directories (and log the message "Error trying cleanup") due to the
  following Docker issue:
  https://github.com/docker/for-win/issues/394.
* False positives from Gradle are still possible if the replacement dependency is defined within a subproject that has subprojects. Work is being continued to fix this with
  IDETECT-2218.

## Version 6.5.0

### New features

* Added [properties](properties/configuration/debug.md#diagnostic-mode) for enabling diagnostic mode.
* [company_name] [solution_name] now supports Vulnerability Impact Analysis. Enabled
  using [Vulnerability Impact Analysis Enabled](properties/configuration/impact-analysis.md#vulnerability-impact-analysis-enabled) property.

### Changed features

* Enabling diagnostic mode is now controlled through two new properties.
	* [--detect.diagnostic](properties/configuration/debug.md#diagnostic-mode)
	* [--detect.diagnostic.extended](properties/configuration/debug.md#diagnostic-mode-extended)
* The *detect.bazel.dependency.type* property now accepts a comma-separated list of dependency types, or the value *NONE*, or the value *ALL*.

### Resolved issues

* (IDETECT-2054) Resolved an issue that caused the Gradle Inspector to fail when detect.output.path is set to a relative path.

## Version 6.4.2

### Resolved issues

* (IDETECT-2164) Resolved an issue with scanning Go applications when using the go list -m command, which couldn't determine available upgrades using the vendor directory.

## Version 6.4.0

### New features

* Bazel detector: added support for Bazel projects that specify dependencies using the haskell_cabal_library repository rule.
* NuGet detector: added support for DotNet 3.1 runtime.
* [company_name] [solution_name] now supports projects managed by the Lerna package manager.
* [company_name] [solution_name] now supports projects managed by the Cargo package manager.
* [company_name] [solution_name] now supports projects managed by the Poetry package manager.

### Changed features

* Eliminated any need for the [blackduck_product_name] Global Code Scanner overall role.
* The CLANG detector collects any dependency files not recognized by the Linux package manager that reside outside the source directory (the directory containing the
  compile_commands.json file) and writes them to the status.json file.
* Added the property [detect.blackduck.signature.scanner.copyright.search](properties/configuration/signature-scanner.md#signature-scanner-copyright-search-advanced).
* Removed PipEnv from the list of buildless detectors as it was never buildless.
* Improved output for signature scanner status and included descriptions for exit codes when reporting overall status.
* Status.json file now collects code location data generated by all tools, not just detectors.
* Status.json file now collects issue data generated by all tools.

### Resolved issues

* (IDETECT-2019) Resolved an issue where the pip inspector would not be able to parse the requirements.txt file if pip's version was >= 20.1.
* (IDETECT-2034) Resolved an issue that would cause a NullPointerException when [company_name] [solution_name]'s initial attempt at generating a code location name produced a code location name
  greater than 250 characters and either code location prefix or code location suffix is not set.
* (IDETECT-1979) Resolved an issue that could cause the CLANG detector to miss some dependencies because it failed to correctly parse complex nested quoted strings within
  compile_commands.json values.
* (IDETECT-1966) Resolved an issue that would cause Detect to ignore replacement directives for Go Mod projects.

### Known Issues

* When a Lerna package depends on another Lerna package within the project, an error may appear indicating a missing dependency on that package. This is normal and no dependencies
  are missing. This will be fixed in a future release.

## Version 6.3.0

### New features

* The Yarn detector now extracts project information from package.json files. Git is no longer the default supplier of project information for Yarn projects.
* Added Yarn Detector support for dependencies that are missing a fuzzy version in a lockfile dependency declaration.
* [company_name] [solution_name] logs policy violations when it is configured to [fail on policy violations](properties/configuration/project.md#fail-on-policy-violation-severities).

### Changed features

* Users can [upload source](properties/configuration/signature-scanner.md#upload-source-mode) files
  when [license search](properties/configuration/signature-scanner.md#signature-scanner-license-search) is enabled regardless of whether
  [snippet matching](properties/configuration/signature-scanner.md#snippet-matching) has been enabled.
* [company_name] [solution_name] is now compatible with Yocto 3.0.
* [company_name] [solution_name] stops if the Docker Inspector tool applies and [company_name] [solution_name] is running on Windows.
* [company_name] [solution_name] configures Docker Inspector's working directories inside [company_name] [solution_name]'s run directory.
* [company_name] [solution_name] requires and runs Docker Inspector version 9.
* Moved the location to which detect.sh downloads the [company_name] [solution_name] .jar from /tmp to ~/synopsys-detect/download.

### Resolved issues

* (IDETECT-1906) Resolved an issue wherein git extraction might fail if "git log" returned unexpected output. As a last resort, the commit hash will be used as a version.
* (IDETECT-1883) Resolved an issue where [company_name] [solution_name] failed to extract project information when parsing a Git repository with a detached head while in buildless mode.
* (IDETECT-1970) Resolved an issue where the default value for [parallel processors](properties/configuration/general.md#detect-parallel-processors-advanced) was not used. The
  available runtime processor count was being used instead.
* (IDETECT-1973) Resolved an issue where the NuGet exe inspector would not resolve from Artifactory.
* (IDETECT-1965) Resolved an issue where [company_name] [solution_name] would fail to resolve environment variables where it did so previously.
* (IDETECT-1974) Resolved an issue wherein the Yarn detector was throwing an exception for dependencies not defined in the yarn.lock file.
* (IDETECT-2037) Resolved an issue where [company_name] [solution_name] would fail with a "hostname in certificate didn't match" error while downloading the Gradle inspector.

## Version 6.2.1

### Resolved issues

* Resolved an issue wherein an exception was thrown when generating a risk report if users didn't set the risk report output path explicitly. (IDETECT-1960)

## Version 6.2.0

### New features

* The [company_name] [solution_name] .jar file is now signed, enabling [code verification](downloadingandinstalling/verification.md) by users.
* [Simple proxy information](packagemgrs/gradle.md#running-the-gradle-inspector-with-a-proxy) will be forwarded to the Gradle Inspector.
* Detect now creates a status file describing the results of the run which includes things like [issues, results, and status codes.](runningdetect/status-file.md)
* The property configuration table in the log now includes the origin of the property's value.
* Added the property [detect.blackduck.signature.scanner.license.search](properties/configuration/signature-scanner.md#signature-scanner-license-search-advanced).
* Added the property [detect.blackduck.signature.scanner.individual.file.matching](properties/configuration/signature-scanner.md#individual-file-matching-advanced).
* If an executable returns a nonzero exit code, Detect will now log the executable output automatically.
* Added page for deprecated properties in help.
* Detect-generated risk reports now feature Synopsys logo and branding.

### Changed features

* The PipEnv Detector now parses a json representation of the dependency tree.
* Powershell download speed increased.

### Resolved issues

* Resolved an issue where the download URL for [company_name] [solution_name] was being set to an internal URL upon release (IDETECT-1847).
* Resolved an issue where all transitive dependencies found by the Pip inspector were being reported as direct dependencies (IDETECT-1893).
* Resolved an issue where using pip version 20+ with the Pip inspector caused a failure to import a
  dependency. [GitHub PR](https://github.com/blackducksoftware/detect/pull/107) (IDETECT-1868)
* Resolved the following vulnerabilities (IDETECT-1872): org.springframework.boot:spring-boot-starter 5.1.7.RELEASE BDSA-2020-0069 (CVE-2020-5398)
* Resolved an issue where [company_name] [solution_name] had the potential to fail on projects that utilized Yarn workspaces (IDETECT-1916).
* Note: Yarn workspaces are not currently supported. See [yarn workspace support](packagemgrs/yarn.md#yarn-workspace-support).
* Resolved an issue in the Bazel Detector that caused it to fail for the maven_install rule when the tags field contained multiple tags with a mixture of formats (IDETECT-1925).
* When parsing package.xml files, Detect will no longer raise a SAXParseException when the file contains a doctype declaration, and will continue parsing the rest of the file (
  IDETECT-1866).
* Resolved an issue that could cause generation of an invalid Black Duck Input/Output (BDIO) file when the only differences between two component names/versions are
  non-alphanumeric characters (IDETECT-1856).

## Version 6.1.0

### New features

* Added the following properties:   
* [detect.bdio2.enabled](properties/configuration/paths.md#bdio-2-enabled).
* [detect.pip.only.project.tree](properties/detectors/pip.md#pip-include-only-project-tree).
* [detect.bitbake.search.depth](properties/detectors/bitbake.md#bitbake-search-depth).
* [detect.bazel.cquery.options](properties/detectors/bazel.md#bazel-cquery-additional-options).
* [detect.docker.image.id](properties/detectors/docker.md#docker-image-id).
* [detect.docker.platform.top.layer.id](properties/detectors/docker.md#platform-top-layer-id-advanced).
* [detect.bom.aggregate.remediation.mode](properties/configuration/project.md#bdio-aggregate-remediation-mode-advanced)

### Changed features

* Deprecated all Polaris specific properties.
* Added wildcard support for several include/exclude list properties.
* Improved the structure of the dependency information produced by the Yarn detector by changing its approach. It now parses dependency information from yarn.lock and package.json,
  instead of running the yarn command. Since the yarn command is no longer executed, the detect.yarn.path property has been removed.
* Improved match accuracy for Bitbake projects by improving external ID generation for dependencies referenced using Git protocols, and dependencies referenced with an epoch and/or
  revision.
* Improved the reliability of the Bitbake detector by generating recipe-depends.dot and package-depends.dot files the source directory, instead of a temporary directory.
* Changed the logging level of Polaris CLI output from DEBUG to INFO.
* Added support for the Noto-CJK font (for Chinese, Japanese, and Korean text) in the risk report.

### Resolved issues

* Resolved an issue that can cause a Null Pointer Exception on Maven projects configured for multi-threaded builds.
* Resolved an issue that can cause Detect to fail due to an expired Black Duck bearer token.
* Resolved an issue that causes Detect to fail when a parent project and version are specified, and the project is already a child of the specified parent.
* Resolved an issue that causes Detect to log the git username and password when a git command executed by Detect fails.
* Resolved an issue that can cause Detect to generate a new code location (scan) when the character case of the value of the detect.source.path property differs from a previous run
  on the same project.
* Resolved the following vulnerabilities: commons-beanutils:commons-beanutils 1.9.3 / BDSA-2014-0129 (CVE-2019-10086), org.apache.commons:commons-compress 1.18 / BDSA-2019-2725 (
  CVE-2019-12402)

## Version 6.0.0

### New features

* Added the property detect.binary.scan.file.name.patterns.
* Added the property detect.detector.search.exclusion.files which accepts a comma-separated list of file names to exclude from the Detector search.
* Custom arguments for the source command can now be supplied to Detect through the property detect.bitbake.source.arguments which accepts a comma-separated list of arguments. (IDETECT-1614)
* Added support for the Swift package manager.
* Added support for GoGradle.
* Added support for Go Modules.
* The property detect.pip.requirements.path is now a comma-separated list of paths to requirements.txt files. This enables you to specify multiple requirements files. Each
  requirements file displays as a new code location in Black Duck.
* Detect now logs username, roles, and groups for the current user.
* Detect now includes the project name/version in every code location name.
* Detect now takes in a go path but does not take in go.dep.path; nor does Detect trigger on *.go.
* The property detect.parallel.processors is added. This property controls the number of parallel threads, and replaces the properties
  detect.blackduck.signature.scanner.parallel.processors and detect.hub.signature.scanner.parallel.processors.
* Added the property detect.maven.included.scopes. This is a comma-separated list of Maven scopes. Output is limited to dependencies within these scopes, and is overridden by
  exclude.
* Added the property detect.maven.excluded.scopes. This is a comma-separated list of Maven scopes. Output is limited to dependencies outside these scopes, and is overridden by
  include.
* Bazel detector: added support for dependencies specified using the maven_install workspace rule. The detect.bazel.advanced.rules.path property is removed.
* When using Detect for static analysis, you can pass the build command to let the Polaris CLI know how to analyze a given project.

### Changed features

* Architecture is no longer included in BitBake dependencies discovered by Detect. The property detect.bitbake.reference.impl is no longer used and is deprecated.
* The BitBake detector no longer uses the property detect.bitbake.reference.impl because architecture is no longer required to match with artifacts in the KnowledgeBase. The
  Bitbake detector now attempts to determine the layer in which a component originated instead of the architecture.
* Improved the Detect on-screen logging to be more concise.
* The PiP inspector is no longer deprecated and is currently supported.
* When creating an air gap zip of Detect using the switch -z or --zip, the created zip file is now published to your output directory.
* Scripts no longer fail if the Artifactory server is unavailable.
* Enhanced placement and formatting of deprecation logs.
* Added support for Java version 11.
* The following properties are removed in Detect version 6.0.0:   
	* detect.go.dep.path
	* detect.npm.node.path
	* detect.perl.path
	* detect.go.run.dep.init
	* detect.maven.scope
	* detect.bazel.advanced.rules.path

### Resolved issues

* Resolved an issue wherein the Windows Java path construction did not account for direction of the slash. The shell script now uses the correct slash direction, based on the
  operating system on which Detect is running.
* Resolved an issue wherein Detect was not finding the file recipe-depends.dot written to the current directory. Detect now looks in the source directory to a depth of 1 if it
  cannot find the expected files in the expected location.
* Resolved an issue wherein Detect was failing if it could not resolve placeholders.
* Resolved an issue wherein Detect was not handling SSH URLs, which caused Detect to fail in extracting project information from the Git executable. GitCliDetectable now properly
  handles SSH URLs.
* Resolved an issue wherein the Detect JAR was downloading for each scan when the script could not communicate with Artifactory. Now, if the script cannot communicate with
  Artifactory, and there is an existing downloaded Detect, then the previously downloaded version of Detect runs. However, if you provided a DETECT_LATEST_RELEASE_VERSION and
  Detect cannot communicate with Artifactory, Detect will not run.
* Resolved an issue wherein Detect was not properly parsing GIT URLs such as git://git.yoctoproject.org/poky.git.

## Version 5.6.2

### Resolved issues

* Synopsys Detect version 5.6.2 is a rebuild of version 5.6.0 and 5.6.1 to address an issue with the binary repository to which it was published.

## Version 5.6.0

### New features

* You can now set custom fields on created Black Duck projects.
* Detect can now generate its own air gap zip.
* Detectors now nest by default.
* Added support for Gradle Kotlin.
* Added support for wildcard (*) in the Detect flag blackduck.proxy.ignored.hosts.
* Added support for --detect.project.tags.
* Added the properties --detect.parent.project.name and --detect.parent.project.version.name.
* Added the property --detect.clone.project.version.latest=true which takes precedence over the exact version name.
* Added support for Yocto 2.0.0.
* Added support to parse components from the &lt;plugins&gt; block in pom.xml. This only works when detect.detector.buildless=true.
* Added capability to represent '' and "" as a null value in Detect multiselect custom fields.

### Changed features

* You can now specify the search depth for buildless mode.
* Updated the help menu and provided more detailed help options.
* Diagnostics now includes signature scanner log files.
* Re-enabled empty aggregate file generation.
* Polaris no longer runs the -w switch enabled by default. To retrieve the issue/policy count, you can use the -w switch.
* Match accuracy for Docker images is improved by running the signature scanner on a squashed version of the Docker image instead of the container file system. This results in a
  different name for the code location because the name of the file being scanned is different. For existing projects, the old code location named by default as &lt;repo&gt;_
  &lt;tag&gt;_containerfilesystem.tar.gz/&lt;repo&gt;/&lt;tag&gt; scan must be removed to ensure it does not contribute stale data to the BOM. Due to the new method of scanning,
  the code location name has changed. You must remove the old code location in favor of the new code location.

### Resolved issues

* Resolved an issue that could cause code location names to contain relative file paths when the value of detect.source.path uses symbolic links to specify the source directory.
* Resolved an issue that caused detect.sh to fail when Java is not on the system path, and the JAVA_HOME path contains a space.
* Resolved an issue wherein the signature scanner may not have been reporting failures correctly.
* Resolved an issue wherein Detect was not locating the file recipe-depends.dot when it was written to the current directory. Detect now searches for the recipe-depends.dot file to
  a depth of 1 when extracting on a BitBake project.
* Detect no longer fails if the Git executable is not found.
* Resolved an issue wherein Detect may fail when the directory pointed to by --detect.notices.report.path does not exist.

## Version 5.5.1

### Resolved issues

* Resolved an issue wherein the Pipenv detector was omitting project dependencies.

## Version 5.5.0

### New features

* Added support for snippet modes.
* The property detect.wait.for.results has been added to wait for Black Duck. The default value is false. If this property is set to true, Detect won't complete until the normal
  timeout is reached or the underlying systems with which Detect is communicating are once again idle and ready to receive more data. The timeout value is controlled by
  blackduck.timeout.
* The shell script and PowerShell script now accept DETECT_JAVA_PATH and DETECT_JAVA_HOME as environment variables for pointing to your Java installation.
* Added a new property --detect.detector.search.exclusion.paths. A comma-separated list of directory paths to exclude from a detector search. For example, foo/bar/biz only excludes
  the biz directory if the parent directory structure is
  'foo/bar/'.
* Detect now uses Git information to determine the default project and version names.
* There is a new Detect property for overriding the Git executable: detect.git.path.

### Resolved issues

* Resolved an issue that caused the risk report to be generated with invalid links to Black Duck components.
* Resolved an issue that caused a null pointer exception error when a golang's Gopkg.lock file contained zero projects.
* Resolved an issue wherein the Clang detector could omit the epoch from the version string in RPM packages.
* Resolved an issue wherein with two users running Detect on a single system may result in a Permission denied error.
* Resolved an issue wherein the property -detect.policy.check.fail.on.severities may not be waiting for the snippet scans to complete.
* Resolved an issue wherein the property --detect.blackduck.signature.scanner.exclusion.name.patterns may not be following the paths.
* Resolved an issue wherein Detect may fail when the directory specified by --detect.risk.report.pdf.path did not exist. Detect now attempts to create the directory structure to
  the specified path. A warning is logged if Detect fails to create the directory.
* Resolved an issue wherein properties that had a primary group and additional property group may have been excluded from the group search.
* Resolved an issue wherein the deprecation warning displayed when the deprecated property was provided by the user.
* Resolved an issue with aggregate BOM filename generation that could cause the message Unable to relativize path, full source path will be used to display in the log.
* Resolved an issue that could cause components to be omitted from the BOM for Conda projects.
* Resolved an issue that could cause errors during parsing of Maven projects with long subproject names.

### Changed features

* The default value for the property detect.docker.path.required is now false.
* The ALL logging level is replaced with the TRACE logging level.
* The results URL for the Black Duck project BOM is now moved to the Detect Results panel.
* Renamed Detect Results to Detect Status.
* Previously, a temp file remained which could contain plain text user name or password information. This temp file is now removed.
* Bazel is added as an acceptable value to the detect.tools properties.
* Detect now uses the current version of Docker Inspector. This means that no matter what version of Docker Inspector is currently released, Detect now uses that version.

## Version 5.4.0

### New features

* Added buildless mode.
* Added a new property for BitBake to remove Yocto reference implementation characters.
* Added a new property for adding group names to projects.
* Added a new property for uploading source files.
* Added the additional_components placeholder.

### Resolved issues

* Resolved an issue wherein Yarn may have been incorrectly calculating the tree level.
* Resolved an issue wherein Detect may fail when Polaris is excluded, a Polaris URL is provided, and connection to Polaris failed.
* Resolved an issue that caused Detect to follow symbolic links while searching directories for files.
* Resolved an issue wherein Detect was not failing policy for UNSPECIFIED when fail on severities is set to ALL.
* Resolved an issue that could cause a counter (an integer intended to ensure uniqueness), to be unnecessarily appended to a code location name.
* Resolved an issue that may have caused the package manager name to be excluded from the code location name when a code location name was provided.
* Resolved an issue that could cause Detect to continue after a Polaris connection failure.
* Resolved an issue wherein the Detect scan results may incorrectly show development dependencies.
* Resolved an issue that could cause reports to fail due to timeout intermittently.
* Resolved an issue that could cause the value of --polaris.access.token to be logged to the console when detect.sh is invoked.
* Resolved an issue wherein Detect was cleaning up the contents but not the directory of the run.

### Changed features

* For getting all logs, the ALL logging level is now TRACE.
* Improved the error message logged when the property detect.binary.scan.file.path, which must point to a readable file, points to something other than a readable file, such as a
  directory.
* Changed the environment variable used to tell the Detect scripts where to download the Detect jar. The previous value DETECT_JAR_PATH is now changed to DETECT_JAR_DOWNLOAD_DIR.
* Improved the parsing of packrat.lock files to better represent the relationships between dependencies in the graph.
* The version of Detect is no longer part of the code location name.

## Version 5.3.3

* Resolved an issue wherein reports for projects containing risks may be generated with a status of zero risks shown.

## Version 5.3.2

* Synopsys Detect version 5.3.2 is a minor maintenance release.

## Version 5.3.1

### New features

* Added new property detect.ignore.connection.failures which enables Synopsys Detect to continue even if it fails to talk to Black Duck.

### Resolved issues

* Resolved an issue wherein build scan failures may occur in TFS with the error `[COPY Operation] noSuchPath in source, path provided: //license/ownership`.
* Resolved an issue wherein if the property detect.clone.project.version.name is set to a non-existent project version, the log messages are now improved to make it easier to
  recognize the problem.

### Changed features

* In cases where the property detect.clone.project.version.name is set to a non-existent project version, the log messages are now improved to make it easier to recognize the
  issue.

## Version 5.2.0

### New features

* Added support for Bazel.
* Added support for CMake.
* Added a property to support using project version nicknames.
* Added a property for application ID.
* Added Java wildcard pattern support.
* Added support for Coverity on Polaris.

### Resolved issues

* Resolved an issue wherein the package-lock.json file may be missing additional versions.
* Resolved an issue wherein multiple simultaneous Detect executions may cause BDIO merges.
* Resolved an issue wherein permission errors may display when creating projects or scanning.

### Changed features

* The --detect.bom.aggregate.name property now checks for an empty BOM. If the BOM is empty, it is not uploaded to Black Duck.
* Added support for PiP versions 6.0.0 and higher.
* Improved error messages for Black Duck connection issues.
* Cosmetic changes: from Black Duck Detect to Synopsys Detect.
* Streamlined execution of Coverity and Black Duck scans through a single continuous integration job.
* Updated location of the shell/PowerShell scripts.
* Updated location of the air gap archive.

## Version 5.1.0

### New features

* Added support for GoVendor.
* Added executable output to diagnostic mode.
* Added the project/version GUID in the console output.
* Added error codes.

### Resolved issues

* Resolved an issue that fixes the Clang Detector (for C/C++) handling of complex quoted strings occurring in compiler commands found in the JSON compilation database (
  compile_commands.json) file.
* Resolved an issue wherein a Null Pointer Exception error may occur when Detect cannot access a file during signature scan exclusion calculating.
* Resolved an issue wherein the RubyGems package manager had missing components.
* Resolved an issue wherein the NPM package lock added every dependency as a root dependency.

### Changed features

* The properties --detect.nuget.path and --detect.nuget.inspector.name are deprecated.
* The properties detect.suppress.results.output and detect.suppress.configuration.output are deprecated. The output from these properties is logged instead of written to sysout.
* Improved the reporting of scan registration limit errors.

## Version 5.0.1

### Resolved issues

* Resolved an issue wherein a null pointer exception error may occur in the NuGet portion of a scan when running Synopsys Detect in Linux.
* Resolved an issue that fixes the Clang Detector (for C/C++) handling of complex quoted strings occurring in compiler commands found in the JSON compilation database (
  compile_commands.json) file.
* Resolved an issue wherein using detect.tools=ALL did not run any tools.
* Resolved an issue wherein Coverity on Polaris may return a failure status for a successful upload.

### Changed features

* NuGet air gap mode now points to other folders.
* Removed support for PiP resolving the project version.

## Version 5.0.0

### New features

* Added a new property to execute Black Duck Docker Inspector.
* CocoaPods are now nestable under Bill of Materials (BOM) tools.
* Added functionality to exclude all BOM tools.
* Added a new property which enables you to search at a determined depth.
* Added functionality to log all found executables.
* Added functionality to run in Docker mode.
* Added support for NuGet in MacOS.
* Added ability to include and exclude all tools.
* Added new properties for SWIP in Detect scans.

### Resolved issues

* Resolved an issue that caused the Gradle inspector to retrieve the maven-metadata.xml file from the default repository, even when the property
  detect.gradle.inspector.repository.url was set to point to a different repository.
* Resolved an issue wherein Gradle may upload older BDIO files into the current project.

### Changed features

* Improved C/C++ multi-threading functionality.
* Deprecated Pipenv inspector messages are now logged.
* The term BOM_TOOL is now replaced with DETECTOR.
* You can no longer supply ranges for the Inspector versions.
* Enhanced the code location naming conventions.

