# NuGet support

## Related properties

[Detector properties](../properties/detectors/nuget.md)

## Overview

The NuGet detectors are used to discover dependencies of NuGet projects.

There are three NuGet detectors: 
 * NuGet Solution Native Inspector
 * NuGet Project Native Inspector
 * NuGet Project Inspector

The detectors run a platform dependent self-contained executable that is currently supported on Windows, Linux, and Mac platforms.

<note type="note">

* NuGet Project Inspector does not accept NuGet specific configuration properties.
* The NuGet Detectors do not work with mono.
</note>

## Excluding dependency types
[detect_product_short] offers the ability to exclude package manager specific dependency types from the BOM.
Nuget dependency types can be filtered with the [detect.nuget.dependency.types.excluded](../properties/detectors/nuget.md#nuget-dependency-types-excluded) property.
This property supports exclusion of development only dependencies in projects that use PackageReference or packages.config.
<note type="note">Support for declaring dependencies in JSON files has been deprecated by NuGet. As such this property does not apply to scans that analyze project.json, project.lock.json or project.assets.json.</note>

A project might be using a dependency purely as a development harness and you might not want to expose that to projects that will consume the package. You can use the PrivateAssets metadata to control this behavior. [detect_product_short] looks for the PrivateAssets attribute used within PackageReference tags to identify a development dependency. [detect_product_short] will ignore the contents of the tag and only observe the presence of these PrivateAssets to exclude those development related dependencies.
For packages.config file, [detect_product_short] will look for developmentDependency tags to determine whether to include or exclude a dependency.

## NuGet Artifacts and Base Intermediate Output Paths
[detect_product_short] supports the ArtifactsPath and BaseIntermediateOutputPath properties provided by NuGet to customize the path in which build artifacts are stored. The default location for storing artifacts is the \obj folder under each project directory, in which XML files for the project such as csproj are present.

To simplify the output paths and gather all the artifacts in a common location, support for the above properties was introduced. To support these properties, [detect_product_short] uses the [detect.nuget.artifacts.path](../properties/detectors/nuget.md#nuget-artifacts-path) property, which allows you to specify a custom project.assets.json location.

[detect_product_short] will examine all directories in the provided path to find the project.assets.json file for the project being scanned.

To avoid requiring .NET 6 to get the resolved artifacts path from the Directory.Build.props file, it is required that the directory specified by the detect.nuget.artifacts.path property have permission set to allow [detect_product_short] access.

### [detect_product_short] NuGet Inspector downloads

[detect_product_short] jar execution will automatically download any required binaries not located in the cache.

For direct access to the binaries or source code see [download locations](../downloadingandinstalling/downloadlocations.md).

### Inspector Operation

An inspector is self-contained and requires no installation. Each executable is platform dependent and the correct inspector is downloaded by [detect_product_short] at runtime.

NuGet Solution Native Inspector runs if one or more solution (.sln) files are found and derives packages (dependencies) via analysis of solution files. Central Package Management is supported to include any package versions and global package references mentioned under `Directory.Packages.props` files indicated the (.sln) file for each project under the solution. Any package references and versions in the solution's `Directory.Build.props` will be included for each project under the solution.

<note type="tip">When running the NuGet Solution Native Inspector the `--detect.detector.search.depth=` value is ignored if a solution (.sln) file is found that contains project references that include subdirectories at levels lower than the specified search depth.
</note>

NuGet Project Native Inspector runs if no solution (.sln) files are found, and one or more project files are found. NuGet Project Native Inspector derives packages (dependencies) from project (.csproj, .fsproj, etc.) file content.

NuGet inspectors derive dependency information from solution (.sln) files in this order:
1. Directory.Packages.props
2. packages.config
3. project.lock.json
4. project.assets.json
5. project.json
6. XML of the project file

In addition to the packages and dependencies found from the above files, packages and dependencies will be included from other `project.assets.json` files if configured in the corresponding project's property file. (`<projectname>.<projectfiletype>.nuget.g.props`).

After discovering dependencies, NuGet client libraries are used to collect further information about the dependencies and write them to a JSON file (`<projectname>_inspection.json`). [detect_product_short] then parses that file for the dependency information.

### NuGet Project Native Inspector supported project files

| Azure Stream Analytics | Cloud Computing | Common Project System Files | C# | Deployment | Docker Compose | F# |
|---|---|---|---|---|---|---|
| *.asaproj | *.ccproj | *.msbuildproj | *.csproj | *.deployproj | *.dcproj | *.fsproj |

| Fabric Application | Hive | JavaScript | .NET Core | Node.js | Pig | Python |
|---|---|---|---|---|---|---|
| *.sfproj | *.hiveproj | *.jsproj | *.xproj | *.njsproj | *.pigproj | *.pyproj |

| RStudio | Shared Projects | SQL | SQL Project Files | U-SQL | VB | VC++ |
|---|---|---|---|---|---|---|
| *.rproj | *.shproj | *.sqlproj | *.dbproj | *.usqlproj | *.vbproj | *.vcxproj *.vcproj |

### NuGet Detector buildless mode

In buildless mode, [detect_product_short] uses Project Inspector to find dependencies and only supports `.csproj` and `.sln` files.

As of [detect_product_short] 9.5.0 the version of Project Inspector in use supports the `--build-system MSBUILD` argument in place of `--strategy MSBUILD`.
The `--force-nuget-repos "url"` argument will be removed from support in the next [detect_product_short] major release and replaced with the `--conf "nuget.repo:url"` argument.

### [detect_product_short] NuGet Inspector on Alpine

The [detect_product_short] NuGet Inspectors depend on packages not installed by default on Alpine systems, such as the dynamic loader for DLLs.

When the dynamic loader is not present, an error message similar to the following appears in the log as a result of
[detect_product_short]'s attempt to execute the NuGet Inspector:
```
java.io.IOException: Cannot run program ".../tools/detect-nuget-inspector/detect-nuget-inspector-1.0.1-linux/detect-nuget-inspector" (in directory ...): error=2, No such file or directory
```

To add these packages to an Alpine system:
```
apk add libstdc++ gcompat icu
```
