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

* When enabled, the new `detect.project.deep.license` property sets the Deep License Data and Deep License Data Snippet fields when creating a project. This property can also be used to update existing projects when the `detect.project.version.update` property is set to true.

### Changed features

* ReversingLabs Scans (`detect.tools=THREAT_INTEL`) has been removed.
* The `detect.threatintel.scan.file.path` property has been removed.
* The `detect.project.codelocation.unmap` property has been removed.

### Resolved issues

* (IDETECT-4738) Corrected behavior of `detect.binary.scan.file.name.patterns` to be case-insensitive. 
* (IDETECT-4802) Fix UV Lockfile Detector not generating BDIOs for projects with non-normalized names per Python requirements.
* (IDETECT-4806) Fixed UV detectors to handle dynamic versions and cyclic dependencies.
* (IDETECT-4751) Prevent server-side parsing errors by normalizing IAC Scan `results.json` contents before uploading to Black Duck SCA.
* (IDETECT-4799) When constructing the BDIO, ignore the Go toolchain directive, as it is the Go project's build-time configuration setting and not a module dependency.
* (IDETECT-4813) Fix Gradle Native Inspector to correctly identify projects with only settings.gradle or settings.gradle.kts file in the root directory.
* (IDETECT-4812) Refactored init-script-gradle.ftl to add support for configuration cache in Gradle projects.


### Dependency updates

* 