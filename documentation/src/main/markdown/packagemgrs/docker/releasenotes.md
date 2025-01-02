# [docker_inspector_name] Release notes

## Version 11.1.0

### Changed features

* Changed [docker_inspector_name]'s JAR signing authority from Synopsys, Inc to Black Duck Software, Inc.

## Version 11.0.1

### Dependency update

* Upgraded Spring framework transitive dependencies to resolve Spring vulnerabilities

## Version 11.0.0

### Changed features
* Branding change from Synopsys to Black Duck.

## Version 10.2.1

### Dependency update
* Logback Core updated to 1.2.13 to resolve high severity issues [CVE-2023-6378](https://nvd.nist.gov/vuln/detail/CVE-2023-6378) and [CVE-2023-6481](https://nvd.nist.gov/vuln/detail/CVE-2023-6481).

## Version 10.2.0

### Changed features

* (IDOCKER-785) [docker_inspector_name] now supports Docker Versions 25 and 26.

## Version 10.1.1

### Resolved issues

* (IDETECT-4058) Improved match accuracy with a correction to identification of components when epoch is zero.

## Version 10.1.0

### Changed features

* (IDETECT-3818) Added support to identify the BDIO namespace and origin as “oracle_linux” correctly when scanning Oracle Linux images.

### Dependency update

* Updated internal build dependencies for Image inspector library to 14.2.0 and Image Inspector Web Service to 5.1.0
* Upgraded Spring Boot to version 2.7.12 to resolve high severity [CVE-2023-20883](https://nvd.nist.gov/vuln/detail/CVE-2023-20883)
* Upgraded SnakeYAML to version 2.0 to resolve critical severity [CVE-2022-1471](https://nvd.nist.gov/vuln/detail/CVE-2022-1471)

## Version 10.0.1

### Dependency update

* Updated internal build dependencies for Image inspector library to 14.1.4, Integration rest library to 10.3.6 and Image Inspector Web Service to 5.0.15

## Version 10.0.0

### Changed features

* Changed name from Black Duck Docker Inspector to Detect Docker Inspector.
* Changed the default value for imageinspector.service.log.length from 10,000 lines to 50,000 lines.

### Removed features

* Removed support for running Docker Inspector as a standalone utility (by executing blackduck-docker-inspector.sh or by executing the [docker_inspector_name] .jar). [docker_inspector_name]  must be invoked by running Synopsys Detect.
* Removed all [docker_inspector_name] properties involved in connecting to Black Duck ("blackduck.*"). Use [company_name] [solution_name] properties instead.

## Version 9.4.3

### Dependency update

* Upgraded to Spring Boot version 2.6.6 / Spring version 5.3.18.

## Version 9.4.2

### Resolved issue

* (IDOCKER-764) Improved the clarity of the error message returned when the file provided via the *docker.tar* property does not have the supported (UNIX tar) format. 

## Version 9.4.1

### Documentation updates

* Added to the documentation the [organize components by layer](advanced.md#organizing-components-by-layer) feature (properties bdio.organize.components.by.layer and bdio.include.removed.components).

## Version 9.4.0

### New features

* Enhanced Black Duck Docker Inspector to provide information on relationships between linux package manager components (ie. discern direct from transitive dependencies).

### Changed features

* Deprecated properties associated with connection to Black Duck. This is a consequence of the transition away from supporting the use of Black Duck Docker Inspector as a standalone utility (as supposed to scanning via Detect).

## Version 9.3.1

### Resolved issue
* (IDETECT-2942) Resolved issue that caused Black Duck Docker Inspector to try to read the body of a bad response from image inspector services without checking the response's status code to make sure it was a successful request. Black Duck Docker Inspector now throws an exception if it receives a bad response.

## Version 9.3.0

### New feature
* Added support for Open Container Initiative (OCI) images provided to Black Duck Docker Inspector using the *docker.tar* property.

### Resolved issue
* (IDOCKER-742) Resolved an issue that caused Black Duck Docker Inspector to fail to find the target image (requested using the *docker.image.repo* property) in a multi-image .tar file when the *docker.image.repo* value includes the registry prefix (e.g. "docker.io/").

## Version 9.2.3

### Resolved issue
* (IDOCKER-736) Resolved an issue that could cause Black Duck Docker Inspector to use the wrong component namespace in BDIO, resulting in an empty BOM, for Oracle Linux images.

## Version 9.2.2

### Resolved issue
* (IDOCKER-722) Resolved an issue that caused Black Duck Docker Inspector to, when it found a "white out opaque directory" file, to delete files added by the current layer as supposed to only deleting files added by lower layers. 

## Version 9.2.1

### Resolved issue
* (IDOCKER-727) Resolved an issue that caused Black Duck Docker Inspector to fail to discover packages in CentOS 7 based images due to an error upgrading the rpm database.

## Version 9.2.0

### New features
* Added the ability to pull and inspect a specified platform of a multi-platform image using the new docker.image.platform property.

### Changed features
* Added support for running Black Duck Docker Inspector using Java 15

### Resolved issue
* (IDOCKER-715) Resolved an issue that could cause Black Duck Docker Inspector to fail on Windows during a docker pull operation with the message "java.lang.NoSuchMethodError: com.sun.jna.Native.load(Ljava/lang/String;Ljava/lang/Class;Ljava/util/Map;)Lcom/sun/jna/Library;".
* (IDOCKER-716) Resolved an issue that caused Black Duck Docker Inspector to discover no packages on fedora:33 and fedora:34 based images.

## Version 9.1.1

### Resolved issues
* (IDOCKER-710) Resolved an issue that could cause Black Duck Docker Inspector to fail when the target image file system contains circular symbolic links.

## Version 9.1.0

### New features
* Added support for running on Windows 10 Enterprise Edition by executing the Black Duck Docker Inspector .jar file directly.
* Added property use.platform.default.docker.host (default to true).
* Added property imageinspector.service.log.length (defaults to 10000 lines). This gives the user control over the
number of lines of the imageinspector service log that are included in the Detect log
when logging level is set to DEBUG or higher.

### Changed feature
* Changed default working directory from /tmp to $HOME/blackduck/docker-inspector

### Resolved issue
* (IDOCKER-709) Resolved an issue that could cause Black Duck Docker Inspector to fail because it ran out of memory while writing the image inspector log to the Black Duck Docker Inspector log.

## Version 9.0.2

### Resolved issues
* (IDOCKER-706) Resolved an issue that could cause Black Duck Docker Inspector to fail when using existing image inspector services when given a target docker .tar file that resided outside the directory shared with the image inspector container(s).

## Version 9.0.1

### Resolved issues
* Resolved an issue that could cause files to be omitted from the squashed image produced by Black Duck Docker Inspector. The problem occurred on images that declared a directory opaque and added files to that directory within the same layer that declared it opaque.

## Version 9.0.0

### Changed feature
* The internal format of the Black Duck Input Output (BDIO) file that is produced is now compatible with [company_name] [solution_name] version 6.3 and later.

## Version 8.3.1

### Resolved issues
* Fixed an issue that prevented the *linux.distro* property from working correctly.

## Version 8.3.0

### New features
* Docker Inspector now writes a summary of results to the file results.json, located in the output directory.

## Version 8.2.3

### Resolved issues
* Eliminated the Spring banner from the log to facilitate piping help output through a Markdown formatter.

## Version 8.2.2

### Resolved issues
* Increased the default value of *service.timeout* from four minutes to ten minutes.
* The time Docker Inspector waits for an image inspector service to come online is now controlled using the *service.timeout* property.

## Version 8.2.1

### Resolved issues
* Resolved an issue that caused *blackduck-docker-inspector.sh* to display the help overview even when a different help topic is requested.
* Resolved an issue that caused Docker Inspector to return the full container filesystem even when only application components are requested (*docker.platform.top.layer.id* is specified).

## Version 8.2.0

### New features
* Added support for Java 11.
* Added the ability to generate help by topic (--help {topic}).
* Added the ability to generate help in HTML.
* Added the ability to write help output to a given file.

## Version 8.1.6

### Changed feature
* Adjusted logging to ensure that sensitive information does not display in a debug log.

## Version 8.1.5

### Resolved issues
* Resolved an issue that could cause Docker Inspector to incorrectly identify the package manager of the target image.

## Version 8.1.4

### New features
* Added a GitLab continuous integration deployment example.

## Version 8.1.3

### New features
* Added a Travis continuous integration deployment example.

## Version 8.1.2

### Changed features
* Updated *blackduck-docker-inspector.sh* to download the Docker Inspector .jar file from the new Artifactory repository at sig-repo.synopsys.com.

## Version 8.1.1

### Resolved issues
* Resolved an issue that caused Docker Inspector to fail when it was unable to read the image inspector container log.

## Version 8.1.0

### Resolved issues
* Resolved an issue that could cause a *No such file* error on files named *classes.jsa* when inspecting images containing Java.
### New features
* Added the property *output.containerfilesystem.excluded.paths*.
* Added the command line switch *--help=true* for invoking help.
* Added the property *output.include.squashedimage*.

## Version 8.0.2

### Resolved issues
* Resolved an issue that could cause missing components on Fedora-based Docker images.

## Version 8.0.1

### Resolved issues
* Resolved an issue that could cause OpenSUSE components to be omitted from the Bill Of Materials. 
### New features
* Increased default image Inspector service timeout from two minutes to four minutes.

## Version 8.0.0

### New features
* Added the ability to collect only those components added to the image package manager database by your application layers, versus the platform layers on which your application is built.
* Added the ability to provide the full code location name.
### Removed features
* Docker exec mode (deprecated in Docker Inspector 7.0.0) is removed. Docker Inspector now supports HTTP client mode only.

## Version 7.3.3

### Resolved issues
* Resolved an issue that could prevent controlling the image inspector HTTP request service timeout through the property *service.timeout*.

## Version 7.3.2
### Resolved issues
* Resolved an issue that could cause RPM packages containing a value in the epoch field to be missing from the Black Duck Bill of Materials (BOM).

## Version 7.3.1

### Resolved issues
* Resolved an issue that could cause Hub Detect versions 5.2.0 and higher to fail with an error message of *DOCKER extraction failed: null* when invoking Docker Inspector on a non-Linux Docker image. 

## Version 7.3.0

### New features
* Added the property *system.properties.path*.

## Version 7.2.4

### Resolved issues
* Resolved an issue that could cause Docker Inspector to fail with an error message of *Error inspecting image: Failed to parse docker configuration file (Unrecognized field "identitytoken")*.

## Version 7.2.3

### Changed features
* When constructing the container file system with the logging level set to DEBUG or TRACE: after applying each image layer, Docker Inspector now logs contents of the layer's metadata (json) file and the list of components.

## Version 7.2.2

### Resolved issues
* Resolved an issue that could cause Black Duck input/output data (BDIO) uploads to fail for openSUSE and Red Hat Docker images.

## Version 7.2.1

### Resolved issues
* Resolved an issue that could cause the Black Duck BOM creation to fail with an error message of *Error in MAPPING_COMPONENTS* displayed on the Black Duck Scans page for certain images.

## Version 7.2.0

### New features
* Added offline mode.
### Resolved issues
* Resolved an issue that could generate a warning message of *Error creating hard link* to be logged when inspecting certain images.
* Resolved an issue that could generate a warning message of *Error removing whited-out file .../.wh..opq* to be logged when inspecting images with opaque directory whiteout files.
* Reduced the disk space used in the working directory within the Inspector containers.

## Version 7.1.0

### Changed features
* Modified the format of the generated external identifiers to take advantage of the Black Duck KnowledgeBase preferred alias namespace feature.
* Modified the format of the generated external identifiers to include the epoch, when applicable, for RPM packages.

## Version 7.0.1

### Resolved issues
* When the logging level is set to DEBUG or higher, the contents of the image inspector service log are now included in the log output.
* The image inspector service is now started with the same logging level as Black Duck Docker Inspector.

## Version 7.0.0

### Changed features
* Hub Docker Inspector is now renamed to Black Duck Docker Inspector. The shell script, .jar filename, properties, and code blocks are updated accordingly.
* Black Duck Docker Inspector now runs in HTTP mode by default.
### Resolved issues
* HTTP client mode: Resolved an issue that prevents removal of the image inspector image upon completion.
* HTTP client mode: Resolved an issue with user-specified Black Duck project names and version names.
* HTTP client mode: Resolved an issue that caused the container filesystem to be provided even when it was not requested.

## Version 6.3.1

### Resolved issues
* Resolved an issue that could cause DEBUG-level warnings to be logged while inspecting images with file paths containing non-ASCII characters.
* Improved logging for when the image inspector service is started but never comes online.
* In http client mode > start service mode: if a health check fails, Docker Inspector now performs a *docker logs* operation on the container to reveal the root problem.
* Orchestration platform properties are now included in the *--help* output.

## Version 6.3.0

### Resolved issues
* Added the *--inspectorimagefamily* command line argument, which prints the Inspector image family name.

## Version 6.2.0

### Resolved issues
* Resolved an issue that caused Docker Inspector to fail when the image repository contained a : character and the image tag was not specified.
### New features
* Added the *--pullairgapzip* option.
* Improved error messages.

## Version 6.1.0

### Resolved issues
* Added support for running on container application platforms such as Kubernetes and OpenShift.
* Renamed Rest Client Mode to HTTP Client Mode.
* Resolved an issue that prevented BDIO (Black Duck Input/Output data) from being uploaded to the Hub. This issue only impacted HTTP client mode.
* Resolved an issue that caused Docker Inspector to fail when inspecting an image containing no package manager. This issue only impacted HTTP client node.

## Version 6.0.4

### Resolved issues
* Resolved an issue wherein Hub Docker Inspector may fail if the target Docker tarfile path contained spaces.

## Version 6.0.3

### Resolved issues
* Resolved an issue causing Hub Docker Inspector to produce an unnecessarily large container filesystem output file.

## Version 6.0.2
### Resolved issues
* Resolved an issue causing Hub Docker Inspector to fail when the image exists in the local cache but not the registry.

## Version 6.0.1

### Resolved issues
* Removed extraneous and possibly misleading log messages.
### New features
* Added the properties *docker.image.repo* and *docker.image.tag* to the usage message generated when using the command line argument *--help*.

## Version 6.0.0

### New features
* Added REST client mode.
### Changed features
* The available properties list included in the usage message, which displays when using the command line argument *--help*, is now sorted alphabetically.
* The format of the (optional) container filesystem output file name has changed.  The new container system file name is *{image name}_{image tag}_ containerfilesystem.tar.gz* or *{image tarfilename}.tar.gz*, depending on how the target image is specified.
