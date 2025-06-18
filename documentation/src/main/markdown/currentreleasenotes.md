# Current Release notes

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

* A new property, [detect.stateless.policy.check.fail.on.severities](properties/basic-properties.html#ariaid-title34) has been added, which will trigger [detect_product_short] to fail the scan and notify the user if a policy violation matches the configured value. This property overrides the default "Blocker" and "Critical" severity settings that cause [detect_product_short] scans to exit. This property applies to both [Rapid](runningdetect/rapidscan.md) and [Stateless](runningdetect/statelessscan.md) scans. Intelligent persistent scans, (when scan mode is not set to RAPID, STATELESS, or [--detect.blackduck.scan.mode](properties/all-properties.html#ariaid-title5) is explicitly set to INTELLIGENT and scan data is persisted), should continue using the [detect.policy.check.fail.on.severities](properties/basic-properties.html#ariaid-title34), property.

### Changed features

* 

### Resolved issues

* 
### Dependency updates

* 
