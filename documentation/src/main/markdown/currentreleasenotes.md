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

## Version 11.5.0

### Changed features

* The default output directory of the Quack Patch feature has been updated to use [detect_product_short] scan output directory. For more information, see [Quack Patch Documentation](runningdetect/quack-patch.md).
* CentOS support in Detect Docker Inspector has been deprecated and will be removed in 12.0.0. For more details, please see [Docker Inspector Release Notes](packagemgrs/docker/releasenotes.md).
    * imageinspector.service.port.centos has been deprecated and will be removed in 12.0.0.
* Clarified documentation for `--detect.uv.dependency.groups.excluded`. Since optional is not a dependency group in uv but a section defining extras, exclusions must reference the extra name directly (e.g., postgres, redis). Supplying optional has no effect.

### Resolved issues
* (IDETECT-5125) Fixed failure during Python scans when the `requirements.txt` file contains Python extras syntax using square brackets, e.g.: `kopf[dev]>=1.3`
* (IDETECT-5090) Fixed PIP Native Inspector failure to parse `requirements.txt` lines that contain [PEP 508 environment markers](https://peps.python.org/pep-0508/).
* (IDETECT-5056) Fixed a Cargo Lock detector failure to parse the caret symbol '^' used in `Cargo.toml` dependency declarations.
* (IDETECT-5071) Fixed an issue with Simple Build Tool (sbt) evictions being included in the BOM.
* (IDETECT-5069) Fixed Setuptools parsing for unsupported install_requires syntax in setup.py: Detect now fails fast and logs an error instead of silently misparsing, generating an incorrect BOM, and incorrectly reporting success.
* (IDETECT-5140) Changed the default output directory of the Quack Patch feature to use [detect_product_short] scan output directory instead of the current working directory.
* (IDETECT-5121) Include Quack Patch output directory as part of diagnostic zip when the feature is enabled.
* (IDETECT-5064) Updated the Gradle init script to explicitly assign an empty configuration set to phantom projects (container modules lacking a `build.gradle` file). This change prevents tools injected by plugins such as Detekt and Ktlint from being included in the dependency report.
* (IDETECT-5097) Updated the Gradle init script to enumerate configurations within `gradle.projectsEvaluated`, ensuring that all `afterEvaluate` callbacks, including those from the Android Gradle Plugin (AGP), have completed before configuration processing begins.
* (IDETECT-5163) Updated the Bazel detector to treat exit code `3` from `query` and `cquery` commands as a partial success. When encountered, the detector now processes any available output and issues a warning indicating that dependency results may be incomplete.
* (IDETECT-5053) / (IDETECT-4988) Fixed pip inspector to correctly parse PEP 440 direct reference packages (`name @ url`), ensuring these packages are included in the dependency tree rather than being omitted.
* (IDETECT-5078) Rather then fail, Detect will now complete scans and generate empty BOMs when a Python Setuptools project has no dependencies.
* (IDETECT-5079) Allow Detect scans to finish with success even if no configured binary file patterns (e.g., .jar, .war, .zip) are found.
* (IDETECT-5118) Fixed UV Lockfile Detector to respect excluded dependency groups for optional‑dependencies. Optional extras specified in exclusion flags are now correctly excluded alongside development dependencies.
* (IDETECT‑5126) Fixed a BitBake layer misidentification issue caused by project folder names colliding with layer names. The detector now resolves layers deterministically, preferring the deepest valid match and falling back to the first valid layer when necessary.

### Dependency Updates

* Updated Project Inspector to version 2026.6.0 ensuring continued security compliance.