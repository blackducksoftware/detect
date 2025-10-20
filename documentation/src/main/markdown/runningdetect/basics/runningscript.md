# Running the [detect_product_long] script

The primary function of the [detect_product_long] scripts is to download and execute the [detect_product_short] .jar file.
Several aspects of script functionality can be configured, including:

* The [detect_product_short] version to download/run; by default, the latest version.
* The download location.
* Where to find Java.

Information on how to configure the scripts is in [Shell script configuration](../../scripts/overview.md).

## Running the script on Linux or Mac

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
bash <(curl -s -L https://detect.blackduck.com/detect11.sh)
````

For example, to run [detect_product_short] version 9.10.0:

````
export DETECT_LATEST_RELEASE_VERSION=9.10.0
bash <(curl -s -L https://detect.blackduck.com/detect9.sh)
````

## Running the script on Windows

On Windows, you can execute the [detect_product_short] script ([powershell_script_name], which is a PowerShell script),   
from [Command Prompt](https://en.wikipedia.org/wiki/Cmd.exe) or from inside a PowerShell session. 

### Running from Windows Command Prompt

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

````
set DETECT_LATEST_RELEASE_VERSION={Detect version}
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect11.ps1?$(Get-Random) | iex; detect"
````

For example, to run [detect_product_short] version 11.0.0:

````
set DETECT_LATEST_RELEASE_VERSION=11.0.0
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect11.ps1?$(Get-Random) | iex; detect"
````

### Running from Windows Powershell

To download and run the latest version of [detect_product_short] in a single command from PowerShell:
````
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; irm https://detect.blackduck.com/detect10.ps1?$(Get-Random) | iex; detect
````

_Note that when running the above command, the PowerShell session is not exited. See [here](../../scripts/script-escaping-special-characters.md) for more information on the difference between the two commands._

Append any command line arguments to the end, separated by spaces.

See [Quoting and escaping shell script arguments](../../scripts/script-escaping-special-characters.md) for details about quoting and escaping arguments.

#### To run a specific version of [detect_product_short] from Powershell:

````
$Env:DETECT_LATEST_RELEASE_VERSION = "{Detect version}"
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; irm https://detect.blackduck.com/detect10.ps1?$(Get-Random) | iex; detect
````

Or:

````
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; $Env:DETECT_LATEST_RELEASE_VERSION = "{Detect version}"; irm https://detect.blackduck.com/detect10.ps1?$(Get-Random) | iex; detect
````


For example, to run [detect_product_short] version 9.0.0:

````
$Env:DETECT_LATEST_RELEASE_VERSION = "9.0.0"
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; irm https://detect.blackduck.com/detect9.ps1?$(Get-Random) | iex; detect
````

Or:

````
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; $Env:DETECT_LATEST_RELEASE_VERSION="9.0.0"; irm https://detect.blackduck.com/detect9.ps1?$(Get-Random) | iex; detect
````

