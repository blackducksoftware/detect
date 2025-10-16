# Current [detect_product_short] release notes

**Notices**   

[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* As part of this activity, sig-repo.synopsys.com and detect.synopsys.com are being deprecated. Please make use of repo.blackduck.com and detect.blackduck.com respectively. 
    * [detect_product_short] script downloads should only be accessed via detect.blackduck.com.
    * [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.
    * If you are using [detect_product_short] 8 or 9 it is essential to update to 8.11.2 or 9.10.1 respectively, before sig-repo is decommissioned.   

<note type="note">It is recommended that customers continue to maintain sig-repo.synopsys.com, and repo.blackduck.com on their allow list until such time as all scripts, services, or pipelines have been updated with the repo.blackduck.com URL.</note>

* [bd_product_long] [SCA Scan Service (SCASS)](https://community.blackduck.com/s/question/0D5Uh00000O2ZSYKA3/black-duck-sca-new-ip-address-requirements-for-2025) requires customers add or update IP addresses configured in their network firewalls or allow lists. This action is required to successfully route scan data to the service for processing.

	* scass.blackduck.com - 35.244.200.22
	* na.scass.blackduck.com - 35.244.200.22
	* na.store.scass.blackduck.com - 34.54.95.139
	* eu.store.scass.blackduck.com - 34.54.213.11
	* eu.scass.blackduck.com - 34.54.38.252

## Version 11.0.0

### New features

* When enabled, the new [detect.project.deep.license](properties/configuration/project.md#deep-license-analysis) property sets the Deep License Data and Deep License Data Snippet fields when creating a project. This property can also be used to update existing projects when the [detect.project.version.update](properties/configuration/project.md#update-project-version) property is set to true.
* The new [detect.project.settings](properties/configuration/project.md#project-settings-via-json) property takes as input a path to a JSON file. This file allows users to pass several existing `detect.project` properties as a single argument to Detect. Detect will parse the JSON file to obtain information relevant to creating or updating projects.
* The new [detect.excluded.detectors](properties/configuration/detector.md#detectors-excluded-advanced) property takes as input a comma-separated list of Detector names to exclude. This allows for greater control over selection of Detectors.
* Added support for capturing dependencies from the `go.mod` file via a new buildless detector named "Go Mod File" for Go projects.
	* Added a new property [detect.go.forge](properties/detectors/go.md#go-forge-url) to customize the Go registry URL used for fetching dependency information. Defaults to `https://proxy.golang.org`.
	* Added a new property [detect.go.forge.connection.timeout](properties/detectors/go.md#go-forge-connection-timeout) to customize the connection timeout limit while connecting to the Go registry. Defaults to 30 seconds.
	* Added a new property [detect.go.forge.read.timeout](properties/detectors/go.md#go-forge-read-timeout) to customize the read timeout limit while fetching go.mod file of a dependency from Go registry. Defaults to 60 seconds.

### Changed features

* ReversingLabs Scans (`detect.tools=THREAT_INTEL`) has been removed.
* The `detect.threatintel.scan.file.path` property has been removed.
* The `detect.project.codelocation.unmap` property has been removed.
* The archived phase (`detect.project.version.phase=ARCHIVED`) has been deprecated.
* The efficiency of the Detector directory evaluation has been enhanced, resulting in the acceleration of certain scans.
* Detector directory evaluation has been made more efficient, resulting in some scans being faster.
* Support for `pyproject.toml` file has been added to PIP Native Inspector. For more details, please see [Python Detector page](packagemgrs/python.md)
* Support for the following package managers have been extended:
  * pip: 25.2
  * pipenv: 2025.0.4
  * Setuptools: 80.9.0
  * uv: 0.8.15
  * Maven: 3.9.11
  * Conan: 2.20.1
  * NuGet: 6.8.1
  * GoLang: 1.25
  * RubyGems: 3.7.1
  * Gradle: 9.0.0
  * Yarn: 4.9.4
  * NPM: 11

### Resolved issues

* (IDETECT-4738) Corrected behavior of `detect.binary.scan.file.name.patterns` to be case-insensitive. 
* (IDETECT-4802) Fix UV Lockfile Detector not generating BDIOs for projects with non-normalized names per Python requirements.
* (IDETECT-4806) Fixed UV detectors to handle dynamic versions and cyclic dependencies.
* (IDETECT-4751) Prevent server-side parsing errors by normalizing IAC Scan `results.json` contents before uploading to Black Duck SCA.
* (IDETECT-4799) When constructing the BDIO, ignore the Go toolchain directive, as it is the Go project's build-time configuration setting and not a module dependency.
* (IDETECT-4813) Fix Gradle Native Inspector to correctly identify projects with only `settings.gradle` or `settings.gradle.kts` file in the root directory.
* (IDETECT-4812) Gradle Native Inspector now supports configuration cache (refactored `init-detect.gradle` to add support for configuration cache in Gradle projects).
* (IDETECT-4845) With added support for extracting Python package versions from direct references [PEP 508 URIs](https://packaging.python.org/en/latest/specifications/dependency-specifiers/#environment-markers) in `pyproject.toml` files, [detect_product_short] now correctly parses versions from wheel and archive URLs and VCS references for impacted detectors (Setuptools CLI, Setuptools Parse, and UV Lock detectors). When data is missing or badly formatted, detectors gracefully switch back to reporting only the package name.
* (IDETECT-4724) Updated Yarn Detector to correctly identify components that were previously unmatched.
* (IDETECT-4850) Log a warning when unsupported `PROC_MACRO` dependency exclusion is attempted with the Cargo Lockfile Detector.
* (IDETECT-4591) The logic for enabling the IAC_SCAN tool has been updated to rely solely on detect.tools and detect.tools.excluded.
* (IDETECT-4786) `BDIO` uploads will no longer retry unnecessarily when the Black Duck SCA server returns a 412 (Precondition Failed), improving scan efficiency and avoiding timeouts.

### Dependency updates

