# Ivy (Ant) support

## Overview

[detect_product_short] includes two Ivy detectors:

* **Ivy CLI Detector**
* **Ivy Build Parse Detector**

## Ivy CLI Detector

* Detector for Ant + Ivy projects, extracts **direct and transitive dependencies** using the `ivy:dependencytree` Ant task.

**Requirements:**
* A valid `ant` executable must be available on the system.
* An `ivy.xml` and `build.xml` file must be present in the project.
* `build.xml` must contain a target with the `<ivy:dependencytree>` task.
* Apache Ant 1.6.0+ and Apache Ivy 2.4.0+ required.

**Behavior:**
* Automatically discovers the target containing `<ivy:dependencytree>` in `build.xml`.
* Executes `ant <targetName>` to generate the dependency tree.
* Parses the output to construct a hierarchical **dependency graph** with parent-child relationships.
* Falls back to the Ivy Build Parse Detector if `ivy:dependencytree` is unavailable.

To specify a custom Ant executable path, use the `detect.ant.path` property.

To facilitate dependency tree generation, the `build.xml` must contain a target with the `ivy:dependencytree` task:

```xml
<target name="dependencytree">
    <ivy:resolve />
    <ivy:dependencytree log="download-only" />
</target>
```

## Ivy Build Parse Detector

If `ivy:dependencytree` is unavailable, [detect_product_short] will default to the Ivy Build Parse Detector, which extracts dependencies by parsing the `ivy.xml` file.

* Parses `ivy.xml` for direct dependency declarations only. Transitive dependencies are not resolved.
* Extracts the project name and version from `build.xml`. If the project name or version is missing, the values are derived via Git from the project's directory or defaults.
