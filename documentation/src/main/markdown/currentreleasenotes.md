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

## Version 11.3.0

### New features

* With the addition of the `detect.cargo.included.features` and `detect.cargo.disable.default.features` properties, [detect_product_short] now supports Cargo features and the inclusion or exclusion of dependencies as options. See [Cargo](properties/detectors/cargo.md) for details.
  <note type="note">This feature is supported for Cargo CLI Detector. Cargo Lockfile Detector will log a warning if these properties are provided.</note>
* (IDETECT-4937) Add support for `environment.yaml` in [detect_product_short] Conda CLI Detector.

### Resolved issues

* (IDETECT-4960) Added support for Cargo features and optional dependencies in Cargo CLI Detector, allowing precise control over which features are included in the SBOM through cargo tree command flags. See [Cargo](properties/detectors/cargo.md) for details.
* (IDETECT-4847) Clarified that the value of `detect.container.scan.file.path` should be a local .tar file path or HTTP/HTTPS URL for a remote .tar file.
* (IDETECT-4970) Fixed an issue where a `quack-patch` output directory was created despite the feature not being enabled.