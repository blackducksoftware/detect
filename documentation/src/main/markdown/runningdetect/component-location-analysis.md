# Component Location Analysis

Enable this feature by adding the `--detect.component.location.analysis.enabled=TRUE` parameter to a run of [detect_product_long].

[detect_product_short] generates a `components-with-locations.json` file in the scan subdirectory of the output directory when Component Location Analysis is enabled. This file contains the declaration locations (file path, line number, and column position) of the open-source component versions that were detected in the scanned project.

<note type="note">By default, when [detect_product_short] shuts down, it performs cleanup operations which include deleting the component location analysis file. You can disable clean up by setting `--detect.cleanup=false`.</note>

## Requirements and Limitations

* A subset of Detector Types support this feature.
    * Supported Detectors: CARGO, CONDA, GRADLE, GO_MOD, GO_DEP, GO_GRADLE, GO_VENDOR, GO_VNDR, MAVEN, NPM, NUGET, PIP, POETRY, and YARN.
* Supported scan modes: Offline and Rapid/Stateless.
    * Offline mode
      * When enabled for a scan without [bd_product_short] connectivity, all detected open source components will be included in the location analysis results.
    * Rapid/Stateless Scan mode requires [bd_product_short] policies.
        * Only components that violate policies will be included in the analysis. If no policies are violated or there are no defined policies, component location analysis is skipped.

## Offline Mode Results

Each component is uniquely identified by a name and version. Components may optionally have a higher-level grouping identifier, commonly referred to as a groupId, organization, or vendor. The declaration location of each component is included in the results if found. When not found, no declarationLocation field will be present for that component in the output file. 

<note type="note">The metadata field is only populated in the case of a Rapid or Stateless Scan. See [Rapid or Stateless Scan Mode Results](#rapid-or-stateless-scan-mode-results)</note>

**Example results BODY:**
```
{
    "sourcePath": "/absolute/path/to/project/root",
    "globalMetadata": {},                                 // Passthrough data from producer to consumer (optional)
    "componentList": [
        { 
            "groupID": "org.sonarqube",                   // Component group (if available)
            "artifactID": "org.sonarqube.gradle.plugin",  // Component name
            "version": "2.8",                             // Component version
            "metadata": {                                 // Passthrough upgrade guidance data (unavailable in offline scan)
                "policyViolationVulnerabilities": [],     // (if available)
                "shortTermUpgradeGuidance": {},           // (if available)
                "longTermUpgradeGuidance": {},            // (if available)
                "transitiveUpgradeGuidance": [],          // (if available)
                "componentViolatingPolicies": []          // (if available)
            },
            "declarationLocation": {                      // Included if the component was located
                "fileLocations": [
                    {
                        "filePath": "build-script/build.gradle.kts",
                        "lineLocations": [
                            {
                                "lineNumber": 12,
                                "columnLocations": [
                                    {
                                        "colStart": 63,
                                        "colEnd": 65
                                    }
                                ]
                            }
                        ]
                    }
                ]
            }
        }
    ]
}
```

## Rapid or Stateless Scan Mode Results

When [detect_product_short] runs a Rapid or Stateless scan, the output file includes policy violation vulnerabilities, component violating policies, dependency trees for individual components and remediation guidance (short term, long term and transitive upgrade guidance) when available. This information is contained within the metadata field of each component.

## Version Range Operator Support

Component Location Analysis supports locating dependency declarations that use version range operators. When a version declaration uses a supported operator, an optional `operator` field is included in the `columnLocations` entry to indicate the specific operator used in the declaration.

### Supported Package Managers

* **NPM/YARN**: Fully supported with all semantic versioning operators (`=`, `!=`, `>`, `<`, `>=`, `<=`, `~`, `^`, `-`).
* **PIP**: Partially supported with comparison operators (`==`, `!=`, `>`, `<`, `>=`, `<=`). Note that Python's `~=` operator has different semantics than npm's `~` operator and is not currently supported.

### Supported Operators

The following version range operators are supported for NPM and YARN:

| Operator | Description | Example |
|----------|-------------|---------|
| `=` | Equal | `=1.2.3` |
| `!=` | Not equal | `!=1.2.3` |
| `>` | Greater than | `>1.2.3` |
| `<` | Less than | `<2.0.0` |
| `>=` | Greater than or equal | `>=1.2.3` |
| `<=` | Less than or equal | `<=2.0.0` |
| `~` | Tilde range (patch updates) | `~1.2.3` (≥1.2.3, <1.3.0) |
| `^` | Caret range (minor updates) | `^1.2.3` (≥1.2.3, <2.0.0) |
| `-` | Hyphen range | `1.2 - 1.4.5` (≥1.2.0, ≤1.4.5) |

For PIP, the comparison operators (`==`, `!=`, `>`, `<`, `>=`, `<=`) are supported.

### Output Format

When a version range operator is detected, the `operator` field appears in the `columnLocations` entry:

```json
{
    "lineNumber": 3753,
    "columnLocations": [
        {
            "colStart": 22,
            "colEnd": 27,
            "operator": "^"
        }
    ]
}
```

<note type="note">Version range operators are not supported for Maven, Gradle, and NuGet at this time. These package managers use bracket notation for version ranges, which requires different parsing logic.</note>

