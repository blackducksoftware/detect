# Current [detect_product_short] release notes

**Notices**

[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* Please make use of repo.blackduck.com and detect.blackduck.com for code downloads.
	* [detect_product_short] script downloads should only be accessed via detect.blackduck.com.
	* [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.
	* If you are using [detect_product_short] 8 or 9 it is essential to update to 8.11.2 or 9.10.1 respectively.

* [bd_product_long] [SCA Scan Service (SCASS)](https://community.blackduck.com/s/question/0D5Uh00000O2ZSYKA3/black-duck-sca-new-ip-address-requirements-for-2025) requires customers add or update IP addresses configured in their network firewalls or allow lists. This action is required to successfully route scan data to the service for processing.

	* scass.blackduck.com - 35.244.200.22
	* na.scass.blackduck.com - 35.244.200.22
	* na.store.scass.blackduck.com - 34.54.95.139
	* eu.store.scass.blackduck.com - 34.54.213.11
	* eu.scass.blackduck.com - 34.54.38.252
	
* **Deprecation of Java 8 support** - In alignment with EU Cyber Resilience Act (CRA) requirements and compliance timelines, Java 8 support will be deprecated in the anticipated 2026 Q3 Detect 12.0.0 release.

## Version 11.4.0

### New features

* Support for the Conda Tree–based detector has been added. For more details, see [Conda Tree](packagemgrs/conda.md#conda-tree-detector).
* Support for pnpm now extends to 10.32.1.
* npm detectors now allow for aliases to be used when specifying dependencies in the package.json file.
* Ivy CLI Detector, leveraging the `ivy:dependencytree` Ant task to extract direct and transitive dependencies for Ant + Ivy projects. For further information, see [Ivy (Ant) support](packagemgrs/ivy.md).

### Resolved issues

* (IDETECT-5014) npm CLI detector now handles components that do not have a version specified, preventing those components from being silently dropped from results.
* (IDETECT-4980) When `detect.clone.project.version.latest` is set to true, an INFO-level log message will be written to identify the exact project version selected as the clone source.
* (IDETECT‑4979) Updated the NuGet Inspector to prevent duplicate components from being reported which end up unversioned in the BOM.
* (IDETECT‑5058) Improved the Poetry detector to eliminate errors encountered while parsing pyproject.toml.
* (IDETECT‑5013) Fixed an issue in the signature scan fallback logic when SCA Scan Service (SCASS) is intentionally bypassed.
* NuGet Solution Native Inspector now supports .slnx files.

### Dependency Updates
* Upgraded and released Nuget Inspector version 2.6.0
* Update tomlj library to version 1.1.1.