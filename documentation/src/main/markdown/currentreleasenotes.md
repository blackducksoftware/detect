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

## Version 10.5.0

### New features

* Support for UV Package Manager has been added under [UV Detector](packagemgrs/python.md#uv-package-manager)
* With the `detect.clone.project.version.name` parameter specified and `detect.project.version.update` set to true, [detect_product_short] will now clone, scan, and update the cloned project via parameters such as `detect.project.version.phase`.

### Resolved issues

* (IDETECT-4177) - [detect_product_short] no longer requires that the X-Artifactory-Filename header is set when specifying an internally hosted version in [bd_product_long].
* (IDETECT-3512) - [detect_product_short] now correctly sends an indicator specifying all categories when `detect.project.clone.categories` is set to ALL. This prevents problems where [bd_product_long] and [detect_product_short] disagree on the full list of categories.

