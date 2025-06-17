# Requirements and release information

## General requirements

* Typically, access to the internet is required to download and run [detect_product_short] and components from GitHub and other locations. For running without internet access,
refer to [Air Gap Mode](../downloadingandinstalling/airgap.md).
* Minimum 8GB RAM.
* Java: OpenJDK 64-bit version 8, 11, 13, 14, 15, 16, 17, or 21. If using Java 11: 11.0.5 or higher is required.
* Minimum curl version 7.34.0, recommended 8.4.0 or later.
* Bash.
* If using [powershell_script_name]: PowerShell versions 4.0 or higher.
* The tools required to build your project source code.

## [bd_product_long] integration requirements

* Licensed installation of the current version of [bd_product_short] with access credentials.
Visit the [Black Duck release page](https://github.com/blackducksoftware/hub/releases) to determine the current version of [bd_product_short].
* For information about additional compatible versions of [bd_product_short], consult the
<xref href="Black-Duck-Release-Compatibility.dita" scope="peer"> [bd_product_short] Release Compatibility.<data name="facets" value="pubname=blackduck-compatibility"/>
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

* For information about support and service durations for [detect_product_short] versions, consult the
<xref href="Support-and-Service-Schedule.dita" scope="peer"> Support and Service Schedule.<data name="facets" value="pubname=blackduck-compatibility"/>
