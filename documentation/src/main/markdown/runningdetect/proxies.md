# Running behind a proxy

When running behind a proxy:

1. The one-liner cannot be used to download the scripts, they are not proxy aware. The scripts must already be downloaded.
2. The script ([bash_script_name] or [powershell_script_name]) requires proxy details to do a version
check on, and/or download the [detect_product_short] .jar file.
3. [detect_product_short]; the code in the .jar file, requires proxy details to download inspectors and
connect to [bd_product_short].

## Providing proxy details to [detect_product_short]

[detect_product_short] looks for proxy details in the properties whose names start with `blackduck.proxy`,
including:

* `blackduck.proxy.host` (proxy hostname)
* `blackduck.proxy.port` (proxy port number)
* `blackduck.proxy.username` (proxy username)
* `blackduck.proxy.password` (proxy password)

When setting the blackduck.proxy.host (proxy hostname) property, the schema/protocol is not accepted.   

For example:  
 
	Correct: `--blackduck.proxy.host=<Proxy_IP/URL>`   
	Incorrect: `--blackduck.proxy.host=<https‎ ://(IP/Server_URL)>`   
	

Refer to [properties](../properties/configuration/proxy.md) for more information.

## Providing proxy details to [bash_script_name]

The curl commands executed by [bash_script_name] to do a version check on, and/or download the [detect_product_short]
.jar file, require additional command line options when run behind a proxy. For more information
on curl options, refer to the [curl documentation](https://curl.haxx.se/docs/manpage.html).

To provide additional curl command line options for [bash_script_name] to use
when it executes curl, set the environment variable DETECT_CURL_OPTS before running
[bash_script_name]. For example:

    export DETECT_CURL_OPTS=--proxy http://myproxy:3128
    ./[bash_script_name]

When using [bash_script_name] to execute [detect_product_short] you must set proxy properties
for [detect_product_short] as previously described.

## Providing proxy details to [powershell_script_name]

[powershell_script_name] derives proxy details from environment variables
whose names match the [detect_product_short] proxy property names.
Configuring [powershell_script_name] for your proxy involves
setting those environment variables before running [powershell_script_name].
Note that typically, the PowerShell script is run from a Command window, using "powershell script.ps1" so these should be run in that Command window.
For example:

    ${r"set BLACKDUCK_PROXY_HOST"}=$ProxyHost
    ${r"set BLACKDUCK_PROXY_PORT"}=$ProxyPort
    ${r"set BLACKDUCK_PROXY_PASSWORD"}=$ProxyUsername
    ${r"set BLACKDUCK_PROXY_USERNAME"}=$ProxyPassword
    powershell "Import-Module FULL_PATH_TO_DOWNLOADED_SCRIPT/detect.ps1; detect"

For additional information on these properties, including alternate key formats, see the [Shell script configuration reference](../scripts/overview.md).

When using [powershell_script_name] to execute [detect_product_short], [detect_product_short] also receives the proxy details
from these environment variables, so no additional configuration is required for [detect_product_short].


