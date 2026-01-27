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
* Control over which workspace members are included or excluded during scanning is made possible by the new `detect.cargo.included.workspaces` and `detect.cargo.excluded.workspaces` properties for Cargo Detector. See [Cargo](properties/detectors/cargo.md) for details.
* When set to true (default: false), the new `detect.cargo.ignore.all.workspaces` property allows you to completely disable workspace support. See [Cargo](properties/detectors/cargo.md) for more information.

### Changed features


### Resolved issues

* (IDETECT-4924) Resolved an issue where Impact Analysis Scan threw errors on malformed classes; it now handles them gracefully by logging a warning, skipping the affected classes, and adding them to the scan output.
* (IDETECT-4921) Fixed upload failures in proxied environments when SCASS is enabled. 
* (IDETECT-4919) Added Cargo workspace support in Cargo detectors. [detect_product_short] now identifies `[workspace]` in the root `Cargo.toml` and resolves dependencies across all members using the shared `Cargo.lock`. The "Additional_components" section has been removed from SBOMs for completeness.
* (IDETECT-4860) When Component Location Analysis is enabled, metadata section of component-source.json will now contain 'dependencyTrees' field from Rapid scan results.
* (IDETECT-4923) Fixed a bug during pyproject.toml parsing when project name could not be derived.

### Dependency Updates

* Updated method-analyzer-core to 1.0.7.

