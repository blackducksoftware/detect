# Running the [detect_product_long] .jar

Recent versions of the [detect_product_short] .jar file are available for download from the location specified in [download locations](../../downloadingandinstalling/downloadlocations.md).

To run [detect_product_short] by invoking the .jar file:

````
java -jar {path to .jar file}
````

For example:

````
curl -O https://sig-repo.synopsys.com/bds-integrations-release/com/blackduck/integration/detect/10.0.0/detect-10.0.0.jar
java -jar synopsys-detect-10.0.0.jar
````

You can use the [detect_product_short] Bash script ([bash_script_name]) to download the [detect_product_short] .jar file:

````
export DETECT_DOWNLOAD_ONLY=1
./detect9.sh
````
