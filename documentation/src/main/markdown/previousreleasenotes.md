<!-- Check the support matrix to determine supported, non-current major version releases -->
# Release notes for previous supported versions

## Version 10.4.0

### New features

* Support for Conda has been extended to 25.1.1.
* Cargo CLI Detector, leveraging `cargo tree` to extract direct and transitive dependencies, improving accuracy over the previous flat lockfile detection. This build-based detector is triggered for Cargo projects with a `Cargo.toml` file and requires Cargo version **1.44.0+**. For further information, see [Cargo package manager support](packagemgrs/cargo.md).
* Added property [detect.cargo.path](properties/detectors/cargo.md) to allow user specification of a custom Cargo executable path.   
* New `detect.pnpm.included.packages` and `detect.pnpm.excluded.packages` properties for pnpm provide increased control over what directories [detect_product_short] scans under a pnpm project. See the [pnpm](properties/detectors/pnpm.html) property page for more information.

### Changed features

* If the URL configured for SCA Scan Service (SCASS) is inaccessible when [detect_product_short] attempts a binary or container scan, [detect_product_short] will retry the scan without using SCASS.
    * See [bd_product_long] SCA Scan Service (SCASS) notice above for information pertaining to IP addresses that require allow listing.

### Resolved issues

* (IDETECT-4642) - Improved handling of pnpm packages that contain detailed version information in the pnpm-lock.yaml. Resolving [detect_product_short] missing some packages through failure to link direct and transitive dependencies. 
* (IDETECT-4641) - Improved [detect_product_short]'s Yarn detector to handle non-standard version entries for component dependencies.
* (IDETECT-4594) - Resolved [detect_product_short] failing to handle duplicate keys in `package.json` files across npm, pnpm, Lerna, and Yarn projects.

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
	See [ReversingLabs Scans](runningdetect/threatintelscan.md) for further information.
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

## Version 8.11.2

<note type="notice">`sig-repo.synopsys.com` and `detect.synopsys.com` are being deprecated. Please make use of `repo.blackduck.com` and `detect.blackduck.com` respectively.</note>
* After February 2025, [detect_product_short] script download details will only be available via detect.blackduck.com.
* See the [Domain Change FAQ for the deprecation of sig-repo](https://community.blackduck.com/s/question/0D5Uh00000Jq18XKAR/black-duck-sca-and-the-impact-of-decommissioning-of-sigrepo).
<note type="important">It is essential to update to 8.11.2 before sig-repo is decommissioned.</note>

<note type="note">It is recommended that customers continue to maintain `sig-repo.synopsys.com`, and `repo.blackduck.com` on their allow list until February 2025 when `sig-repo.synopsys.com` will be fully replaced by `repo.blackduck.com`.</note>

### Changed features

* Adds logic to pull necessary artifacts from the repo.blackduck.com repository. If this is not accessible, artifacts will be downloaded from the sig-repo.synopsys.com repository. 

## Version 8.11.1

### Resolved issues

* (IDETECT-4281) Improved forward compatibility of Project Version Update requests `--detect.project.version.update=true`, sent from [company_name] [solution_name] to [blackduck_product_name], for the Projects API by specifying the content type of the request.

## Version 8.11.0

### New features

* For Stateless and Rapid scans, the scanId and scan type being run are now stored in the codeLocations section of the status.json file. For a given scanId, the scan type can be DETECTOR, BINARY_SCAN, SIGNATURE_SCAN, or CONTAINER_SCAN.
* Stateless Signature and Package Manager scans now support the <code>--detect.blackduck.rapid.compare.mode</code> flag. Values are ALL, BOM_COMPARE, or BOM_COMPARE_STRICT. See the [Stateless Scans page](runningdetect/statelessscan.md) for further details. 
* [Component Location Analysis](runningdetect/component-location-analysis.md) is now available for offline and Rapid/Stateless online scans of NPM, Maven, Gradle and NuGet projects.

### Resolved issues

* (IDETECT-3921) [company_name] [solution_name] will now validate directory permissions prior to downloading the [company_name] [solution_name] JAR file.

### Dependency updates

* Upgraded Docker Inspector to version 10.1.0. See the [Docker Inspector Release notes](packagemgrs/docker/releasenotes.md) for further details.

## Version 8.10.0

### Changed features

* Leading and trailing spaces specified within quotes for `detect.project.name` or `detect.project.version.name` properties will now be trimmed.

### Resolved issues

* (IDETECT-3657) Resolved an issue where Intelligent Scans would fail if a project or version name included non-ASCII characters. 
* (IDETECT-3776) Resolved an issue with not detecting certain components in `go.mod` files as transitive dependencies when marked with `// indirect`, by improving identification of direct and indirect dependencies.
* (IDETECT-3817) Improved handling of large inspection results to prevent OutOfMemory exceptions and optimize memory usage.
* (IDETECT-3888) Improved the runtime performance of PIP Inspector for aws-cdk dependency cases by passing the package history list by reference instead of value.
* (IDETECT-3867) Resolved a lack of support for properties set in SPRING_APPLICATION_JSON environment variable for configuring [company_name] [solution_name] when the Self Update feature is utilized.

### Dependency updates

* Upgraded Spring Boot to version 2.7.12 to resolve high severity [CVE-2023-20883](https://nvd.nist.gov/vuln/detail/CVE-2023-20883)
* Upgraded SnakeYAML to version 2.0 for [company_name] [solution_name] air gap package to resolve critical severity [CVE-2022-1471](https://nvd.nist.gov/vuln/detail/CVE-2022-1471)
* Upgraded Jackson Databind to version 2.15.0 for [company_name] [solution_name] air gap package to resolve high severity [CVE-2022-42003](https://nvd.nist.gov/vuln/detail/CVE-2022-42003) and [CVE-2022-42004](https://nvd.nist.gov/vuln/detail/CVE-2022-42004)
* Upgraded Project Inspector to version 2021.9.9


## Version 8.9.0

### New features

* [company_name] [solution_name] Self Update feature will allow customers who choose to enable Centralized [company_name] [solution_name] Version Management in [blackduck_product_name] to automate the update of [company_name] [solution_name] across their pipelines. The Self Update feature will call the '/api/tools/detect' API to check for the existence of a mapped [company_name] [solution_name] version in [blackduck_product_name]. If a version has been mapped, the API will redirect the request to download the specified version and the current execution of [company_name] [solution_name] will invoke it to execute the requested scan. If no mapping exists, the current version of [company_name] [solution_name] matches the mapped version in [blackduck_product_name], or if there is any issue during the execution of the Self Update feature, then [company_name] [solution_name] will continue with the currently deployed version to execute the scan.
    * Centralized [company_name] [solution_name] Version Management feature support in [blackduck_product_name] is available from [blackduck_product_name] version 2023.4.0 onwards.
    * See [Version Management](downloadingandinstalling/selfupdatingdetect.md) for more details.

### Changed features

* Release notes are now broken into sections covering the current, supported, and unsupported [company_name] [solution_name] releases.
* npm 6 has reached end of life and is being deprecated. Support for npm 6 will be removed in [company_name] [solution_name] 9.

### Resolved issues

* (IDETECT-3613) Resolved an issue where running a scan with `detect.maven.build.command=-Dverbose` caused a KB mismatch issue for omitted transitive dependencies.

### Dependency updates

* Upgraded SnakeYAML to version 2.0 to resolve critical severity [CVE-2022-1471]( https://nvd.nist.gov/vuln/detail/CVE-2022-1471)
* Upgraded Jackson Dataformat YAML to version 2.15.0 to resolve critical severity [CVE-2022-1471]( https://nvd.nist.gov/vuln/detail/CVE-2022-1471)
* Upgraded Spring Boot to version 2.7.11 to resolve high severity [CVE-2023-20873](https://nvd.nist.gov/vuln/detail/CVE-2023-20873)

## Version 8.8.0

### New features

* New Binary Stateless and Container Stateless Scans have been added to [company_name] [solution_name]. These scans require the new detect.scaaas.scan.path property to be set to either a binary file or a compressed Docker image. See the [Stateless Scans page](runningdetect/statelessscan.md) for further details.
<note type="attention">A Black Duck Binary Analysis (BDBA) license is required to execute these scan types.</note>

### Changed features

* Evicted dependencies in Simple Build Tool(SBT) projects will no longer be included in the Bill of Materials(BoM) generated during the scan.
* Introduced an optional flag to allow a space-separated list of global options to pass to all invocations of Project Inspector. Specify the <code>--detect.project.inspector.global.arguments</code> flag in the command, followed by other global flags if needed for pass through to Project Inspector. <br />
See [project-inspector properties for further details](properties/configuration/project-inspector.md).
* The maximum polling interval threshold is now dynamic when [company_name] [solution_name] polls Black Duck for results. This dynamic threshold is dependent upon, and optimized for, the specific scan size. (The maximum polling threshold was formerly a fixed 60-second value.)

### Resolved issues

* (IDETECT-3111) When scanning SBT projects, "Evicted" dependencies are excluded from the resulting BOM.
* (IDETECT-3685) Gracefully handled use case when a Podfile.lock file has no PODS or dependencies in the generated dependency graph.
* (IDETECT-3738) Repositioned the global flags for inclusion before sub-commands for Project Inspector invocation.

## Version 8.7.0

### New features

* The accuracy of dependency determination, HIGH or LOW, of any detectors run during a scan will now be recorded in the status.json file.
* STATELESS/RAPID scans, when run against [blackduck_product_name] 2023.1.2 or later, will provide upgrade guidance for mitigation of vulnerabilities in transitive dependencies.

### Changed features

* Addition of command line help option, -hyaml, to generate a template configuration file.
* [company_name] [solution_name]'s generated air gap zip is uploaded to Artifactory under the name "synopsys-detect-<version>-air-gap-no-docker.zip". Older naming patterns for this file are no longer supported.
* Failures in detectors will now be reported in the console output using the ERROR logging level. The ERROR logging is also used if there are errors in the overall status.

### Resolved issues

* (IDETECT-3661) [company_name] [solution_name] will fail and echo the error received from [blackduck_product_name], if a problem occurs during the initiation of a Stateless Signature Scan.
* (IDETECT-3623) [company_name] [solution_name] will now fail with exit code 3, FAILURE_POLICY_VIOLATION, if [blackduck_product_name] reports any violated policies during scans.
* (IDETECT-3630) Notices and risk report PDFs now appropriately contain the supplied project and version name when characters from non-English alphabets are used.
* (IDETECT-3654) As of version 8.0.0 of [company_name] [solution_name], Cargo project dependency graphs stopped being post-processed. Previously, attempts to define parent relationships for dependencies when the Cargo.lock file is a flat list resulted in marking any dependencies with a parent relationship as Transitive. This meant a dependency, which if Direct, may appear as Transitive in [blackduck_product_name] if it is also a dependency of another component. BOMs created with 8.0.0 or later, no longer assume any relationships and all dependencies are DIRECT.

## Version 8.6.0

### Changed features

* Package Manager and Signature Scans will now query [blackduck_product_name] directly when using the detect.wait.for.results property. This expedites scanning by allowing [company_name] [solution_name] to determine if results are ready, rather than waiting for a notification from [blackduck_product_name].
Note: this feature requires [blackduck_product_name] 2023.1.1 or later.

### Resolved issues

* (IDETECT-3627) When waiting for results, Signature Scans will now wait for all scans that the Signature Scan could invoke, such as Snippet and String Search scans. Previously, only the Signature Scan itself was checked for completion.
Note: this improvement requires [blackduck_product_name] 2023.1.2 or later. 

### Dependency updates

* Upgraded Apache Commons Text to version 1.10.0.
* Upgraded Docker Inspector to version 10.0.1.

## Version 8.5.0

### New features

* Added property blackduck.offline.mode.force.bdio which when set to true will force [company_name] [solution_name] used in offline mode to create a BDIO even if no code locations were identified.

### Changed features

* The .yarn directory will now be ignored by default when determining which detectors are applicable to a project.
* An exit code of 2, representing FAILURE_TIMEOUT, will be returned when STATELESS scans do not report status in a timely fashion. The timeout can be controlled using the detect.timeout property.

## Version 8.4.0

### Changed features

* The flag value EPHEMERAL has been deprecated in favor of the value STATELESS.  See the [Stateless Scans page](runningdetect/statelessscan.md) for further details.

### Resolved issues

* (IDETECT-3384) Changed Warning message "No dependency found" in Lerna projects to Debug level.

## Version 8.3.0

### New features

* Added support for Reduced Persistence Signature Scanning. This feature allows users to specify if unmatched files should be persisted or discarded. Not storing data for unmatched files decreases scan time and database size. Note: this feature requires Black Duck 2022.10.0 or later.

### Resolved issues

* (IDETECT-3285) go.mod file "// indirects" matching as Direct Dependencies.  Additional information for the go project is obtained in order to definitively establish direct module dependencies and then establish which module dependencies are transitive.

* (IDETECT-3228) Resolved an issue that caused certain Maven dependency tree formats to not be parsed.

## Version 8.2.0

### New features

* Ephemeral Scan, or Ephemeral Scan Mode, is a new way of running [company_name] [solution_name] with [blackduck_product_name]. This mode is designed to be as fast as possible and does not persist any data on [blackduck_product_name]. See the [Ephemeral Scans page](runningdetect/statelessscan.md) for further details.
* The output for Rapid and the new Ephemeral Scan Modes will now include upgrade guidance for security errors and warnings.

## Version 8.1.1

### Resolved issues

* (IDETECT-3509) Corrected the version of the NuGet Inspector built into the air gap zip files (from 1.0.1 to 1.0.2).

## Version 8.1.0

### New features

* Added support for Bazel project dependencies specified via a github released artifact location (URL) in an *http_archive* workspace rule.
* Added property detect.project.inspector.path to enable pointing [company_name] [solution_name] to a local Project Inspector zip file.
* Added property detect.status.json.output.path to place a copy of the status.json file in a specified directory.

### Changed features

* Enhancements to error reporting to ensure that any exception will have the root cause reported in the error message for certain exception types.
* Overall Detect exit status is now being reported along with individual detector status/issues in the Status.json file.
* The __MACOSX directory will now be ignored by default when determining which detectors are applicable to a project.

### Resolved issues

* (IDETECT-3419) Resolved an issue where the NuGet inspector cannot be found when a solution file cannot be found but multiple C# projects are found by Detect.
* (IDETECT-3306) Resolved an issue where a NullPointerException would occur when project inspector discovered no modules for a project.
* (IDETECT-3307) Warn when project inspector cannot be downloaded, installed, or found.
* (IDETECT-3187) Report Black Duck provided error message (from response body) whenever a Black Duck api call returns an error code
* (IDETECT-3311) Include Detect's "Overall Status" in the status.json / diagnostic zip
* (IDETECT-3449) Resolved an issue that caused overridden violations to be reported as active violations when the BOM contained additional active violations.
* (IDETECT-3476) Resolved an issue that caused an "Input request parsing error" on IaC scans on certain projects when running on Windows.

## Version 8.0.0

### New features

* [company_name] [solution_name] will now retry (until timeout; see property `detect.timeout`) BDIO2 uploads that fail with a non-fatal exit code.
* Added Detector cascade. Refer to [Detector search and accuracy](runningdetect/detectorcascade.md) for more information.

### Changed features

* The default value of `detect.project.clone.categories` now includes DEEP_LICENSE (added to Black Duck in 2022.2.0), raising the minimum version of Black Duck for [company_name] [solution_name] 8.0.0 to 2022.2.0.
* The [codelocation naming scheme](naming/projectversionscannaming.md#code-location-scan-naming) has changed. To prevent old codelocations from contributing stale results to re-scanned projects, set property `detect.project.codelocation.unmap` to true for the first run of [company_name] [solution_name] 8. This will unmap the old codelocations.
* The default value of `detect.force.success.on.skip` has changed to false, so by default [company_name] [solution_name] will exit with return code FAILURE_MINIMUM_INTERVAL_NOT_MET (13) when a scan is skipped because the Black Duck minimum scan interval has not been met.
* By default, all detectors now include in their dependency graph all discovered
dependencies, packages, and configurations, because the default for
properties `detect.*.[dependency|package|configuration].types.excluded` is NONE. 
This is a change in the default behavior for the following detector types: 
GO_MOD, GRADLE, LERNA, RUBYGEMS.
* Dropped NONE as a supported value for the following properties: `detect.included.detector.types`, `detect.tools`.
* Dropped ALL as a supported value for the following properties: `detect.excluded.detector.types`, `detect.tools.excluded`.
* Removed support for parsing SBT report files.
* Cargo project dependency graphs are no longer post-processed to reduce direct dependencies in the BOM.
* Removed the ability to upload BDIO2 documents to legacy endpoints via the `blackduck.legacy.upload.enabled` property.
* Removed the ability to choose the type of BDIO aggregation strategy via the now removed `detect.bom.aggregate.remediation.mode` property.  All BDIO will be aggregated in a manner similar to [company_name] [solution_name] 7's SUBPROJECT remediation mode.
* [company_name] [solution_name] now only produces a single Scan in Black Duck for Detectors, named (by default) "\<projectName\>/\<projectVersion\> Black Duck I/O Export". 
* detect8.sh has improvements (relative to detect7.sh and detect.sh) related to argument handling that simplify its argument quoting/escaping requirements.
* [company_name] [solution_name] requires and runs [docker_inspector_name] version 10.
* Incorporated [docker_inspector_name] documentation into [company_name] [solution_name] documentation.
* The search for files for binary scanning (when property `detect.binary.scan.file.name.patterns` is set) now excludes directories specified by property `detect.excluded.directories`.
* The status.json field `detectors[n].descriptiveName` (which was simply a hyphen-separated concatenation of the `detectorType` and `detectorName` fields) has been removed.
* There is no longer a distinction between extended and non-extended diagnostic zip files. All diagnostic zip files now include all relevant files.
* The following properties (that were deprecated in [company_name] [solution_name] 7.x) have been removed: `blackduck.legacy.upload.enabled`, `detect.bazel.dependency.type`,
`detect.bdio2.enabled`, `detect.bom.aggregate.name`, `detect.bom.aggregate.remediation.mode`, `detect.conan.include.build.dependencies`, `detect.detector.buildless`,
`detect.docker.path.required`, `detect.dotnet.path`, `detect.go.mod.enable.verification`, `detect.gradle.include.unresolved.configurations`, `detect.gradle.inspector.version`,
`detect.lerna.include.private`, `detect.maven.buildless.legacy.mode`, `detect.maven.include.plugins`, `detect.npm.include.dev.dependencies`, `detect.npm.include.peer.dependencies`,
`detect.nuget.inspector.version`, `detect.packagist.include.dev.dependencies`, `detect.pear.only.required.deps`, `detect.pnpm.dependency.types`,
`detect.pub.deps.exclude.dev`, `detect.ruby.include.dev.dependencies`, `detect.ruby.include.runtime.dependencies`, `detect.sbt.excluded.configurations`,
`detect.sbt.included.configurations`, `detect.sbt.report.search.depth`, `detect.yarn.prod.only`.

### Resolved issues

* (IDETECT-3375) Resolved an issue where [company_name] [solution_name] would unnecessarily upload empty BDIO entry file when initiating an IaC scan.
* (IDETECT-3224) Resolved an issue where Cargo projects with Cyclical dependencies could cause a failure of [company_name] [solution_name].
* (IDETECT-3246) Resolved an issue where [company_name] [solution_name] would fail when scanning flutter projects after a new version of flutter was released.
* (IDETECT-3275) Resolved an issue that caused impact analysis to fail with an "Unsupported class file major version" error when an analyzed .class file contained invalid version bytes (byte 7 and 8).
* (IDETECT-3180) Resolved an issue that caused the Binary Search tool to throw an exception when the patterns provided via property detect.binary.scan.file.name.patterns matched one or more directories.
* (IDETECT-3352) Resolved an issue that caused the Gradle Project Inspector detector to fail when the value of detect.output.path was a relative path.
* (IDETECT-3371) Resolved an issue that could cause some transitive dependencies to be omitted from aggregated BDIO in cases where the transitive dependencies provided by the package manager for a component differed across subprojects.
