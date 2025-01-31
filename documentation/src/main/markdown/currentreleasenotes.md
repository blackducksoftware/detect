# Current Release notes

**Notice**
[company_name] [solution_name] has been renamed [detect_product_long] with page links, documentation, and other URLs updated accordingly. Update any [detect_product_short] documentation, or other bookmarks you may have. See the [Domain Change FAQ](https://community.blackduck.com/s/article/Black-Duck-Domain-Change-FAQ).
* As part of this activity, sig-repo.synopsys.com and detect.synopsys.com are being deprecated. Please make use of repo.blackduck.com and detect.blackduck.com respectively. 
    * After February 2025, [detect_product_short] script download details will only be available via detect.blackduck.com.
    * [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com.
    * If you are using [detect_product_short] 8 or 9 it is essential to update to 8.11.2 or 9.10.1 respectively, before sig-repo is decommissioned.   

<note type="note">It is recommended that customers continue to maintain sig-repo.synopsys.com, and repo.blackduck.com on their allow list until February 2025 when sig-repo.synopsys.com will be fully replaced by repo.blackduck.com.</note>

## Version 10.2.1

### Resolved issues

* (IDETECT-4560) - Fix to the FTL script used to build the [detect_product_short] Air Gap zips to prevent outdated JARs from being included.

## Version 10.2.0

### New features

* The scanCLI `detect.blackduck.signature.scanner.csv.archive` property has been added for generating and uploading CSV files to [bd_product_long] 2025.1.0 or later. If used in offline mode, the generated CSV files will be located in the [detect_product_short] run directory in the csv folder.
<note type="note">This feature is only available for intelligent persistence scans.</note>

### Changed features

* Use of the --detect.yarn.ignore.all.workspaces flag is not required for Yarn 4 projects, thus configuration parameters such as detect.yarn.dependency.types.excluded=NON_PRODUCTION can be employed.

### Resolved issues

* (IDETECT-4447) - ID strings of detected Yarn project dependencies are now correctly formed. Related warning messages have been improved to identify entries in the yarn.lock file that have not been resolved through package.json files and could not be resolved with any standard NPM packages.
* (IDETECT-4533) - Resolved an issue with [detect_product_short] Gradle Native Inspector causing scans to hang indefinitely when submodule has the same name as the parent module.
* (IDETECT-4560) - Updated version of Jackson-Core (a transitive dependency) to address a vulnerability.
