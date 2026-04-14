# Script

The primary function of the [detect_product_short] script is to download and execute the [detect_product_short] JAR file, which enables the scan capability.

Users download and run the latest version of [detect_product_short] by providing the following commands, and adding properties to refine the behaviour.

Windows:
````
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect.ps1?$(Get-Random) | iex; detect"
````

Linux/MacOs:
````
bash <(curl -s https://detect.blackduck.com/detect.sh)
````

<note type="note">Running the unversioned `detect.sh/ps1` script will use the latest version of the [detect_product_short] .jar file, whereas running a versioned script such as `detect11.sh/ps1` will use the latest version of the [detect_product_short] .jar file within that specific major version.</note>  
