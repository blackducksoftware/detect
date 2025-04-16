# Black Duck Client

## Using the Image

blackduck-client is a wrapper of Black Duck Detect, in which an air-gap `.zip` file (containing the latest version of Detect) is stored under the `/opt/` directory.

While running this image as a container, extract the `.zip` file onto your local machine for use. Note that this image is not intended to be used as a persistently running container, nor for running Detect.

This image can be pulled by running either of the following Docker commands:
- `docker pull blackducksoftware/blackduck-client:{tag}`
- `docker pull registry1.dso.mil/ironbank/blackduck/blackduck/blackduck-client:{tag}`

Tag example: `9.3.0_ubi8.8`

## Documentation
 
For details on how to run Detect, please see our documentation below: 
- [Introduction to Black Duck Detect](https://documentation.blackduck.com/bundle/detect/page/introduction.html)
- [Running Black Duck Detect in air gap mode](https://documentation.blackduck.com/bundle/detect/page/runningdetect/runningairgap.html)

## Example of running the image and copying the air-gap .zip to an existing local directory

1) The following command runs the blackduck-client image as a container in detached mode with the name "detect", and keeps the container running indefinitely (with the tail command) until it is manually stopped (replace the image tag as needed): `docker run --rm --name detect -d blackducksoftware/blackduck-client:9.3.0_ubi8.8 tail -f /dev/null`

2) Copy the file into an existing directory on your local machine (replace the version as needed): 
`docker cp detect:/opt/blackduck-detect-9.3.0-air-gap.zip /tmp/detect-zip`

3) Stop the running container: `docker stop detect`
