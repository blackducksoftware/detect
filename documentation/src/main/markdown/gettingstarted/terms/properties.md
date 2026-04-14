# [detect_product_short] Properties

Properties in [detect_product_long] provide information used to determine how and what actions [detect_product_short] takes during a scanning run. A property to which you assign a value is like a flag or a parameter on the command line or in a script that provides instructions for the [detect_product_short] scan task.

When setting a property value, the property name is prefixed with two hyphens (--). 

````
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) <--property=value>
````

Example using properties to specify project name and [bd_product_short] URL:

````
bash <(curl -s -L https://detect.blackduck.com/detect11.sh) --detect.project.name=MyProject --blackduck.url=https://blackduck.yourdomain.com
````

<note type="note">When configuring [detect_product_short] via environment variables or configuration file, specific property handling applies. See [Using environment variables](../../configuring/envvars.md) or [Using a configuration file](../../configuring/configfile.md).</note>
