# Current Release notes

**Notice**
[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* As part of this activity, sig-repo.synopsys.com and detect.synopsys.com are being deprecated. Please make use of repo.blackduck.com and detect.blackduck.com respectively. 
    * After March 2025, [detect_product_short] script download details will only be available via detect.blackduck.com.
    * [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.
    * If you are using [detect_product_short] 8 or 9 it is essential to update to 8.11.2 or 9.10.1 respectively, before sig-repo is decommissioned.   

<note type="note">It is recommended that customers continue to maintain sig-repo.synopsys.com, and repo.blackduck.com on their allow list until March 31st, 2025 when sig-repo.synopsys.com will be fully replaced by repo.blackduck.com.</note>

## Version 10.3.0

### Resolved issues

* (IDETECT-4610) - Improved [detect_product_short]'s air gap for Gradle creation script to prevent unwanted JAR files from being included in the gradle subdirectory.
* (IDETECT-4611) - Updated [detect_product_short]'s air gap for Gradle creation script to remove reference to Integration Common library that is no longer a dependency.

### New features

* Added support for ArtifactsPath and BaseIntermediateOutputPath properties in [detect_product_long] NuGet Inspector.

### Changed features

* 

### Resolved issues

* 

### Dependency updates

* Upgraded and released NuGet Inspector version 2.1.0.
* Upgraded to Rebranded Method Analyzer Core Library version 1.0.1 for Vulnerability Impact Analysis.
