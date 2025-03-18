# Supported image formats

Images passed to [detect_product_short] via the *detect.docker.image* property must either be pullable using the machine's docker engine (via the equivalent of a "docker pull" command) or already exist in the local docker cache. [detect_product_short] will save these to a file using the equivalent of a "docker save" command.

Images passed to [detect_product_short] via the *detect.docker.image.id* property must already exist in the local docker cache. [detect_product_short] will save these to a file using the equivalent of a "docker save" command.

Image files passed to [detect_product_short] via the *detect.docker.tar* property must be .tar files, and the contents must conform to either of the following image format specifications: 
1. [Docker Image Specification v1.2.0](https://github.com/moby/moby/blob/master/image/spec/v1.2.md) (the format produced by the "docker save" command)
2. [Open Container Initiative Image (OCI) Format Specification](https://github.com/opencontainers/image-spec/blob/main/spec.md).

The base layer OS package manager invocation and resolution of installed packages by [docker_inspector_name] is restricted to Ubuntu, CentOS, and Alpine base OS layer images. If the image meets other requirements and regardless of the base layer OS, [docker_inspector_name] will run a signature scan/analysis on the tarball of the image and produce matches if any. For example, if the base layer OS is CentOS, [docker_inspector_name] will start CentOS image inspector container service, mount the image onto this container and run "rpm qa" to get a list of installed packages if available. 
 
Should unresolvable errors occur during attempts to scan Docker images we recommend switching to analysis via either [Container Scan](../../runningdetect/containerscanning.md) or [Binary scanning](../../properties/configuration/binary-scanner.md) on Docker images.