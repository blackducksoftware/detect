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

## Version 11.2.0

### New features

* [detect_product_short] now supports Rush Package Manager. For details and configuration information, see: [Rush Detector](packagemgrs/rush.md)
* Introducing Quack Patch: An AI-assisted code patching tool integrated into [detect_product_short] to help developers generate code patches for vulnerable components. For more information, see: [Quack Patch Documentation](runningdetect/quack-patch.md)

### Changed features


### Resolved issues

* (IDETECT-4924) Resolved an issue where Impact Analysis Scan threw errors on malformed classes; it now handles them gracefully by logging a warning, skipping the affected classes, and adding them to the scan output.
* (IDETECT-4921) Fixed upload failures in proxied environments when SCASS is enabled. 

### Dependency Updates

* Updated method-analyzer-core to 1.0.7.
* Upgraded and released Nuget Inspector version 2.5.0.

