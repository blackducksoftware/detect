# Air Gap Mode

To run [detect_product_long] on an air-gapped computer or network, you must first download and install [detect_product_short] and dependencies that [detect_product_short] normally downloads as it runs. These include inspectors for Docker, NuGet and other files. These files are packaged together in an air gap archive that will be extracted on the target system.

## Downloading or creating an air gap archive

Air gap archives are available for download from the location specified in [download locations](downloadlocations.md).
These air gap archives contain the versions of the dependencies that were current at the time of the [detect_product_short] release. 

As an alternative, you can create an air gap archive yourself.
An air gap archive that you create will contain the versions of the dependencies that are current at the time you create the air gap archive
(the same versions [detect_product_short] would download if run at that time).

To create an air gap archive, run [detect_product_short] with the
-z or --zip command line option. 
Optionally you can follow --zip with a space and an argument (for example: --zip FULL) to customize the air gap zip. 
  
Possible values:   
* FULL (produce a full air gap zip; the default)   
* NO_DOCKER (do not include the Docker Inspector)    

## Running in air gap mode

For information refer to [Running in air gap mode](../runningdetect/runningairgap.md).
