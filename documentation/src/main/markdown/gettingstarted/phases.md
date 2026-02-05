# [detect_product_short] Processing

[detect_product_short] processing can be broken into the following phases:

## Initialization phase

In this phase, [detect_product_short] does verification checks on the user-provided configuration, checks connectivity to any external systems needed for the run, and creates any required directories.

## Run phase

In this phase, [detect_product_short] processes an ordered list of tools, invoking all that apply, which depends on how [detect_product_short] is configured.

[detect_product_short] analysis is done using an ordered set of tools that you specify using [detect_product_short] properties.

* By default, the build detector tool is run. This detector runs after a build and has access to both build artifacts and build tools; it produces the most accurate results.
* If [bd_product_short] connection details are provided, the [bd_product_short] signature scanner tool also runs by default.

Depending on project contents, the detector tool runs different types of detectors to find and extract dependencies from supported package managers. For example, if [detect_product_short] finds a pom.xml file, it runs the Maven detector. If [detect_product_short] finds Gradle files, it runs the Gradle detector.

At the end of the run phase, [detect_product_short] uploads results to [bd_product_short], and optionally performs tasks such as generating a risk report or checking for policy violations.

## Cleanup phase

During the cleanup phase, [detect_product_short] removes temporary files and directories before exiting.
