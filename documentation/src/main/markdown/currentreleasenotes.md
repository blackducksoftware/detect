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
	
* **Deprecation of Java 8 support** - In alignment with EU Cyber Resilience Act (CRA) requirements and compliance timelines, Java 8 support will be deprecated in the anticipated 2026 Q3 Detect 12.0.0 release.

## Version 12.0.0

### New features

* Support for npm has been extended to 11.13.0 and Node.js 24.17.0.
* Introduced the property `detect.project.version.create.when.no.components` (default: true). When configured to false, [detect_product_short] will refrain from creating a project version in [bd_product_long] in cases where no components are identified and no other scan tools are active.
* Introduced a property named `detect.diagnostic.archive.path`, which enables the specification of a custom path for the diagnostic archive.

### Dependency Updates
* Update ANTLR library to version 4.13.2.
* Update Jackson libraries to version 2.22.0.
* Update Java minimum version to 11.
