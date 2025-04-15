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

## Version 10.4.0

### New features

* Support for Conda has been extended to 25.1.1.
* Cargo CLI Detector, leveraging `cargo tree` to extract direct and transitive dependencies, improving accuracy over the previous flat lockfile detection. This build-based detector is triggered for Cargo projects with a `Cargo.toml` file and requires Cargo version **1.44.0+**. For further information, see [Cargo package manager support](packagemgrs/cargo.md).
* Added property [detect.cargo.path](properties/detectors/cargo.md) to allow user specification of a custom Cargo executable path.   
* New `detect.pnpm.included.packages` and `detect.pnpm.excluded.packages` properties for pnpm provide increased control over what directories [detect_product_short] scans under a pnpm project. See the [pnpm](properties/detectors/pnpm.html) property page for more information.

### Changed features

* If the URL configured for SCA Scan Service (SCASS) is inaccessible when [detect_product_short] attempts a binary or container scan, [detect_product_short] will retry the scan without using SCASS.
	* See [bd_product_long] SCA Scan Service (SCASS) notice above for information pertaining to IP addresses that require allow listing.

### Changed features

* ReversingLabs Scans (`detect.tools=THREAT_INTEL`) has been deprecated.
* The `detect.threatintel.scan.file.path` property has been deprecated. 

### Resolved issues

* (IDETECT-4642) - Improved handling of pnpm packages that contain detailed version information in the pnpm-lock.yaml. Resolving [detect_product_short] missing some packages through failure to link direct and transitive dependencies. 
* (IDETECT-4641) - Improved [detect_product_short]'s Yarn detector to handle non-standard version entries for component dependencies.
* (IDETECT-4602 & IDETECT-4180) - Resolved Go dependency scan issue that resulted in transitive dependencies assigned to incorrect parent.
* (IDETECT-4594) - Resolved [detect_product_short] failing to handle duplicate keys in `package.json` files across npm, pnpm, Lerna, and Yarn projects.