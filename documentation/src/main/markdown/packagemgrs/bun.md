# Bun support

## Overview

[detect_product_short] includes one Bun detector:

* **Bun Lockfile Detector**

## Bun Lockfile Detector

* Detector for Bun projects, extracts **direct and transitive dependencies** by parsing the [bun.lock](https://bun.sh/docs/install/lockfile) file.

**Requirements:**
* A `package.json` file must be present in the project.
* A `bun.lock` file must be present in the same directory as `package.json`.

**Behavior:**
* Parses the `bun.lock` file to obtain information on your project's dependencies.
* Constructs a hierarchical **dependency graph** of the project's direct and transitive dependencies.
* Does not require a `bun` executable to be available on the system.
* Extracts the project's name and version from the `package.json` file. If it does not find a `package.json` file, it will defer to a project name derived by git, from the project's directory, or defaults.

<note type="note">The Bun Lockfile Detector supports only the text-based `bun.lock` file. The binary `bun.lockb` file is not supported.</note>
