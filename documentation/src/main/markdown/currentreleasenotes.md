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

## Version 11.1.0

### New features

* The Component Location Analysis feature has been extended to the Cargo package manager.

### Changed features

* When using the `detect.excluded.detectors` property, any fallback Detectors will now we executed if the primary Detector is excluded. Previously, entire sets of Detectors would be excluded.

### Resolved issues

* (IDETECT-4874) Improved support for multibyte characters in project names, version names, and code location names during package manager scans.
* (IDETECT-4880) The `.bridge` directory will now be excluded by default from Detector and Signature Scans.
* (IDETECT-4897) [detect_product_short] now looks for headers in a case-insensitive fashion when performing multipart binary uploads.
* (IDETECT-4707) The PIP Native Inspector now appropriately handles package names containing a dot character.
* (IDETECT-4864) The UV Detector now appropriately runs even if the optional field `[tool.uv] manage = true` is not specified.
* (IDETECT-4760) Any dependencies listed in a Gradle dependency tree as a `(c)` dependency constraint will no longer be identified as dependencies unless they also appear elsewhere in the tree.
* (IDETECT-4860) When Component Location Analysis is enabled, metadata section of component-source.json will now contain 'dependencyTrees' field from Rapid scan results.
* (IDETECT-4923) Fixed a bug during pyproject.toml parsing when project name could not be derived.

