# Gradle support

## Related properties

[Detector properties](../properties/detectors/gradle.md)

<note type="Note">Gradle Project Inspector relies on the Project Inspector tool thus does not accept Gradle specific configuration properties.
</note>

## Overview

[detect_product_short] has two detectors for Gradle:

* Gradle Native Inspector
* Gradle Project Inspector

## Gradle Native Inspector

* Discovers dependencies of Gradle projects.

* Will run on your project if it finds a build.gradle, build.gradle.kts, settings.gradle, or settings.gradle.kts file in the top level source directory.

Gradle Native Inspector requires either gradlew or gradle:

1. [detect_product_short] looks for gradlew in the source directory (top level). You can override this by setting the Gradle path property. If not overridden and not found:
1. [detect_product_short] looks for gradle on $PATH.

Runs `gradlew gatherDependencies` to get a list of the project's dependencies, and then parses the output.

Gradle Native Inspector allows you to filter projects based on both the name and the path. The path is unique for each project in the hierarchy and follows the form ":parent:child". Both filtering mechanism support wildcards.

The inspector defines the custom task 'gatherDependencies' with the help of a Gradle script (`init-detect.gradle`). 
Filtering (including/excluding projects and configurations) is performed by the Gradle/Groovy code to control
the output of the `dependencies` Gradle task invoked by the 'gradlew gatherDependencies' command.

The init-detect.gradle script configures each project with the custom 'gatherDependencies' task, which invokes the 'dependencies' Gradle task on each project. This ensures the same output is produced as previous versions. The inspector consumes the output of `gradlew gatherDependencies` task.

### Rich version declaration support

Rich version declarations allow a user to define rules around which version of a given direct or transitive dependency are resolved when Gradle performs its dependency conflict resolution. Typically, these are set in a parent build.gradle file, and because these rich version declarations set a specific requirement that conflict resolution must respect, the subsequent child modules will pull dependencies according to the rich version declaration.
[detect_product_short] derives this information from the dependency graph that Gradle Native Inspector generates as described above. If the information is not mentioned in the graph then [detect_product_short] will not support those declarations.
See Gradle documentation: [Rich Version Declaration](https://docs.gradle.org/current/userguide/rich_versions.html).

### Running the Gradle Native Inspector with a proxy

[detect_product_short] will pass along supplied [proxy host](../properties/configuration/proxy.md#proxy-host-advanced) and [proxy port](../properties/configuration/proxy.md#proxy-port-advanced) properties to the Gradle daemon if applicable.

### Gradle Project Inspector (Buildless)

For buildless detection, the gradle detector uses Project Inspector to find dependencies.

Currently supports capturing dependencies from files with the pattern `*.gradle`, including the standard `build.gradle` file. 

<note type="note">Does not support Kotlin build files or dependency exclusions.</note>

As of [detect_product_short] 9.5.0 the version of Project Inspector in use supports the `--build-system GRADLE` argument in place of `--strategy GRADLE`.
The `--force-gradle-repos "url"` argument will be removed from support in the next [detect_product_short] major release and replaced with the `--conf "maven.repo:url"` argument.
