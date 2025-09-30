# Running with [bd_product_long]

[detect_product_long] can be used with multiple [bd_product_short] platforms to perform Software Composition Analysis (SCA).

## Overview

When running [detect_product_short] with [bd_product_short] and connection details are provided, [detect_product_short] executes all eligible detection tools by default, including the following:

* The [detector tool](../../components/detectors.dita), which runs the appropriate package manager-specific detector; the Maven detector
for Maven projects, the Gradle detector for Gradle projects, and so forth.
* The [Black Duck Signature Scanner](../../properties/configuration/signature-scanner.md), which performs a [blackduck_signature_scan_act] on the
project directory.
* Run [Black Duck Binary Analysis](../../properties/configuration/binary-scanner.md) on given binary files.

[detect_product_short] can be configured to perform additional tasks, including the following:

* Enable any of the supported snippet matching modes in the [Black Duck Signature Scanner](../../properties/configuration/signature-scanner.md).
* Enable the [Vulnerability Impact Analysis Tool](../../properties/configuration/impact-analysis.md#vulnerability-impact-analysis-enabled) on any Java project.
* Run the [dockerinspector_name] on a given [Docker image](../../packagemgrs/docker/intro.md).
* Generate a [report](../../properties/configuration/report.md).
* Fail on [policy violation](../../properties/configuration/project.md#fail-on-policy-violation-severities-advanced).
* Run [IaC Scan](../iacscan.md) on provided targets. Note: Iac Scan capabilities require [bd_product_short] 2022.7.0 or later.

Refer to [Black Duck SCA Server properties](../../properties/configuration/blackduck-server.md), [Black Duck Signature Scanner properties](../../properties/configuration/signature-scanner.md), and [IaC Scan](../iacscan.md) for details.

<note type="tip">Available signature scanner properties can be determined by specifying `--help` when executing the signature scanner jar file from the command line.</note>

## Offline mode

If you do not have a [bd_product_short] instance, or if your network is down, you can still run [detect_product_short] in offline mode.
<note type="note">Offline mode is not the same as air gap mode. Air gap mode requires the airgap.jar available to execute as it contains local copies of scanning libraries to support full offline execution.</note>
In offline mode, [detect_product_short] writes output files (.bdio files and, when Vulnerability Impact Analysis runs, .bdmu files) to subdirectories
within the run directory without attempting to upload them to [bd_product_short]. You can find the value of the run directory in the [detect_product_short] log.
You can run [detect_product_short] in offline mode using the [offline mode property](../../properties/configuration/blackduck-server.md#offline-mode).

### Running in offline mode

Download the latest [detect_product_short] version:
 [See download locations](../../downloadingandinstalling/downloadlocations.md)
 
*Choose one of the following to download.*

**Version 10.0.0 or later**   
`detect-X.X.X-air-gap.zip` includes scanning for Gradle, Nuget, and Docker.
`detect-X.X.X-air-gap-no-docker.zip` includes scanning for Gradle and Nuget. No Docker scanning.

**Version 8.7 to 9.10.0**   
`synopsys-detect-X.X.X-air-gap.zip` includes scanning for Gradle, Nuget, and Docker.
`synopsys-detect-X.X.X-air-gap-no-docker.zip` includes scanning for Gradle and Nuget. No Docker scanning.

Download the Signature Scanner from your [bd_product_short] server:
https://{blackduckserver}/download/scan.cli.zip
https://{blackduckserver}/download/scan.cli-windows.zip
https://{blackduckserver}/download/scan.cli-macosx.zip

Specify the following [detect_product_short] scan properties:
* --blackduck.offline.mode=true
* --detect.scan.output.path= output of the Signature Scanner
* --detect.output.path= output directory to store files that [detect_product_short] downloads or creates
* --detect.blackduck.signature.scanner.local.path= location to the signature scanner scan.cli-202x.xx.x

<note type="note">If using air gap zip archive files for [detect_product_short] scanning, [see Air Gap Mode](../../downloadingandinstalling/airgap.md) for additional information.</note>

[detect_product_short] Scan Command example:
```
java -jar  detect-x.x.x.jar --blackduck.url= --blackduck.api.token= --detect.project.name= --detect.project.version.name= --blackduck.offline.mode=true --detect.scan.output.path= --detect.output.path= --detect.blackduck.signature.scanner.local.path=
```
      
Upload Scan results via the [bd_product_short] UI:

The scan files to upload to [bd_product_short] are found in the output [bd_product_short] directory. There will be a scan file for the Signature Scanner and Dependency Scanner. Look at console output to check if both scanners ran. It is possible one scanner ran, but the other did not.

The following are the locations of the scan files if the following [detect_product_short] properties were used:

* scan.output.path (ends in .json): {the path provided}\BlackDuckScanOutput\{date and time of scan}\data
* output.path (ends in *.bdio): {the path provided}\runs\{date and time of scan}\bdio

[bd_product_short] UI upload

- Go to **Scans** in the left navigation bar
- Click **+Upload** Scans
- Add scan files

## BDIO format

[detect_product_short] produces dependency information for [bd_product_short], and other products and platforms, in [bd_product_short] Input Output (BDIO) format files.
[detect_product_short] supports generating BDIO version 2 documents.
