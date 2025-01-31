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
