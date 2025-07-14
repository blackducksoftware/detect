# Current [detect_product_short] release notes

**Notices**   

[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* As part of this activity, sig-repo.synopsys.com and detect.synopsys.com are being deprecated. Please make use of repo.blackduck.com and detect.blackduck.com respectively. 
    * [detect_product_short] script downloads should only be accessed via detect.blackduck.com.
    * [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.
    * If you are using [detect_product_short] 8 or 9 it is essential to update to 8.11.2 or 9.10.1 respectively, before sig-repo is decommissioned.   

<note type="note">It is recommended that customers continue to maintain sig-repo.synopsys.com, and repo.blackduck.com on their allow list until such time as all scripts, services, or pipelines have been updated with the repo.blackduck.com URL.</note>

* [bd_product_long] [SCA Scan Service (SCASS)](https://community.blackduck.com/s/question/0D5Uh00000O2ZSYKA3/black-duck-sca-new-ip-address-requirements-for-2025) requires customers add or update IP addresses configured in their network firewalls or allow lists. This action is required to successfully route scan data to the new service for processing.

	* scass.blackduck.com - 35.244.200.22
	* na.scass.blackduck.com - 35.244.200.22
	* na.store.scass.blackduck.com - 34.54.95.139
	* eu.store.scass.blackduck.com - 34.54.213.11
	* eu.scass.blackduck.com - 34.54.38.252

## Version 10.6.0

### New features

* SCA Scan Service (SCASS) has been extended to work with package manager and signature scans. For further information see [About SCASS](https://documentation.blackduck.com/bundle/bd-hub/page/ComponentDiscovery/aboutScaScanService.html)

* A new property, [detect.stateless.policy.check.fail.on.severities](properties/basic-properties.html#ariaid-title34) has been added, which will trigger [detect_product_short] to fail the scan and notify the user if a policy violation matches the configured value. This property overrides the default "Blocker" and "Critical" severity settings that cause [detect_product_short] scans to exit. This property applies to both [Rapid](runningdetect/rapidscan.md) and [Stateless](runningdetect/statelessscan.md) scans. Intelligent persistent scans, (when scan mode is not set to RAPID, STATELESS, or [--detect.blackduck.scan.mode](properties/all-properties.html#ariaid-title5) is explicitly set to INTELLIGENT and scan data is persisted), should continue using the [detect.policy.check.fail.on.severities](properties/basic-properties.html#ariaid-title34), property.

* To provide greater control over Cargo dependencies reported in the BOM, a new property, `detect.cargo.dependency.types.excluded` has been added to allow exclusion of specific Cargo dependency types (`DEV`, `BUILD`) from scans. The default behavior (`NONE`) will include all dependency types.

* Node Package Manager (npm) scans now report optional dependencies. The `detect.npm.dependency.types.excluded` property has been extended to exclude optional dependencies if OPTIONAL is specified in the list of arguments.

* [detect_product_long] will now provide an option to generate [bd_product_long] risk report in a parseable (JSON) format. See [Risk Report Generation](results/reports.md) for more details.

* Support for SBT is now extended to 1.11.3.

### Changed features

* To improve processing time when both PNPM and NPM detectors apply to a directory, only PNPM detector will execute, producing the same quality of results.

### Resolved issues

* (IDETECT-4657) - Additional logging has been added for occurances of [detect_product_short] erroring out when loading a malformed Spring Boot config file. 
<note type="hint">A valid Spring Boot config file can be specified via the `--spring.config.location=""` parameter.</note>

### Dependency updates

* Upgraded and released Detect Docker Inspector version 11.4.0.