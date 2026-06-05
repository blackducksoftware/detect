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

### New features

* Support for the Conda Tree–based detector has been added. For more details, see [Conda Tree](packagemgrs/conda.md#conda-tree-detector).
* Support for pnpm now extends to 10.32.1.
* npm detectors now allow for aliases to be used when specifying dependencies in the package.json file.
* Ivy CLI Detector, leveraging the `ivy:dependencytree` Ant task to extract direct and transitive dependencies for Ant + Ivy projects. For further information, see [Ivy (Ant) support](packagemgrs/ivy.md).

### Changed features

* The default output directory of the Quack Patch feature has been updated to use [detect_product_short] scan output directory. For more information, see [Quack Patch Documentation](runningdetect/quack-patch.md).
* CentOS support in Detect Docker Inspector has been deprecated and will be removed in 12.0.0. For more details, please see [Docker Inspector Release Notes](releasenotes.md).
    * imageinspector.service.port.centos has been deprecated and will be removed in 12.0.0.

### Resolved issues

* (IDETECT-5140) Changed the default output directory of the Quack Patch feature to use [detect_product_short] scan output directory instead of the current working directory.
* (IDETECT-5121) Include Quack Patch output directory as part of diagnostic zip when the feature is enabled.
* (IDETECT-5064) Updated the Gradle init script to explicitly assign an empty configuration set to phantom projects (container modules lacking a build.gradle file). This change prevents tools injected by plugins such as Detekt and Ktlint from being included in the dependency report.
* (IDETECT-5097) Updated the Gradle init script to enumerate configurations within gradle.projectsEvaluated, ensuring that all afterEvaluate callbacks, including those from the Android Gradle Plugin (AGP), have completed before configuration processing begins.
* (IDETECT-5163) Updated the Bazel detector to treat exit code 3 from query and cquery commands as a partial success. When encountered, the detector now processes any available output and issues a warning indicating that dependency results may be incomplete.
* (IDETECT-5053) Fixed pip inspector to correctly parse PEP 440 direct reference packages (name @ url), ensuring these packages are included in the dependency tree rather than being omitted.

### Dependency Updates