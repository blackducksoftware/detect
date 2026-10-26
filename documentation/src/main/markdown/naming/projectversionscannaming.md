# Project, Version, and Code Location Naming

The following sections describe the project, version, and code location (scan) naming in [detect_product_long].

## Project and version naming

The project and version names of the project to which [detect_product_short] writes results are, by default, derived from the project on which [detect_product_short] is run. The mechanism [detect_product_short] uses to determine the project and version names depends on project type. If [detect_product_short] cannot determine the project and version names, then [detect_product_short] uses the project directory name as the project name, and the value "Default Detect Version" as the version name.

You can use the following properties to override the project and version names:
```
--detect.project.name=PROJECT-NAME
--detect.project.version.name=VERSION-NAME
```

## Project and version naming for Git projects

If no package manager provides project and version names, you have not provided the project and version names through properties, and the project uses Git, [detect_product_short] attempts to use Git to determine project information.

Project information is extracted from the remote URL for the current branch. The version is the current branch name, or the commit hash if a detached head is checked out. This is done by the Git detector. If you don't want [detect_product_short] to use Git data, omit the Git detector using the following property:
```
--detect.excluded.detector.types=GIT
```

For example, for a project with a remote URL of "https://github.com/blackducksoftware/blackduck-detect" and a checked-out branch of "9.10.0", [detect_product_short] by default uses the project name "blackducksoftware/blackduck-detect" and project version "9.10.0".

[detect_product_short] attempts to derive project and version information by running the Git executable. If that is not successful, it attempts to derive project and version information by parsing Git files.

The [detect_product_short] property for providing the path to the Git executable: `detect.git.path`.

## Code location (scan) naming

[detect_product_short] often generates multiple code locations (scans) in a single run. Each code location name consists of a base name and a type suffix that indicates what type of scan generated it.

| Scan type | Default base name |Suffix |
|---|---|---|
| Package manager (all detectors plus Docker Inspector and Bazel) | project/version | bdio |
| Impact analysis | directory/project/version | impact |
| Signature | directory/project/version | signature |
| Binary | file/project/version | binary |
| IaC | directory/project/version | iac |

You can modify the base name by adding a prefix and/or a suffix to the default base name using the `detect.project.codelocation.prefix` and `detect.project.codelocation.suffix` properties.

<note type="important">
  <ul>
	<li>Black Duck recommends that you set project and project version names using
    <codeph>detect.project.name</codeph> and
    <codeph>detect.project.version.name</codeph> instead of manually assigning
    code location names.</li>
  <li>Detect automatically generates code location
    names from project and version information. In most CI/CD environments,
    generated code location names provide the most reliable and maintainable
    configuration.</li>
  <li>Code location names must be unique within Black Duck SCA. Reusing manually
    assigned code location names across repositories, branches, pipelines, or
    projects can cause scan results to be associated with the wrong project
    version, result in incomplete scan data, and make troubleshooting more
    difficult.</li>
	</ul>
</note>

You also have the option to set the base name using the `detect.code.location.name` property.

```
--detect.code.location.name=CODELOCATION-NAME
```

When `detect.code.location.name` is set, `detect.project.codelocation.prefix` and `detect.project.codelocation.suffix` are ignored.

Use `detect.code.location.name` only when you require a specific, stable code location name and can guarantee that the name remains unique across all repositories, branches, and pipelines that write scan results to the Black Duck SCA instance.
