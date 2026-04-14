# Advanced [detect_product_short] topics

This section covers a variety of advanced topics.

## How [docker_inspector_name] discovers dependencies

[docker_inspector_name] discovers dependencies in the target image by making a request to
an image inspector service (running inside a container).
The image inspector service works as follows:

1. Reads the target image, and constructs the file system that the container would have at
time zero if you were to run the image.
2. Finds the linux package manager database in that file system.
3. Runs its own linux package manager (invoking its "list" function: "dpkg -l", "rpm -qa --qf ...", or "apk info -v") on the package manager
database from the target image
to generate the list of packages installed in the target image.
If the package manager database type of the target image does not match
the image inspector's package manager type, the image inspector will redirect
the request
to the image inspector that can process the target image (which starts
again at step 1).
4. Translates that package list to BDIO and uploads it to [bd_product_short], or returns it to the the
caller (e.g. [detect_product_short]) to upload to [bd_product_short].
5. Returns the constructed file system to the caller (e.g. [detect_product_short]) for signature scanning.

The user ID and group ID of the image inspector service process will (in general) be different from the user ID and
group ID of the [docker_inspector_name] process. Consequently, the the environment must be confugred
so that files created by [docker_inspector_name] are readable by all. On Linux, this means an appropriate
umask value (for example 002 or 022). On Windows, the working directory must be readable by all.
In addition, the permissions on a Docker tarfile passed via
the *docker.tar* property must allow read by all.

## Enabling file sharing in Docker

Docker may restrict file sharing to certain directories that you've configured in
Docker > Settings > Resource > File Sharing. To enable [docker_inspector_name] to mount the volumes
that it needs, you may need to enabling file sharing for directory $HOME/blackduck.

## Organizing components by layer

By default [docker_inspector_name] will produce BDIO containing a component graph consisting only of components (linux packages).

Alternatively, you can direct [docker_inspector_name] to organize components by image layer
by setting property `bdio.organize.components.by.layer=true`.
Run this way, [docker_inspector_name] will produce BDIO containing image layers
at the top level of the graph, and components associated with each layer appearing as children of that layer.
This structure is visible in from the [bd_product_short] project version Source display.

A side effect of this components-under-layers graph structure is
the categorization by [bd_product_short] of all components as Transitive.

In the BDIO, layers are named `Layer{index}_{layer digest}`, where `{index}` is a two digit index starting at 00 to indicate layer ordering
within the image, and `{layer digest}` is the layer digest with ":" replaced with "_".
For example, the first layer of an image could be named:
`Layer00_sha256_1bcfbfaf95f95ea8a28711c83085dbbeceefa11576e1c889304aa5bacbaa6ac2`.

This feature produces BDIO in which the same component may appear at multiple points in the graph, so
only [bd_product_short] versions 2021.8.0 and newer have the ability to correctly display graphs organized by layer, if *Admin > System Settings > Scan > Component Dependency Duplication Sensitivity* is set high enough to avoid removal of components
that appear multiple times in the graph (minimum: 2).

When organizing components by layer, you must also choose whether or not to include removed (whited-out) components in the output.

### Including removed components

If you include removed components, [docker_inspector_name] will produce BDIO containing a graph with image layers
at the top level.  Under each image layer it will include all components present after the layer was
applied to the file system (including components added by lower layers that are still present). If a component is added by a lower layer
but later removed (whited-out) by a higher layer, it will not
appear under the layer that removed it or any higher layer (unless/until it is re-added by another layer).

The benefit of using this mode: for every component added by any layer, you can see where it was added, and where it was removed (if it was).

The downside of using this mode: components added by a lower layer but removed by a higher layer will appear in the BOM, even though they are not present in the final container filesystem.

To include removed components, set property `bdio.include.removed.components=true`.

### Excluding removed components

If you exclude removed components (the default), [docker_inspector_name] will behave as described in the section above
except that components not present in the final container filesystem will not appear on any layer in the BDIO graph.

The benefit of using this mode: components added by a lower layer but removed by a higher layer
(and therefore not present in the final container filesystem) will not appear in the BOM.

The downside of using this mode: if a component is added by a lower layer and removed by a higher layer, there is no evidence of that in the BDIO graph or the BOM.

To exclude removed components, set property `bdio.include.removed.components=false` (the default).

## Isolating application components

If you are interested in components from the application layers of your image, but not interested in components
from the underlying platform layers, you can exclude components from platform layers from your results.

For example, if you build your application on *ubuntu:latest* (your Dockerfile starts
with FROM ubuntu:latest), you can exclude components from the Ubuntu layer(s) so that
the components generated by [docker_inspector_name] contain only components from your application layers.

### Limitations to this feature

This feature works when the method you use to build your image adds layers
on top of a base or platform image (single-stage builds).
Here's an example of a Dockerfile that adds layers on top of a base image. This feature will work
for images built this way:

    FROM ubuntu:latest
    RUN apt-get update && apt-get -y install curl

The resulting image will include all layers from ubuntu:latest, plus an additional layer
that adds the curl package. This feature is designed for this type of image, and will
be able to isolate the layers added to ubuntu:latest (which in this case is a single
layer that adds curl).

On the other hand, when you build an image using multiple FROM statements in the Dockerfile
(multi-stage builds),
the layers in the resulting image are not, in general, the layers of the first image
followed by the layers of the second image. You can verify this by comparing
the *RootFS.Layers* list of the resulting image to the *RootFS.Layers* lists of the images
named in the FROM statements. (See below for instructions on how to get the RootFS.Layers list
using the *docker inspect* command).

Here's an example of a Dockerfile that uses
multiple FROM statements. This feature will not, in general, work for images built this way:

    FROM ubuntu:latest
    FROM myapplicationimage:mytag
    
### How to use this feature

First, find the layer ID of the platform's top layer using the following process.

1. Run the *docker inspect* command on the platform image. In this example, the platform image is ubuntu:latest,
so you would run `docker inspect ubuntu:latest`.
1. Find the last element in the *RootFS.Layers* array. This is the platform top layer ID. In the following example, this is 
sha256:b079b3fa8d1b4b30a71a6e81763ed3da1327abaf0680ed3ed9f00ad1d5de5e7c. (Because the *latest* tag moves frequently,
the top layer ID for ubuntu:latest changes over time.)

Set the value of the [detect_product_short] property *detect.docker.platform.top.layer.id* to the platform top layer ID.
For example:

    {Detect command} ... --docker.platform.top.layer.id=sha256:b079b3fa8d1b4b30a71a6e81763ed3da1327abaf0680ed3ed9f00ad1d5de5e7c

In this mode, the container file system and/or container file system squashed image produced by [docker_inspector_name]
 only contains files added to the image by application layers. If the [bd_product_short] signature scanner is run on this file,
it generates results based only on files found on application layers. This provides the benefit of isolating
application components by excluding platform components. However, there may be some loss in match accuracy from the
signature scanner because in this scenario, the signature scanner may be deprived of some contextual
information, such as the operating system files that enable it to determine the Linux distribution.  
This could negatively affect its ability to accurately identify components.

## Considerations when running on Windows

### Docker file sharing settings

[docker_inspector_name] requires the ability to share directories with the image inspector containers.
It shares a directory with image inspector containers by mounting it as a volume.
You will need to configure your Docker settings to enable this file sharing.
The simplest way to do this is to add your home directory as a sharable directory
on the Docker settings Resources > FILE SHARING screen.

The shared directories are created under the [detect_product_short] output directory
(controlled by [detect_product_short] *detect.output.path*).
If you change the location of the [detect_product_short] output directory, be sure your Docker file sharing settings enable
sharing of that directory.

### Docker restrictions

Docker on Windows has restrictions that impact [docker_inspector_name]:

1. Docker can be configured to pull either Linix images, or Windows images.
You can see how your Docker installation is configured by looking
at the *OSType* value in the output of the *docker info* command.
If Docker is configured for Linix images, it cannot pull Windows images,
and vice versa. The command to change Docker's *OSType* value appears
in the Docker Desktop menu. Refer to Docker documentation for more information.
2. When pulling Windows images, Docker requires (a) that the architecture of the
pulled image matches the architecture of your machine, and (b) that the Windows version
of the pulled image is a close match to the Windows version of your machine.

## Inspecting Windows images on non-Windows systems

Running on a non-Windows system, Docker can neither pull nor build a Windows image.
Consequently, you cannot run [docker_inspector_name] on a Windows Docker image
using the either the *docker.image* or *docker.image.id* property.
Instead, you must, using a Windows system,
pull and save the target image as a .tar file, and pass that .tar file
to [docker_inspector_name] using the *docker.tar* property. 

## Inspecting multiple images more efficiently by leaving services running

By default, [docker_inspector_name] starts, uses, and then stops and removes either one or two containerized
image inspector services per run. This may be appropriate when scanning
a single image, but when scanning many images it is highly inefficient,
and it doesn't support concurrent execution of multiple [docker_inspector_name] runs. 

The scanning of many images is completed significantly faster by starting the image inspector services
once, and running multiple instances of [docker_inspector_name], so that each one sends requests to the already-running
image inspector services.

The following script illustrates how this is done in a Docker environment:

    curl -O [source_raw_content_url_base]/[source_repo_organization]/[project_name]/master/deployment/docker/batchedImageInspection.sh

To keep the example simple, this script only starts the Alpine image inspector service.
In general, you must start two more services: the Ubuntu image inspector service
for inspecting images built from dpkg-based Linux distros, and the CentOS image inspector service
for inspecting images built from rpm-based Linux distributions. It doesn't matter which service receives
the request; any service redirects if necessary.

## Configuring [docker_inspector_name] for your Docker Engine and registry

If you invoke [docker_inspector_name] with an image reference; in other words, a repo:tag value versus a .tar file,
it uses the [docker-java library](https://github.com/docker-java/docker-java), to access the Docker registry to pull the image.

If you can pull an image using *docker pull* from the command line, then you will be able to configure [docker_inspector_name] to pull that image.
The docker-java library can often be configured the same way as the Docker command line utility (*docker*).

There are also other ways to configure docker-java. For more information on configuring docker-java
(and therefore [docker_inspector_name]) for your Docker registry,
refer to the configuration information in the [docker-java documentation](https://github.com/docker-java/docker-java/blob/master/docs/getting_started.md).

For example, one way to configure your Docker registry username and password is to create a file
in your home directory named *.docker-java.properties* that configures username and password:
````
registry.username=mydockerhubusername
registry.password=mydockerhubpassword
````

If you need to override the DOCKER_HOST value, set property *use.platform.default.docker.host* to false.
When *use.platform.default.docker.host* is set to false,
[docker_inspector_name] does not override any of the configuration settings in the code,
so all other docker-java configuration methods such as properties, system properties,
and system environment, are available to you.

When *use.platform.default.docker.host* is set to true (the default value) *and* [docker_inspector_name] is running
on Windows, [docker_inspector_name] overrides only the DOCKER_HOST value
(setting it to "npipe:////./pipe/docker_engine").

## Running [detect_product_short] on a project directory that exists within a Docker image

When you want to run [detect_product_short] on a directory that exists within a Docker image, you can use the following approach:
1. Run [detect_product_short] on the image to generate the container filesystem for the image.
2. Run [detect_product_short] on a directory within that container filesystem.

[detect_product_short] performs these actions without running the image/container.

To see a simple example that illustrates this approach, use the following commands to download these sample files:

    curl -O [source_raw_content_url_base]/[source_repo_organization]/[project_name]/master/deployment/docker/runDetectInImageDir/runDetectInImageDir.sh
    curl -O [source_raw_content_url_base]/[source_repo_organization]/[project_name]/master/deployment/docker/runDetectInImageDir/Dockerfile

Review the script before running it to make sure the side effects
(files and directories that it creates) are acceptable.
You must make the script executable before you run it. 

## Running the signature scanner on a specific directory within a Docker image

To use iScan to scan a specific directory within an image:
 
1. Run [docker_inspector_name] on the target image to get the container file system.
You can also do this using [detect_product_short] using `--detect.docker.passthrough.*` properties.
Include the following [docker_inspector_name] properties:
```
--upload.bdio=false                        # disable BDIO upload
--output.include.containerfilesystem=true  # tell DI to output the container file system
--output.path={your output dir}            # tell DI where to put the output
```
2. Locate the container file system in the output directory (*.tar.gz) and untar it.
3. cd into the directory within the untared container file system that you want to scan.
4. Invoke [detect_product_short] there.

## Excluding files/directories from the returned container file system which excludes them from [detect_product_short]'s signature scan

To exclude certain files and/or directories from the returned file system, you can
specify that list of directories with the property *--output.containerfilesystem.excluded.paths*.

For example, if you are invoking [docker_inspector_name] from [detect_product_short], and want [detect_product_short]
to exclude the */etc* and */usr/bin* directories from the signature scan, you
could run [detect_product_short] like this:
```
./detect.sh --detect.docker.image=ubuntu:latest --detect.docker.passthrough.output.containerfilesystem.excluded.paths=/etc,/usr/bin
```

## OCI Image support

[docker_inspector_name] supports [OCI image](https://github.com/opencontainers/image-spec/blob/main/spec.md) archives (.tar files)
passed to it via the [detect_product_short] `detect.docker.tar` property.

[docker_inspector_name] derives the image repo:tag value for each manifest in the index.json file from an
annotation with key `org.opencontainers.image.ref.name` if present.

If insufficient information exists in the image's manifest.json for the repo name and tag, [docker_inspector_name] will default to using the image name, (without the .tar extension), as the project name and "latest" as the project version in the BOM. These default values can be overridden by using the `detect.project.name` and `detect.project.version.name` properties respectively.

If the OCI archive contains multiple images, [docker_inspector_name] constructs the target repo:tag from the values of [detect_product_short] properties `detect.docker.passthrough.docker.image.repo`
and `detect.docker.passthrough.docker.image.tag` (if `detect.docker.passthrough.docker.image.tag` is not set it defaults to "latest"), and looks for a manifest annotation with key `org.opencontainers.image.ref.name`
that has value matching the constructed target repo:tag. If a match is found, [docker_inspector_name] inspects the matching image. If no match is found, or `detect.docker.passthrough.docker.image.repo`
is not set, [docker_inspector_name] fails.
