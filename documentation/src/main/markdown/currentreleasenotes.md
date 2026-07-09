# Current [detect_product_short] release notes

**Notices**

* Please make use of repo.blackduck.com and detect.blackduck.com for code downloads.
	* [detect_product_short] script downloads should only be accessed via detect.blackduck.com.
	* [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.

* [bd_product_long] [SCA Scan Service (SCASS)](https://community.blackduck.com/s/question/0D5Uh00000O2ZSYKA3/black-duck-sca-new-ip-address-requirements-for-2025) requires customers add or update IP addresses configured in their network firewalls or allow lists. This action is required to successfully route scan data to the service for processing.

	* scass.blackduck.com - 35.244.200.22
	* na.scass.blackduck.com - 35.244.200.22
	* na.store.scass.blackduck.com - 34.54.95.139
	* eu.store.scass.blackduck.com - 34.54.213.11
	* eu.scass.blackduck.com - 34.54.38.252
	
* **Deprecation of Java 8 support** - In alignment with EU Cyber Resilience Act (CRA) requirements and compliance timelines, Java 8 support will be deprecated in the anticipated 2026 Q3 Detect 12.0.0 release.

## Version 12.0.0

### New features

* Introduced a property named `detect.diagnostic.archive.path`, which enables the specification of a custom path for the diagnostic archive.
* Support for the following package managers have been extended:
  * RubyGems: 4.0.15
  * Gradle: 9.6.1
  * Maven: 3.9.16
  * Pnpm: 11.8.0
  * NPM: 11.13.0
  * Node.js: 24.17.0

### Dependency Updates
* Update ANTLR library to version 4.13.2.
* Update Jackson libraries to version 2.22.0.
* Update Java minimum version to 11.