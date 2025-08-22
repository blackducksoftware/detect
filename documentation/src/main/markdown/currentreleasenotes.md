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
* [detect_product_short] Docker Inspector air gap distribution JAR files are now digitally signed with Black Duck Software, Inc authority.
* [detect_product_short] Nuget Inspector binaries for macOS and Windows binaries are now digitally signed with Black Duck Software, Inc authority.

### Changed features

* 

### Resolved issues

* (IDETECT-3456) BOM components marked as "ignored" will no longer appear in [detect_product_short] risk reports.
* (IDETECT-4781) Signature Scans will no longer fail if SCA Scan Service (SCASS) related IPs are blocked. A performance warning will be printed and a non-SCASS Signature Scan will be performed.
* (IDETECT-4759) Updated [detect_product_short] UV Detector to prevent execution when the `toml` file does not have a `[tool.uv]` section, and to not return a success status unless a BDIO file is generated.
* (IDETECT-4746) Fixed Cargo Lockfile Detector incorrectly labeling transitive dependencies as direct dependencies.
* (IDETECT-4728) Rapid Scans using `BOM_COMPARE_STRICT` now show a clear message if the project version doesn’t exist, guiding users to run a full scan to create it.
* (IDETECT-4736) Gradle Native Inspector no longer appends `+FAILED` suffix to unresolved dependency versions in BDIO output.

### Dependency updates

* Upgraded and released Docker Inspector version 11.5.0.
* Upgraded and released Nuget Inspector version 2.3.0.
* Updated the Black Duck Software BDIO2 protobuf library to version 3.2.12 to resolve a security vulnerability in its Google Protobuf Java library. 
