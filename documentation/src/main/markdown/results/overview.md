# Viewing and managing [detect_product_short] scan results

## Online mode

To view and manage your [detect_product_long] scan results after running [detect_product_short] online, do the following.

- In the [detect_product_short] output look for "Detect Result" and copy the [bd_product_short] Project BOM URL as shown in the following example:

````
2020-06-11 06:35:39 INFO [main] ---======== Detect Result ========
2020-06-11 06:35:39 INFO [main] --- Black Duck SCA Project BOM: https://my-hub-docker/api/projects/d8f798f1-1901-4902-aec7-f2e1cf2e4958/versions/6a8938e9-3615-40dd-8386-3bcb4ba52bec/components
````
- Open the [bd_product_short] Project BOM URL in a browser to view the scan results in [bd_product_short].
- To find your scan in [bd_product_short], go to your [bd_product_short] instance and click Scans to see a list of scans on the Scans page.

For help with viewing and analyzing your scan results go to the [bd_product_short] Help page navigation menu at https://\<Your hub host\>/doc/Welcome.htm

## Offline mode

To view and manage your [detect_product_short] scan results after running [detect_product_short] offline (with property *blackduck.offline.mode* set to *true*), do the following.

- In the [detect_product_short] output (near the beginning), look for the value of "Run directory". The output files will be written into subdirectories of the run directory. For example:

````
2022-03-07 15:46:29 EST INFO  [main] --- Run directory: /Users/billings/blackduck/runs/2022-03-07-20-46-29-611
````

Upload each of the output files (.bdio and .bdmu files, found in subdirectories of the run directory) into [bd_product_short] using the [bd_product_short] Scans page.
