# Current Release notes

**Notice**
[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* As part of this activity, sig-repo.synopsys.com and detect.synopsys.com are being deprecated. Please make use of repo.blackduck.com and detect.blackduck.com respectively. 
    * After February 2025, [detect_product_short] script download details will only be available via detect.blackduck.com.
    * [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.
    * If you are using [detect_product_short] 8 or 9 it is essential to update to 8.11.2 or 9.10.1 respectively, before sig-repo is decommissioned.   

<note type="note">It is recommended that customers continue to maintain sig-repo.synopsys.com, and repo.blackduck.com on their allow list until February 2025 when sig-repo.synopsys.com will be fully replaced by repo.blackduck.com.</note>

## Version 10.1.0

### New features

* npm lockfile and shrinkwrap detectors now ignore packages flagged as extraneous in the package-lock.json and npm-shrinkwrap.json files.
* Support added for Opam Package Manager via [Opam Detector](packagemgrs/opam.md).
* New Gradle Native Inspector option to only process the root dependencies of a Gradle project. See [detect.gradle.root.only](properties/detectors/gradle.md#gradle-root-only-enabled-advanced) for more details.

### Changed features

* npm version 1 package-lock.json and npm-shrinkwrap.json file parsing has been restored.
* The `detect.project.codelocation.unmap` property has been deprecated.
* Changed [detect_product_long]'s JAR signing authority from Synopsys, Inc. to Black Duck Software, Inc.
* Yarn 4 support does not require use of --detect.yarn.ignore.all.workspaces flag. Thus, properties like detect.yarn.dependency.types.excluded=NON_PRODUCTION are available for Yarn 4 projects.

### Resolved issues

* (IDETECT-4517) - [detect_product_short] now correctly indicates a timeout failure occurred when multipart binary or container scans timeout during an upload.
* (IDETECT-4540) - Multipart binary and container scans now correctly retry when authentication errors are received during transmission.
* (IDETECT-4469) - Eliminating null (`\u0000`) and replacement (`\uFFFD`) characters during the processing of Python requirements.txt files to ensure successful extraction of dependency information.
* (IDETECT-4447) - ID strings of detected Yarn project dependencies are now correctly formed. The warning message cited to report this bug has been improved to mean that the entries in the yarn.lock file that have not so far been resolved through package.json files, could not be resolved with any standard NPM packages either.

### Dependency updates

* Upgraded and released [docker_inspector_name] version 11.1.0.
* Upgraded to [project_inspector_name] v2024.12.1.