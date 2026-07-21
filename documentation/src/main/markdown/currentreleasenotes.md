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

* Introduced the property `detect.project.version.create.when.no.components` (default: true). When configured to false, [detect_product_short] will refrain from creating a project version in [bd_product_long] in cases where no components are identified and no other scan tools are active.
* Introduced a property named `detect.diagnostic.archive.path`, which enables the specification of a custom path for the diagnostic archive.
* Renamed `detect.quack.patch.output` property to `detect.quack.patch.output.path` for improved clarity.
* Support for the following package managers have been extended:
    * RubyGems: 4.0.15
    * Gradle: 9.6.1
    * Maven: 3.9.16
    * Pnpm: 11.8.0
    * NPM: 11.13.0
    * Node.js: 24.17.0

### Changed features

* Added `detect.uv.dependency.groups.only` property for the UV CLI detector. To restrict scanning to specific dependency groups while excluding standard dependencies and optional extras, use this property. When set, Detect limits analysis to the explicitly listed dependency groups defined in the project's pyproject.toml. Multiple groups can be specified as a comma-separated list (e.g., `detect.uv.dependency.groups.only='dev,lint'`). This applies exclusively to groups under the `[dependency-groups]` section; extras under `[project.optional-dependencies]` are not included. If both this property and `detect.uv.dependency.groups.excluded` are configured, the exclusion setting takes precedence for any overlapping groups and Detect will log a warning.

### Resolved issues
* (IDETECT-5207) Fixed an IndexOutOfBoundsException in component location analysis that was due to space characters within a version string.  

### Dependency Updates
* Update ANTLR library to version 4.13.2.
* Update Jackson libraries to version 2.22.0.
* Update Java minimum version to 11.
* Update Tika library to version 3.2.2.