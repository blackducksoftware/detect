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

* **Removal of Java 8 support** - Support for Java 8 has been removed in compliance with EU Cyber Resilience Act (CRA) requirements.
* **Deprecation of support for Java versions earlier than 17** - Support for Java versions earlier than 17 has been deprecated in [detect_product_short] 12.0.0 and will be removed in 13.0.0 to align with EU Cyber Resilience Act (CRA) requirements and compliance timelines.
* **Deprecation of Docker Inspector** - Docker Inspector has been deprecated and will be removed in 13.0.0 release.

## Version 12.1.0

### New features

* 

### Changed features

* 

### Resolved issues

* (IDETECT-5267) Fixed cargo detectors to handle pre-release version suffixes and malformed version output.

### Dependency Updates

* 
