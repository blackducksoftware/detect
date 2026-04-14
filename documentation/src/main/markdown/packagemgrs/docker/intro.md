# [detect_product_short] Docker image support

## Related properties

[Detector properties](../../properties/detectors/docker.md)

## Overview

On Linux, Mac, and Windows 10 Enterprise, [detect_product_short] can invoke [docker_inspector_name] to inspect Linux Docker images to discover packages installed by the Linux package manager.
For simple use cases, add ```--detect.docker.image={repo}:{tag}```, ```--detect.docker.tar={path to an image archive}```, or
```--detect.docker.image.id={image id}```,
to the [detect_product_short] command line.

When passed a value for *detect.docker.image*, *detect.docker.image.id*, or *detect.docker.tar*,
[detect_product_short] runs [docker_inspector_name] on the given image (the "target image"),
creating one BDIO file for one code location.

[docker_inspector_name] will:

1. Discover packages (components) installed in a given Linux image by analyzing the contents of the Linux package manager database.
2. Provide to [detect_product_short], for any image, potentially useful targets (file archives) for signature and binary scanning.

[docker_inspector_name] does not run the target image, so it is safe to run it on untrusted images.

While earlier versions of [docker_inspector_name] could be run standalone,
the only way to use [docker_inspector_name] now and in the future is
to run [detect_product_short] on a Docker image.

## Package (component) discovery

For package discovery on a Linux image, [docker_inspector_name] extracts the Linux package manager
database from the image, and utilizes the appropriate Linux package manager to provide a list of
the installed packages, which
it returns to [detect_product_short] in BDIO ([bd_product_short] Input Output) format.
Because it relies on the Linux package manager as its source of this data,
the discovered packages are limited to those installed and managed using the Linux package manager.

[docker_inspector_name] can discover package manager-installed components in
Linux Docker images that use the DPKG, RPM, or APK package manager database formats.

## Signature and binary scan targets

Signature and binary scan targets contain the container file system.
The container file system
is the file system that a container created from the target image. The
container file system is (by default) returned to [detect_product_short] in two forms:
as an archive file that contains the container file system (the preferred format for binary
scanning), and as a saved squashed (single layer) image
that contains the container file system (the preferred format for signature scanning).

## Non-linux images

When run on a non-Linux image (for example, a Windows image,
or an image that contains no operating system), [docker_inspector_name]
will return to [detect_product_short] a BDIO file with zero components
along with the signature and binary scan targets.
Components may be discovered for these images
during the signature and/or binary scanning perfomed by
[detect_product_short].

## Modes of operation

[docker_inspector_name] has two modes:

* Host mode, for running on a server or virtual machine (VM) where [docker_inspector_name] can perform Docker operations using a Docker Engine.
* Container mode, for running inside a container started by Docker, Kubernetes, OpenShift, and others.

In either mode, [docker_inspector_name] runs as a [detect_product_short] inspector to extend the capaibilities of [detect_product_short].
[docker_inspector_name] is more complex than most [detect_product_short] inspectors because it relies on container-based services
(the image inspector services)
to perform its job. When running on a host machine that has access to a Docker Engine ("host mode"),
[docker_inspector_name] can start and manage the image inspector services (containers) automatically.
When [detect_product_short] and [docker_inspector_name] are running within a Docker container
("container mode"), the image inspector services must be started and managed by the user or
the container orchestration system.

### Host mode

Host mode (the default) is for servers/VMs where [docker_inspector_name] can perform Docker operations (such as pulling an image)
using a Docker Engine.

Host mode requires that [docker_inspector_name] can access a Docker Engine. https://github.com/docker-java/docker-java utilizes the
[docker-java library](https://github.com/docker-java/docker-java) to act as a client of that Docker Engine.
This enables [docker_inspector_name] to pull the target image from a Docker registry such
as Docker Hub. Alternatively, you can save an image to a .tar file by using the *docker save* command. Then, run [docker_inspector_name] (via [detect_product_short])
on the .tar file. See [Supported image formats](formats.md) for details on supported .tar file formats.

In Host mode, [docker_inspector_name] can also pull, run, stop, and remove the image inspector service images as needed,
greatly simplifying usage, and greatly increasing run time.

### Container mode

Container mode is for container orchestration environments (Kubernetes, OpenShift, etc.)
where [detect_product_short] and [docker_inspector_name] run
inside a container where [docker_inspector_name] cannot perform Docker operations.
For information on running [docker_inspector_name] in container mode,
refer to [Deploying](deployment.md).

It is possible to utilize container mode when running [detect_product_short] and [docker_inspector_name] on a host
that supports host mode. Container mode is more difficult to manage than host mode,
but you might choose container mode in order to increase throughput (to scan more images per hour).
Most of the time spent by [docker_inspector_name] running in host mode is spent starting and stopping the image inspector services.
When these services are already running (in the usual sense of the word "service")
as they do in container mode,
[docker_inspector_name] executes much more quickly than it would in host mode.

## Requirements

Requirements for including [docker_inspector_name] in a [detect_product_short] run
include of all of [detect_product_short]'s requirements plus:

* Three available ports for the image inspector services. By default, these ports are 9000, 9001, and 9002.
* The environment must be configured so that files created by [docker_inspector_name] are readable by all. On Linux, this means an appropriate umask value (for example, 002 or 022 would work). On Windows, this means the
Detect "output" directory (controlled by the [detect_product_short] property *detect.output.path*)
must be readable by all.
* In host mode: access to a Docker Engine versions 17.09 or higher running as root.
* In container mode: you must start the [docker_inspector_name] container that meets the preceding requirements, and three container-based
"image inspector" services. All four of these containers must share a mounted volume and be able to reach each other through HTTP GET operations using base URLs
that you provide. For more information, refer to [Deploying](deployment.md).
    
## Running [docker_inspector_name]

To invoke [docker_inspector_name], pass a docker image to 
[detect_product_short] via one of the following properties:

* detect.docker.image
* detect.docker.image.id
* detect.docker.image
* detect.docker.tar

## Advanced usage (using passthrough properties)

The most common cases of [docker_inspector_name] can be configured using 
[detect_product_short] properties.
However, there are scenarios (including container mode)
that require access to [docker_inspector_name] advanced properties for which there is no corresponding
[detect_product_short] property.
For the list of [docker_inspector_name] advanced properties, see [Advanced properties](advanced-properties.md).

When you need to set one of the [docker_inspector_name] advanced properties,
construct the [detect_product_short] property name by prefixing the Docker Inspector property name with ```detect.docker.passthrough.```.

Suppose you need to set Docker Inspector's `service.timeout` value (the length of time Docker Inspector waits for a response from the Image Inspector services that it uses) to 480000 milliseconds. You add the prefix to the Docker Inspector property name to derive the [detect_product_short] property name ```detect.docker.passthrough.service.timeout```. Therefore, add ```--detect.docker.passthrough.service.timeout=480000``` to the [detect_product_short] command line.

For example:
```
./detect11.sh --detect.docker.image=ubuntu:latest --detect.docker.passthrough.service.timeout=480000
```

You can set any [docker_inspector_name] property using this method.
However, you should not use this method to change the value of the [docker_inspector_name] property `output.path`.
[detect_product_short] sets this property and changing its value via the passthrough mechanism will make it impossible
for [detect_product_short] to find [docker_inspector_name]'s output files.

## Transitioning from [bd_product_short] Docker Inspector to [detect_product_short]

If you have been running the [bd_product_short] Docker Inspector directly, and need to transition to
invoking [docker_inspector_name] from [detect_product_short], here are some recommendations likely to 
help you make the transition:

1. If you run [bd_product_short] Docker Inspector with `blackduck-docker-inspector.sh`, replace `blackduck-docker-inspector.sh` in your command line with `detect11.sh` (adjust the [detect_product_short] major version as necessary or run `detect.sh` to always pick up the latest [detect_product_short] release).
See the [detect_product_short] documentation for information on where to get the [detect_product_short] script.
1. If you run [bd_product_short] Docker Inspector with `java -jar blackduck-docker-inspector-{version}.jar`, replace `blackduck-docker-inspector-{version}.jar` in your command line with `blackduck-detect-{version}.jar`.
See the [detect_product_short] documentation for information on where to get the [detect_product_short] .jar.
1. For each of the following properties used in your command line, add `detect.` to the beginning of the property name: docker.image, docker.image.id, docker.tar, docker.platform.top.layer.id. For example, change `--docker.image=repo:tag` with `--detect.docker.image=repo:tag`.
1. For all other Docker Inspector properties used in your command line, add `detect.docker.passthrough.` to the beginning of the property name. For example, change `--bdio.organize.components.by.layer=true` to `--detect.docker.passthrough.bdio.organize.components.by.layer=true`.
