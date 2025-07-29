# [detect_product_short] architecture overview

[docker_inspector_name] uses up to three container-based image inspector services; 
one for each of the supported Linux package manager database formats.

The three image inspector services provide coverage of the three package manager database formats: DPKG, RPM, and APK.
By default, [docker_inspector_name] submits its request to inspect the target image to the DPKG (Ubuntu) image inspector service. All services 
redirect to the appropriate image inspector service if it cannot handle the request. For example,
if the target image is a Red Hat image, the Ubuntu inspector service, which cannot inspect a Red Hat image, 
redirects to the CentOS inspector
service, which can inspect a Red Hat image. If you know
that most of your images have either RPM or APK databases, you can improve performance by configuring
[docker_inspector_name] to send requests to the CentOS (RPM) or Alpine (APK) image inspector service using
the [docker_inspector_name] property *imageinspector.service.distro.default*.

In host mode (the default), [docker_inspector_name] automatically uses the Docker engine to pull as
needed from Docker Hub
the following three images: [image_repo_organization]/[imageinspector_image_name_base]-alpine, 
[image_repo_organization]/[imageinspector_image_name_base]-centos, and [image_repo_organization]/[imageinspector_image_name_base]-ubuntu.
[docker_inspector_name] starts those services as needed,
and stops and removes the containers when [docker_inspector_name] exits. It uses a shared volume to share files, such as the target Docker image,
between the [docker_inspector_name] utility and the three service containers.

In container mode, start the container running [docker_inspector_name] and the three image inspector container-based services such that
all four containers share a mounted volume and can communicate with each other using HTTP GET operations using base URLs that you provide.
For more information, refer to [Deploying](deployment.md).

## Execution modes

### Host mode

In host mode, [docker_inspector_name] performs the following steps on the host:

1. Pulls and saves the target image to a .tar file if you passed the image by *repo:tag*.
2. Checks to see if the default image inspector service is running. If not, it pulls the inspector image and
starts a container, mounting a shared volume.
3. Requests the Black Duck input/output (BDIO) file and container file system by sending an HTTP GET request to the image inspector service.

The following steps are performed inside the image inspector container:

1. Builds the container file system that a container would have if you ran the target image. It does not run the target image.
2. Determines the target image package manager database format, and redirects to a different image inspector service if necessary.
3. Runs the image inspector's Linux package manager on the target image package manager database to get details of
installed packages.
4. Produces and returns a BDIO1 (.jsonld) file consisting of a graph of target image packages and, optionally, the container filesystem.

The following steps are performed back on the host when the request to the image inspector service returns:

1. Returns the output files (BDIO and signature and binary scan targets) to [detect_product_short] by copying them to the output directory.
1. Stops/removes the image inspector container.  Note that this can be disabled.

### Container mode

In container mode, you start four containers in such a way that they share a mounted volume and can reach each other through HTTP GET operations using
base URLs that you provide:

* One container for [detect_product_short] / [docker_inspector_name].
* One container for each of the three image inspector services: Alpine, CentOS, and Ubuntu.

In container mode you must provide the target image in a .tar file with one of the supported formats; you cannot specify that target image by repo:tag.

[detect_product_short] invokes [docker_inspector_name], which
requests the dependency graph (in BDIO format) and signature/binary scan targets using HTTP from the default image inspector service using a 
base URL that you have provided.

The following steps are performed inside the image inspector container:

1. Builds the container file system that a container would have if you ran the target image. It does not run the target image.
1. Determines the target image package manager database format, and redirects to a different image inspector service if necessary.
1. Runs the image inspector's Linux package manager on the target image package manager database to get details of the installed packages.
1. Produces and returns a BDIO1 (.jsonld) file consisting of a graph of target image packages and, optionally, the container filesystem.

The following steps are performed by [docker_inspector_name]/[detect_product_short] back in the [detect_product_short] container when the request to the image inspector service returns:

1. [docker_inspector_name] returns the output files (BDIO and signature and binary scan targets) to [detect_product_short] by copying them to the output directory.
1. [detect_product_short] converts the BDIO to BDIO2, adjusts the project, project version, and codelocation names, and uploads it to [bd_product_short].
1. [detect_product_short] performs [blackduck_signature_scan_act] and [bdba_product_short] on the scan targets.

#### Deploying in container mode

Deploying in container mode is challenging and requires expertise in the container platform on which you will deploy.
We recommend engaging [professional_services] for a solution tailored to your environment. 

You can find several container mode deployment examples on the [deployment page](deployment.md).
