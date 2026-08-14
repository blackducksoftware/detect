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

## Version 12.0.0

### New features

* The Bazel detector now classifies Bazel Central Registry (BCR) dependencies as direct or transitive when running in Bzlmod mode on Bazel 7.1 or later.
* Introduced the property `detect.project.version.create.when.no.components` (default: true). When configured to false, [detect_product_short] will refrain from creating a project version in [bd_product_long] in cases where no components are identified and no other scan tools are active.
* Introduced a property named `detect.diagnostic.archive.path`, which enables the specification of a custom path for the diagnostic archive.
* Added `detect.uv.dependency.groups.only` property for the UV CLI detector. To restrict scanning to specific dependency groups while excluding standard dependencies and optional extras, use this property. When set, Detect limits analysis to the explicitly listed dependency groups defined in the project's pyproject.toml. Multiple groups can be specified as a comma-separated list (e.g., `detect.uv.dependency.groups.only='dev,lint'`). This applies exclusively to groups under the `[dependency-groups]` section; extras under `[project.optional-dependencies]` are not included. If both this property and `detect.uv.dependency.groups.excluded` are configured, the exclusion setting takes precedence for any overlapping groups and Detect will log a warning.
* Added the `detect.npm.excluded.workspaces` and `detect.npm.included.workspaces` configuration properties to control which npm workspaces are included in a [detect_product_short] scan. If a workspace is specified in both lists, the exclusion takes precedence. Added the `detect.npm.ignore.all.workspaces` property to exclude all npm workspaces when set to true, which is equivalent to excluding every workspace explicitly.
* Support for the following package managers have been extended:
	* RubyGems: 4.0.15
	* Gradle: 9.6.1
	* Maven: 3.9.16
	* Pnpm: 11.8.0
	* NPM: 11.13.0
	* Node.js: 24.17.0

### Changed features

* (IDETECT-5117) The UV detector now scans all dependency groups by default. In previous releases, only the default group was included in the scan. The detector now passes the `--all-groups` flag to the `uv tree` command, ensuring all groups defined under `[dependency-groups]` in `pyproject.toml` are included. To restrict the scan to specific groups, use the `detect.uv.dependency.groups.only` property.
* (IDETECT-5134) Enabled UTF-8 encoding when reading the pnpm-lock.yaml file allowing emojis and non-ASCII characters to be parsed.
* (IDETECT-5146) pnpm scans now complete when the pnpm-lock.yaml has no dependencies.
* [detect_product_short] Docker Inspector support for RPM-based Linux platforms such as CentOS, and RedHat has been removed in 12.0.0.
* Renamed `detect.quack.patch.output` property to `detect.quack.patch.output.path` for improved clarity.
* `detect.bazel.workspace.rules` property removed.


### Dependency Updates
* Upgraded direct and transitive dependencies throughout [detect_product_short], plugins and add-ons.
	* [detect_product_short] Docker images were migrated to Chainguard.
	* Update ANTLR library to version 4.13.2.
	* Update Jackson libraries to version 2.22.0.
	* Update Java minimum version to 11.
	* Update Tika library to version 3.2.2.
	* Update Component Locator Library to version 2.4.5