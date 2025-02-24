# Current Release notes

**Notices**   

[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* As part of this activity, sig-repo.synopsys.com and detect.synopsys.com are being deprecated. Please make use of repo.blackduck.com and detect.blackduck.com respectively. 
    * After March 2025, [detect_product_short] script download details will only be available via detect.blackduck.com.
    * [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.
    * If you are using [detect_product_short] 8 or 9 it is essential to update to 8.11.2 or 9.10.1 respectively, before sig-repo is decommissioned.   

<note type="note">It is recommended that customers continue to maintain sig-repo.synopsys.com, and repo.blackduck.com on their allow list until March 31st, 2025 when sig-repo.synopsys.com will be fully replaced by repo.blackduck.com.</note>

* Effective March 1, 2025, [bd_product_long] is implementing a new [SCA Scanning Service (SCAss)](https://community.blackduck.com/s/question/0D5Uh00000O2ZSYKA3/black-duck-sca-new-ip-address-requirements-for-2025) which will require customers to add or update IP addresses configured in their network firewalls or allow lists. This action is required to successfully route scan data to the new service for processing.

	* scass.blackduck.com - 35.244.200.22
	* na.scass.blackduck.com - 35.244.200.22
	* na.store.scass.blackduck.com - 34.54.95.139
	* eu.store.scass.blackduck.com - 34.54.213.11
	* eu.scass.blackduck.com - 34.54.38.252

## Version 10.3.0

### New features

* Added support for `ArtifactsPath` and `BaseIntermediateOutputPath` properties in [detect_product_long] NuGet Inspector. See [detect.nuget.artifacts.path](packagemgrs/nuget.md#nuget-artifacts-and-base-intermediate-output-paths) for more details.
* SCA Scan Service (SCASS) is a scalable solution for performing software composition analysis scans outside of the traditional [bd_product_long] environment. This [detect_product_short] release provides support for the SCA Scanning Service (SCAss) for [bd_product_short] version 2025.1.1 or later. For further information see [About SCAss](https://documentation.blackduck.com/bundle/bd-hub/page/ComponentDiscovery/aboutScaScanService.html).
	* See IP address notice above for details on related IP configuration for your deployments.

### Resolved issues

* (IDETECT-4610) - Improved [detect_product_short]'s air gap for Gradle creation script to prevent unwanted JAR files from being included in the gradle subdirectory.
* (IDETECT-4611) - Updated [detect_product_short]'s air gap for Gradle creation script to remove reference to Integration Common library that is no longer a dependency.
* (IDETECT-3932) - Improved the exit code and error output generated when a duplicate project name is used in simultaneous scans.
* (IDETECT-4327) - Updated the Conan 2 detector to provide log entries in case of error.

### Dependency updates

* Upgraded and released NuGet Inspector version 2.1.0.
