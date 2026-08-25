# Running the [detect_product_short] script

The primary function of the [detect_product_short] scripts is to download and execute the [detect_product_short] .jar file.
Several aspects of script functionality can be configured, including:

* The [detect_product_short] version to download/run; by default, the latest version.
* The download location.
* Where to find Java.

Information on how to configure the scripts is in [Shell script configuration](../../scripts/overview.md).

## Important Information

[var_company_short] recommends using version-specific [detect_product_short] scripts, such as `detect11.sh`, `detect11.ps1`, `detect12.sh`, or `detect12.ps1`, in production environments.

The generic `detect.sh` and `detect.ps1` scripts download and run the latest available [detect_product_short] release by default. As new releases become available, these scripts can automatically upgrade your environment to a newer [detect_product_short] version, including a new major version.

Major-version upgrades can introduce breaking changes. For example, support for specific Java versions may be added or removed, deprecated functionality may be eliminated, and existing behavior may change. To ensure predictable and repeatable execution, use a version-specific script and explicitly control the [detect_product_short] version that is run.

## Running the script on Linux or Mac

<note type="important">The `detect.sh` script downloads and runs the latest available [detect_product_short] release. As new versions become available, this command may begin running a newer major version of [detect_product_short] that includes breaking changes. For production environments, [var_company_short] recommends using a version-specific script such as `detect11.sh` or `detect12.sh`.</note>

On Linux or Mac, execute the [detect_product_short] script ([bash_script_name], which is a Bash script) from Bash.

To download and run the latest version of [detect_product_short] in a single command:

````
bash <(curl -s -L https://detect.blackduck.com/detect.sh)
````

Append any command line arguments to the end, separated by spaces. For example:

````
bash <(curl -s -L https://detect.blackduck.com/detect.sh) --blackduck.url=https://blackduck.mydomain.com --blackduck.api.token=myaccesstoken
````

See [Quoting and escaping shell script arguments](../../scripts/script-escaping-special-characters.md) for details about quoting and escaping arguments.

### To run a specific version of [detect_product_short]:

````
export DETECT_LATEST_RELEASE_VERSION={Detect version}
bash <(curl -s -L https://detect.blackduck.com/detect12.sh)
````

For example, to run [detect_product_short] version 11.5.1:

````
export DETECT_LATEST_RELEASE_VERSION=11.5.1
bash <(curl -s -L https://detect.blackduck.com/detect11.sh)
````

## Running the script on Windows

On Windows, you can execute the [detect_product_short] script ([powershell_script_name], which is a PowerShell script),   
from [Command Prompt](https://en.wikipedia.org/wiki/Cmd.exe) or from inside a PowerShell session. 

### Running from Windows Command Prompt

<note type="important">The `detect.ps1` script downloads and runs the latest available [detect_product_short] release. As new versions become available, this command may begin running a newer major version of [detect_product_short] that includes breaking changes. For production environments, [var_company_short] recommends using a version-specific script and explicitly controlling the [detect_product_short] version that is executed.</note>

To download and run the latest version of [detect_product_short] in a single command from Command Prompt:

````
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect.ps1?$(Get-Random) | iex; detect"
````

Append any command line arguments to the end, separated by spaces. For example:

````
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect.ps1?$(Get-Random) | iex; detect" --blackduck.url=https://blackduck.mydomain.com --blackduck.api.token=myaccesstoken
````

See [Quoting and escaping shell script arguments](../../scripts/script-escaping-special-characters.md) for details about quoting and escaping arguments.

#### To run a specific version of [detect_product_short] from Command Prompt:

Using a version-specific script is the recommended approach for production deployments because it helps prevent unintended upgrades to newer major versions of [detect_product_short].

````
set DETECT_LATEST_RELEASE_VERSION={Detect version}
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect12.ps1?$(Get-Random) | iex; detect"
````

For example, to run [detect_product_short] version 12.0.0:

````
set DETECT_LATEST_RELEASE_VERSION=12.0.0
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect12.ps1?$(Get-Random) | iex; detect"
````

### Running from Windows Powershell

To download and run the latest version of [detect_product_short] in a single command from PowerShell:
````
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; irm https://detect.blackduck.com/detect12.ps1?$(Get-Random) | iex; detect
````

<note type="note">When running the above command, the PowerShell session is not exited. See [here](../../scripts/script-escaping-special-characters.md) for more information on the difference between the two commands.</note>

Append any command line arguments to the end, separated by spaces.

See [Quoting and escaping shell script arguments](../../scripts/script-escaping-special-characters.md) for details about quoting and escaping arguments.

#### To run a specific version of [detect_product_short] from Powershell:

Using a version-specific script is the recommended approach for production deployments because it helps prevent unintended upgrades to newer major versions of [detect_product_short].

````
$Env:DETECT_LATEST_RELEASE_VERSION = "{Detect version}"
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; irm https://detect.blackduck.com/detect11.ps1?$(Get-Random) | iex; detect
````

Or:

````
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; $Env:DETECT_LATEST_RELEASE_VERSION = "{Detect version}"; irm https://detect.blackduck.com/detect11.ps1?$(Get-Random) | iex; detect
````


For example, to run [detect_product_short] version 11.0.0:

````
$Env:DETECT_LATEST_RELEASE_VERSION = "11.0.0"
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; irm https://detect.blackduck.com/detect11.ps1?$(Get-Random) | iex; detect
````

Or:

````
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; $Env:DETECT_LATEST_RELEASE_VERSION="11.0.0"; irm https://detect.blackduck.com/detect11.ps1?$(Get-Random) | iex; detect
````
