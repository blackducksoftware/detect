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

## Version 10.7.0

### New features

* Maven CLI Detector now accepts a custom pom.xml file name (matching the pattern *pom.xml) when provided via `detect.maven.build.command`.  
* Signature Scan now supports ARM architecture with correctly packaged ARM JRE for Windows, Mac, Linux, and Alpine operating systems for [bd_product_long] version 2025.7.0 or later.
<note type="hint">To ensure [detect_product_short] correctly identifies the system architecture on ARM-based systems, please install an ARM-specific Java runtime. This is necessary for accurate detection and proper functionality on ARM platforms.</note>
* Support for Poetry is now extended to 2.1.4.

### Changed features

* 

### Resolved issues

* (IDETECT-3456) BOM components marked as "ignored" will no longer appear in [detect_product_short] risk reports.
* (IDETECT-4781) Signature Scans will no longer fail if SCA Scan Service (SCASS) related IPs are blocked. A performance warning will be printed and a non-SCASS Signature Scan will be performed.
* (IDETECT-4759) Updated [detect_product_short] UV Detector to prevent execution when the `toml` file does not have a `[tool.uv]` section, and to not return a success status unless a BDIO file is generated.
* (IDETECT-4746) Fixed Cargo Lockfile Detector incorrectly labeling transitive dependencies as direct dependencies.
* (IDETECT-4770) The Cargo Dependency Type Exclusion has been expanded to include `NORMAL` dependencies for both the Cargo Lockfile and Cargo CLI detectors. Additionally, the Cargo CLI Detector now supports excluding `PROC_MACRO` dependencies, though this exclusion is limited to the CLI detector due to the absence of explicit `PROC_MACRO` entries in `Cargo.toml` or `Cargo.lock`.

### Dependency updates

* 