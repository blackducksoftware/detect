# [detect_product_long] requirements and release information

## General requirements
     
* Minimum 8GB RAM.   
* Java: OpenJDK 64-bit version 8, 11, 13, 14, 15, 16, 17, or 21. If using Java 11: 11.0.5 or higher is required.   
* Minimum curl version 7.34.0, recommended 8.4.0 or later.   
* Bash.   
* If using [powershell_script_name]: PowerShell versions 4.0 or higher.   
* The tools required to build your project source code.   

## Network requirements and information

<note type="note">Unless you are running [detect_product_short] in Air Gap mode, access to the internet is required to download and run [detect_product_short] and related components from GitHub and other locations.</note>

* [detect_product_short] script downloads should only be accessed via detect.blackduck.com.
* [detect_product_short] 10.0.0 and later will only work when using repo.blackduck.com. 

<note type="tip">Configure repo.blackduck.com on network allow lists to ensure connectivity for any scripts, services, or pipelines requiring access.</note>

* [bd_product_long] [SCA Scan Service (SCASS)](https://community.blackduck.com/s/question/0D5Uh00000O2ZSYKA3/black-duck-sca-new-ip-address-requirements-for-2025) requires customers add or update IP addresses configured in their network firewalls or allow lists. This action is required to successfully route scan data for processing.

	* scass.blackduck.com - 35.244.200.22
	* na.scass.blackduck.com - 35.244.200.22
	* na.store.scass.blackduck.com - 34.54.95.139
	* eu.store.scass.blackduck.com - 34.54.213.11
	* eu.scass.blackduck.com - 34.54.38.252

* Collection of phone home metrics requires the following IP address to be allowlisted:
	* static-content.app.blackduck.com - 34.117.80.109
	
## Running [detect_product_short] in a container

[var_company_long] publishes [detect_product_short] Docker images which can be used to run [detect_product_short] from within a Docker container. Refer to [Running Detect from within a Docker container](../runningdetect/runincontainer.md) for details.

## Running [detect_product_short] in an Air Gap environment

* To run [detect_product_short] without internet access, refer to [Air Gap Mode](../downloadingandinstalling/airgap.md).

## [bd_product_short] integration requirements

* Licensed installation of the current version of [bd_product_short] with access credentials.
Visit the [Black Duck release page](https://github.com/blackducksoftware/hub/releases) to determine the current version of [bd_product_short].
* For information regarding compatible versions of [bd_product_short], consult the [bd_product_short] [Release Compatibility page](https://documentation.blackduck.com/bundle/blackduck-compatibility/page/topics/Black-Duck-Release-Compatibility.html)
* The [bd_product_short] notifications module must be enabled.
* A [bd_product_short] user with the [required roles](usersandroles.md).
* On Alpine Linux you will also need to override the Java installation used by the [blackduck_signature_scanner_name] as
described [here](../troubleshooting/solutions.md#ariaid-title29).

## Project type-specific requirements

In general, the detectors require:

* All dependencies must be resolvable. This generally means that each dependency has been installed using the package manager's cache, virtual environment, and others.
* The package manager / build tool must be installed and in the path.

Refer to the applicable [package manager sections](../packagemgrs/overview.md) for information on specific detectors. 
<note type="important">Review requirements for [Docker Inspector](../packagemgrs/docker/intro.md) and [NuGet Inspector](../packagemgrs/nuget.md).</note>

## Risk report requirements

The risk report requires that the following fonts are installed:

* Helvetica
* Helvetica bold

## Supported [detect_product_short] versions and Service duration

For information about support and service durations for [detect_product_short] versions, consult the
<xref href="Support-and-Service-Schedule.dita" scope="peer"> Support and Service Schedule.<data name="facets" value="pubname=blackduck-compatibility"/>
