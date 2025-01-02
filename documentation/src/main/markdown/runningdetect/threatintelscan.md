# [threat_intel] Scan

[threat_intel] Scans are a way of running binary file analysis that provides malware warnings, with a risk analysis level applied, for open source and commercial software.

[detect_product_short] will accept a user provided local file path to a binary file for [threat_intel] Scan. This file may be a single executable, or a compressed file, such as a tar or zip, that contains many files for analysis.

Identification of malware displayed to [bd_product_short] users will include file name, file path, and other identifiers, along with a description of the type of malware, and severity of the findings.

## Workflow

1. The file for scanning is uploaded to [bd_product_short] Storage service by [detect_product_short].   
1. Once uploaded, [threat_intel] service takes the file from Storage service and downloads it to its own container.   
1. The [threat_intel] service invokes [threat_intel] tools to extract any archived files and generate file hashes.   
1. [threat_intel] sends the SHA-1 hash of the uploaded file, along with hashes, and their size in bytes, of files extracted from any archives.
1. Once complete, a report in JSON format is sent back to [threat_intel] service, which is then forwarded to [bd_product_short] Scan service. This report is saved in the [bd_product_short] database.   
<note type="note">The scanned file is removed from Storage service when the scan completes, and [threat_intel] service does not persist any data for this file.</note>

## Requirements and Limitations

### General Requirements
 * [bd_product_short] server must have the appropriate [threat_intel] license.
 * [detect_product_short] 9.6.0 or greater.
 * Must be running [bd_product_short] 2024.4.0 or greater.
 * The [threat_intel] service container (rl-service) must be running.
 * [threat_intel] scans require network connectivity (Air gap mode is not supported).
    * For more network requirement information please consult the documentation provided by [bd_product_short] under the topic:
<xref href="Require_Network.dita" scope="peer"> Network requirements
<data name="facets" value="pubname=bd-hub"/></xref>   
   
 * [threat_intel] scan does not provide project and version name defaults so you need to set project and version names via properties when [threat_intel] is the only tool invoked. (If the specified project or version does not exist in [bd_product_short], it will be created.)
 
## Invocation
To invoke a [threat_intel] scan, which only executes in "Intelligent" mode, the following must be provided at a minimum in addition to [bd_product_short] Server related configuration properties:   
 ```
--detect.tools=THREAT_INTEL
--detect.threatintel.scan.file.path=<Path to local binary file>
--detect.project.name=<Use existing or set as a value to be created>
--detect.project.version.name=<Use existing or set as a value to be created>
```
 
## Results

[threat_intel] scan findings will appear in the [bd_product_short] user interface under the **Malware** tab. Further information on viewing [threat_intel] results is available [here](https://documentation.blackduck.com/bundle/bd-hub/page/ComponentDiscovery/aboutReversinglabsScanning.html).

## Further information
For additional information regarding the related properties, see [threat-intel](../properties/configuration/threat-intel.md).
