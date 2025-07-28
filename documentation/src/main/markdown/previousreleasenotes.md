<!-- Check the support matrix to determine supported, non-current major version releases -->
# Release notes for previous [detect_product_short] versions

## Version 10.5.0

### New features

* Support for UV Package Manager has been added under [UV Detector](packagemgrs/python.md#uv-package-manager)
* With the `detect.clone.project.version.name` parameter specified and `detect.project.version.update` set to true, [detect_product_short] will now clone, scan, and update the cloned project via parameters such as `detect.project.version.phase`.
* Support for Java 21 has been added.
* If feasible, the most probable keys are recommended in place of invalid property keys that contain misspellings or malformations.

### Changed features

* Gradle inspector script no longer requires, or includes, Gradle dependencies. This applies to both non-air gap and air gap zip generation.
* [detect_product_short] will now fail the scan if `detect.wait.for.results` is set to true and a scan is not properly included in the BOM.

### Resolved issues

* (IDETECT-4177) - [detect_product_short] no longer requires that the X-Artifactory-Filename header is set when specifying an internally hosted version in [bd_product_long].
* (IDETECT-3512) - To prevent issues when [bd_product_long] and [detect_product_short] disagree on the full list of categories, [detect_product_short] now sends an indicator specifying "all categories" when detect.project.clone.categories is set to ALL.
* (IDETECT-4606) - Support for the exclusion of dependency types in [detect_product_short] Nuget Inspector now includes `project.assets.json` and `project.lock.json` files.
* (IDETECT-4209) - [detect_product_short] no longer creates numerous access denied exceptions in [bd_product_long] logs when a user does not have system administrator access.
* (IDETECT-4222) [detect_product_short] now reports a failure status (FAILURE_BOM_PREPARATION) when BOM preparation fails in [bd_product_long].

### Dependency updates

* Upgraded and released Nuget Inspector version 2.2.0.
* Upgraded and released Docker Inspector version 11.3.0.
* Updated usage of Apache Commons BeanUtils to version 1.11.0.

## Version 10.4.0

### New features

* Support for Conda has been extended to 25.1.1.
* Cargo CLI Detector, leveraging `cargo tree` to extract direct and transitive dependencies, improving accuracy over the previous flat lockfile detection. This build-based detector is triggered for Cargo projects with a `Cargo.toml` file and requires Cargo version **1.44.0+**. For further information, see [Cargo package manager support](packagemgrs/cargo.md).
* Added property [detect.cargo.path](properties/detectors/cargo.md) to allow user specification of a custom Cargo executable path.   
* New `detect.pnpm.included.packages` and `detect.pnpm.excluded.packages` properties for pnpm provide increased control over what directories [detect_product_short] scans under a pnpm project. See the [pnpm](properties/detectors/pnpm.html) property page for more information.

### Changed features

* If the URL configured for SCA Scan Service (SCASS) is inaccessible when [detect_product_short] attempts a binary or container scan, [detect_product_short] will retry the scan without using SCASS.
	* See [bd_product_long] SCA Scan Service (SCASS) notice above for information pertaining to IP addresses that require allow listing.
* ReversingLabs Scans (`detect.tools=THREAT_INTEL`) has been deprecated.
* The `detect.threatintel.scan.file.path` property has been deprecated. 
* [detect_product_short] will now return a unique error code `code 16 - FAILURE_OUT_OF_MEMORY` when sub processes experience "Out of memory" issues.
* PIP Native Inspector now supports Python 3.12+.

### Resolved issues

* (IDETECT-4642) - Improved handling of pnpm packages that contain detailed version information in the pnpm-lock.yaml. Resolving [detect_product_short] missing some packages through failure to link direct and transitive dependencies. 
* (IDETECT-4641) - Improved [detect_product_short]'s Yarn detector to handle non-standard version entries for component dependencies.
* (IDETECT-4602 & IDETECT-4180) - Resolved Go dependency scan issue that resulted in transitive dependencies assigned to incorrect parent. (For further details on how the Go Mod CLI detector determines parents, please see [GoLang support](packagemgrs/golang.md).)
* (IDETECT-4594) - Resolved [detect_product_short] failing to handle duplicate keys in `package.json` files across npm, pnpm, Lerna, and Yarn projects.
* (IDETECT-4467) - Resolved an issue where [detect_product_short] would exit with a 0 (zero) success code despite dependency requirements not being met for PIP Native Inspector.
	* The PIP Native Inspector will yield to other detectors when it cannot resolve an expected dependency from the PIP cache.

### Dependency updates
* Upgraded and released Detect Docker Inspector version 11.2.0.

## Version 10.3.0

### New features

* Added support for `ArtifactsPath` and `BaseIntermediateOutputPath` properties in [detect_product_long] NuGet Inspector. See [detect.nuget.artifacts.path](packagemgrs/nuget.md#nuget-artifacts-and-base-intermediate-output-paths) for more details.
* SCA Scan Service (SCASS) is a scalable solution for performing software composition analysis scans outside of the traditional [bd_product_long] environment. This [detect_product_short] release provides support for the SCA Scan Service (SCASS) for [bd_product_short] version 2025.1.1 or later. For further information see [About SCASS](https://documentation.blackduck.com/bundle/bd-hub/page/ComponentDiscovery/aboutScaScanService.html).
	* See IP address notice above for details on related IP configuration for your deployments.

### Resolved issues

* (IDETECT-4610) - Improved [detect_product_short]'s air gap for Gradle creation script to prevent unwanted JAR files from being included in the gradle subdirectory.
* (IDETECT-4611) - Updated [detect_product_short]'s air gap for Gradle creation script to remove reference to Integration Common library that is no longer a dependency.
* (IDETECT-3932) - Improved the exit code and error output generated when a duplicate project name is used in simultaneous scans.
* (IDETECT-4327) - Updated the Conan 2 detector to provide log entries in case of error.

### Dependency updates

* Upgraded and released NuGet Inspector version 2.1.0.
* Upgraded to rebranded Method Analyzer Core Library version 1.0.1 for Vulnerability Impact Analysis.

## Version 10.2.1

### Resolved issues

* (IDETECT-4560) - Update the FreeMarker Template Language (FTL) script used to build the [detect_product_short] air gap zips to prevent inclusion of outdated JARs.

## Version 10.2.0

### New features

* The scanCLI `detect.blackduck.signature.scanner.csv.archive` property has been added for generating and uploading CSV files to [bd_product_long] 2025.1.0 or later. If used in offline mode, the generated CSV files will be located in the [detect_product_short] run directory in the csv folder.
<note type="note">This feature is only available for intelligent persistence scans.</note>

### Changed features

* Use of the --detect.yarn.ignore.all.workspaces flag is not required for Yarn 4 projects, thus configuration parameters such as detect.yarn.dependency.types.excluded=NON_PRODUCTION can be employed.

### Resolved issues

* (IDETECT-4447) - ID strings of detected Yarn project dependencies are now correctly formed. Related warning messages have been improved to identify entries in the yarn.lock file that have not been resolved through package.json files and could not be resolved with any standard NPM packages.
* (IDETECT-4533) - Resolved an issue with [detect_product_short] Gradle Native Inspector causing scans to hang indefinitely when submodule has the same name as the parent module.
* (IDETECT-4560) - Updated version of Jackson-Core (a transitive dependency) to address a vulnerability.

## Version 10.1.0

### New features

* npm lockfile and shrinkwrap detectors now ignore packages flagged as extraneous in the package-lock.json and npm-shrinkwrap.json files.
* Support added for Opam Package Manager via [Opam Detector](packagemgrs/opam.md).
* New Gradle Native Inspector option to only process the root dependencies of a Gradle project. See [detect.gradle.root.only](properties/detectors/gradle.md#gradle-root-only-enabled-advanced) for more details.

### Changed features

* npm version 1 package-lock.json and npm-shrinkwrap.json file parsing has been restored.
* The `detect.project.codelocation.unmap` property has been deprecated.
* Changed [detect_product_long]'s JAR signing authority from Synopsys, Inc. to Black Duck Software, Inc.

### Resolved issues

* (IDETECT-4517) - [detect_product_short] now correctly indicates a timeout failure occurred when multipart binary or container scans timeout during an upload.
* (IDETECT-4540) - Multipart binary and container scans now correctly retry when authentication errors are received during transmission.
* (IDETECT-4469) - Eliminating null (`\u0000`) and replacement (`\uFFFD`) characters during the processing of Python requirements.txt files to ensure successful extraction of dependency information.

### Dependency updates

* Upgraded and released [docker_inspector_name] version 11.1.0.
* Upgraded to [project_inspector_name] v2024.12.1.

## Version 10.0.0

[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* As part of this activity, sig-repo.synopsys.com and detect.synopsys.com are being deprecated. Please make use of repo.blackduck.com and detect.blackduck.com respectively. 
    * After March 2025, [detect_product_short] script download details will only be available via detect.blackduck.com.
    * [detect_product_short] 10.0.0 will only work when using repo.blackduck.com.

<note type="note">It is recommended that customers continue to maintain sig-repo.synopsys.com, and repo.blackduck.com on their allow list until March 31st, 2025 when sig-repo.synopsys.com will be fully replaced by repo.blackduck.com.</note>

### New features

* The npm package.json detector now performs additional parsing when attempting to find dependency versions. This can result in additional matches since versions like `^1.2.0` will now be extracted as `1.2.0` instead of as the raw `^1.2.0` string. In the case where multiple versions for a dependency are discovered, the earliest version will be used.
* Support for Python has now been extended with Pip 24.2, Pipenv 2024.0.1, and Setuptools 74.0.0.
* Support for npm has been extended to 10.8.2 and Node.js 22.7.0.
* Support for Maven has been extended to 3.9.9.
* Support for pnpm has been extended to 9.0.
* Support for BitBake is now extended to 2.8.0 (Yocto 5.0.3)
* Support for Nuget has been extended to 6.11.
* Support for GoLang is now extended to Go 1.22.7.
* Correlated Scanning is a new Match as a Service (MaaS) feature which correlates match results from Package Manager (Detector), and Signature scans when running [detect_product_short] with [bd_product_long] 2024.10.0 or later.
    * Correlation between scanning methods increases accuracy and provides for more comprehensive scan results.
    See the [detect.blackduck.correlated.scanning.enabled](properties/configuration/general.html#correlated-scanning-enabled) property for more information
    <note type="note">Correlated Scanning support is available for persistent Package Manager and Signature Scanning only.</note>
* [detect_product_short] now supports container scanning of large files via a chunking method employed during upload.
    <note type="note">This feature requires [bd_product_long] 2024.10.0 or later.</note>

### Changed features

* The `logging.level.com.synopsys.integration` property deprecated in [detect_product_short] 9.x, has been removed. Use `logging.level.detect` instead.
* The FULL_SNIPPET_MATCHING and FULL_SNIPPET_MATCHING_ONLY options for the `detect.blackduck.signature.scanner.snippet.matching` property deprecated in [detect_product_short] 9.x, have been removed.
* The `.blackduck` temporary folder has been added to the default exclusion list.

### Dependency updates

* Updated jackson-core library to version 2.15.0 to resolve a security vulnerability.
* Upgraded and released Nuget Inspector version 2.0.0.
* Upgraded and released [detect_product_short] Docker Inspector version 11.0.1

## Version 9.10.1

<note type="notice">`sig-repo.synopsys.com` and `detect.synopsys.com` are being deprecated. Please make use of `repo.blackduck.com` and `detect.blackduck.com` respectively.</note>
* After February 2025, [detect_product_short] script download details will only be available via detect.blackduck.com.
* See the [Domain Change FAQ for the deprecation of sig-repo](https://community.blackduck.com/s/question/0D5Uh00000Jq18XKAR/black-duck-sca-and-the-impact-of-decommissioning-of-sigrepo).
<note type="important">It is essential to update to 9.10.1 before sig-repo is decommissioned.</note>

<note type="note">It is recommended that customers continue to maintain `sig-repo.synopsys.com`, and `repo.blackduck.com` on their allow list until February 2025 when `sig-repo.synopsys.com` will be fully replaced by `repo.blackduck.com`.</note>

### Changed features

* Adds logic to pull necessary artifacts from the repo.blackduck.com repository. If this is not accessible, artifacts will be downloaded from the sig-repo.synopsys.com repository. 

## Version 9.10.0

### Changed features

* The `logging.level.com.synopsys.integration` property has been deprecated in favor of `logging.level.detect` and will be removed in 10.0.0. 
    <note type="note">There is no functional difference between the two properties.</note>

* Switched from Universal Analytics to Google Analytics 4 (GA4) as our phone home analytics measurement solution. 

* In 9.9.0 the ability to perform multipart uploads for binary scans was added where related properties were not configurable at runtime. As of this release an optional environment variable setting the upload chunk size has been made available. This variable is primarily intended for troubleshooting purposes. See [Environment variables](scripts/overview.md).

### Dependency updates

* Detect Docker Inspector version updated to 10.2.1

## Version 9.9.0

### New features

* [solution_name] now supports binary scanning of large files via a chunking method employed during upload. Testing has confirmed successful upload of 20GB files.
    <note type="note">This feature requires [blackduck_product_name] 2024.7.0 or later.</note>

### Changed features

* When running [company_name] [solution_name] against a [blackduck_product_name] instance of version 2024.7.0 or later, the Scan CLI tool download will use a new format for the URL. 
    * Current URL format: https://<BlackDuck_Instance>/download/scan.cli-macosx.zip
    * New URL format: https://<BlackDuck_Instance>/api/tools/scan.cli.zip/versions/latest/platforms/macosx

### Resolved issues

* (IDETECT-4408) - Remediated vulnerability in Logback-Core library to resolve high severity issues [CVE-2023-6378](https://nvd.nist.gov/vuln/detail/CVE-2023-6378) and [CVE-2023-6481](https://nvd.nist.gov/vuln/detail/CVE-2023-6481).

### Dependency updates

* Component Location Analysis version updated to 1.1.13
* Project Inspector version updated to 2024.9.0
* Logback Core version updated to 1.2.13

## Version 9.8.0

### New features
* Autonomous Scanning - this new feature simplifies default analysis of source and binary files by allowing [company_name] [solution_name] to handle, and easily repeat, basic analysis decisions.
  See [Autonomous Scanning](runningdetect/autonomousscan.dita) for further information.

### Resolved issues
* (IDETECT-4315) A filter was added to prevent performance issues related to the [company_name] [solution_name] API call that retrieves role information on startup.
* (IDETECT-4360) Resolved an issue with component location analysis failing with an index out of bounds exception when attempting to extract certain code substrings.

## Version 9.7.0

### New features

* Support for GoLang is now extended to Go 1.22.2.
* [company_name] [solution_name] now allows exclusion of development dependencies when using the Poetry detector. See the [detect.poetry.dependency.groups.excluded](properties/detectors/poetry.md#detect.poetry.dependency.groups.excluded) property for more information.
* Support has been added for Python package detection via [Setuptools](https://setuptools.pypa.io/en/latest/index.html), versions 47.0.0 through 69.4.2. See the [Python Package Managers](packagemgrs/python.md) page for further details.
* Added Docker 25 and 26 support to [Docker Inspector](packagemgrs/docker/releasenotes.md).

### Resolved issues

* (IDETECT-4341) The Poetry detector will now recognize Python components with case insensitivity.
* (IDETECT-3181) Improved Eclipse component matching implementation through better handling of external identifiers.
* (IDETECT-3989) Complete set of policy violations, regardless of category, now printed to console output.
* (IDETECT-4353) Resolved issue of including "go" as an unmatched component for Go Mod CLI Detector.

## Version 9.6.0

### New features

* ReversingLabs Scans - this new feature provides analysis of software packages for file-based malware threats.
* Component Location Analysis upgraded to certify support for location of components in Yarn Lock and Nuget Centralized Package Management files.
* Added support for Gradles rich model for declaring versions, allowing the combination of different levels of version information. See [rich version declarations](packagemgrs/gradle.md#rich-version-declaration-support).

### Resolved issues

* (IDETECT-4211) Resolved an error handling issue with the scan retry mechanism when the git SCM data is conflicting with another already scanned project.
* (IDETECT-4263) Remediated the possibility of [solution_name] sending Git credentials to [blackduck_product_name] Projects API in cases when the credentials are present in the Git URLs.

## Version 9.5.0

### New features

* [company_name] [solution_name] now includes the Maven embedded or shaded dependencies as part of the Bill of Materials (BOM) via the property --detect.maven.include.shaded.dependencies. See the [detect.maven.include.shaded.dependencies](properties/detectors/maven.md#maven-include-shaded-dependencies) property for more information.
* [company_name] [solution_name] Maven Project Inspector now supports the exclusion of Maven dependencies having "\<exclude\>" tags in the pom file.
* [company_name] [solution_name] Maven Project Inspector and Gradle Project Inspector honours effects of dependency scopes during dependency resolution.

### Dependency updates

* Upgraded Project Inspector to version 2024.2.0. Please refer to [Maven](packagemgrs/maven.md), [Gradle](packagemgrs/gradle.md) and [Nuget](packagemgrs/nuget.md) documentation for more information on the changes.
  As of version 9.5.0 [company_name] [solution_name] will only be compatible with, and support, Project Inspector 2024.2.0 or later.

## Version 9.4.0

### New features

* Nuget Inspector now supports the exclusion of user-specified dependency types from the Bill of Materials (BOM) via the [solution_name] property --detect.nuget.dependency.types.excluded. See the [detect.nuget.dependency.types.excluded](properties/detectors/nuget.md#nuget-dependency-types-excluded) property for more information.
* A new detector for Python packages has been added. The PIP Requirements File Parse is a buildless detector that acts as a LOW accuracy fallback for the PIP Native Inspector. This detector is triggered for PIP projects that contain one or more requirements.txt files if [solution_name] does not have access to a PIP executable in the environment where the scan is run.
	* See [PIP Requirements File Parse](packagemgrs/python.md).
* To improve Yarn detector performance a new parameter is now available. The `--detect.yarn.ignore.all.workspaces` parameter enables the Yarn detector to build the dependency graph without analysis of workspaces. The default setting for this parameter is false and must be set to true to be enabled. This property ignores other Yarn detector properties if set.
	* See [Yarn support](packagemgrs/yarn.md).
* Support for BitBake is now extended to 2.6 (Yocto 4.3.2).
* Support for Yarn extended to include Yarn 3 and Yarn 4.

### Changed features

* Key-value pairs specified as part of the `detect.blackduck.signature.scanner.arguments` property will now replace the values specified elsewhere, rather than act as additions.

### Resolved issues

* (IDETECT-4155) Improved input validation in Component Location Analysis.
* (IDETECT-4187) Removed references to 'murex' from test resources.
* (IDETECT-4207) Fixed Nuget Inspector IndexOutofRangeException for cases of multiple `Directory.Packages.props` files.
* (IDETECT-3909) Resolved an issue causing ASM8 Error when running Vulnerability Impact Analysis.

### Dependency updates

* Released and Upgraded Nuget Inspector to version 1.3.0.
* Released and Upgraded Detect Docker Inspector to version 10.1.1.

## Version 9.3.0

### Changed features

* Any arguments that specify the number of threads to be used provided as part of the `detect.maven.build.command` [company_name] [solution_name] property will be omitted when executing the Maven CLI.

### Resolved issues

* (IDETECT-4164) Improved Component Location Analysis parser support for package managers like Poetry that employ variable delimiters, for better location accuracy.
* (IDETECT-4171) Improved Component Location Analysis data validation support for package managers like NPM.
* (IDETECT-4174) Resolved an issue where [company_name] [solution_name] was not sending the container scan size to [blackduck_product_name] server, resulting in  [blackduck_product_name]'s "Scans" page reporting the size as zero.
* (IDETECT-4176) The FULL_SNIPPET_MATCHING and FULL_SNIPPET_MATCHING_ONLY options, currently controlled via registration key, for the --detect.blackduck.signature.scanner.snippet.matching property are deprecated and will be removed in the next major release of [company_name] [solution_name].

### Dependency updates

* Updated Guava library from 31.1 to 32.1.2 to resolve high severity [CVE-2023-2976](https://nvd.nist.gov/vuln/detail/CVE-2023-2976).

## Version 9.2.0

### New features

* Support for pnpm is now extended to 8.9.2.
* Nuget support extended to version 6.2 with Central Package Management now supported for projects and solutions.
* Support for Conan is now extended to 2.0.14.
* Support for Go and Python added to Component Location Analysis.

### Changed features

* pnpm 6, and pnpm 7 using the default v5 pnpm-lock.yaml file, are being deprecated. Support will be removed in [company_name] [solution_name] 10.

### Resolved issues

* (IDETECT-3515) Resolved an issue where the Nuget Inspector was not supporting "\<Version\>" tags for "\<PackageReference\>" on the second line and was not cascading to Project Inspector in case of failure.

### Dependency updates

* Released and Upgraded Nuget Inspector to version 1.2.0.

## Version 9.1.0

### New features

* Container Scan. Providing component risk detail analysis for each layer of a container image, (including non-Linux, non-Docker images). Please see [Container Scan ](runningdetect/containerscanning.md) for details.
	<note type="restriction">Your [blackduck_product_name] server must have [blackduck_product_name] Secure Container (BDSC) licensed and enabled.</note>
* Support for Dart is now extended to Dart 3.1.2 and Flutter 3.13.4.
* Documentation for [CPAN Package Manager](packagemgrs/cpan.md) and [BitBucket Integration](integrations/bitbucket/bitbucketintegration.md) has been added.

### Changed features

* When [blackduck_product_name] version 2023.10.0 or later is busy and includes a retry-after value greater than 0 in the header, [company_name] [solution_name] will now wait the number of seconds specified by [blackduck_product_name] before attempting to retry scan creation. 
	* [company_name] [solution_name] 9.1.0 will not retry scan creation with versions of [blackduck_product_name] prior to 2023.10.0

### Resolved issues

* (IDETECT-3843) Additional information is now provided when [company_name] [solution_name] fails to update and [company_name] [solution_name] is internally hosted.
* (IDETECT-4056) Resolved an issue where no components were reported by CPAN detector.
  If the cpan command has not been previously configured and run on the system, [company_name] [solution_name] instructs CPAN to accept default configurations.
* (IDETECT-4005) Resolved an issue where the location is not identified for a Maven component version when defined as a property.
* (IDETECT-4066) Resolved an issue of incorrect TAB width calculation in Component Locator.

### Dependency updates

* Upgraded [company_name] [solution_name] Alpine Docker images (standard and buildless) to 3.18 to pull the latest curl version with no known vulnerabilities.
* Removed curl as a dependency from [company_name] [solution_name] Ubuntu Docker image by using wget instead of curl.

## Version 9.0.0

### New features

* Support for npm is now extended to npm 9.8.1.
* Support for npm workspaces.
* Lerna projects leveraging npm now support npm up to version 9.8.1.
* Support for Gradle is now extended to Gradle 8.2.
* Support for GoLang is now extended to Go 1.20.4.
* Support for Nuget package reference properties from Directory.Build.props and Project.csproj.nuget.g.props files.

### Changed features

* The `detect.diagnostic.extended` property and the -de command line option, that were deprecated in [company_name] [solution_name] 8.x, have been removed. Use `detect.diagnostic`, and the command line option -d, instead.
* The Ephemeral Scan Mode, that was deprecated in [company_name] [solution_name] 8.x, has been removed in favor of Stateless Scan Mode. See the [Stateless Scans page](runningdetect/statelessscan.md) for further details.
* npm 6, which was deprecated in [company_name] [solution_name] 8.x, is no longer supported.
* The detectors\[N\].statusReason field of the status.json file will now contain the exit code of the detector subprocess command in cases when the code is non-zero.
  In the case of subprocess exit code 137, the detectors\[N\].statusCode and detectors\[N\].statusReason fields will be populated with a new status indicating a likely out-of-memory issue.
* In addition to node_modules, bin, build, .git, .gradle, out, packages, target, the Gradle wrapper directory `gradle` will be excluded from signature scan by default. Use
  [detect.excluded.directories.defaults.disabled](properties/configuration/paths.md#detect-excluded-directories-defaults-disabled-advanced) to disable these defaults.
* Removed reliance on [company_name] [solution_name] libraries for init-detect.gradle script to prevent them from being included in the Gradle dependency verification of target projects.   
<note type="notice">[company_name] [solution_name] 7.x has entered end of support. See the [Product Maintenance, Support, and Service Schedule page](https://sig-product-docs.synopsys.com/bundle/blackduck-compatibility/page/topics/Support-and-Service-Schedule.html) for further details.</note>

### Resolved issues

* (IDETECT-3821) Detect will now capture and record failures of the Signature Scanner due to command lengths exceeding Windows limits. This can happen with certain folder structures when using the `detect.excluded.directories` property.
* (IDETECT-3820) Introduced an enhanced approach to NuGet Inspector for handling different formats of the `project.json` file, ensuring compatibility with both old and new structures.
* (IDETECT-4027) Resolved a problem with the npm CLI detector for npm versions 7 and later, which was causing only direct dependencies to be reported.
* (IDETECT-3997) Resolved npm package JSON parse detector issue of classifying components as RubyGems instead of npmjs.
* (IDETECT-4023) Resolved the issue of Scan failure if Project level "Retain Unmatched File Data" not set for "System Default".

### Dependency updates

* Released and Upgraded Project Inspector to version 2021.9.10.
* Released and Upgraded Nuget Inspector to version 1.1.0.
* Fixed EsotericSoftware YAMLBeans library version to resolve critical severity [CVE-2023-24621](https://nvd.nist.gov/vuln/detail/CVE-2023-24621)
