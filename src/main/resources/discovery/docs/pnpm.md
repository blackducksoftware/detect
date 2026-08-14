# pnpm support

## Related properties

[Detector properties](../properties/detectors/pnpm.md)

## Overview

[detect_product_short] runs the pnpm detector if it finds a pnpm-lock.yaml file in your project, and parses the file to obtain information on your project's dependencies.

To specify which types of dependencies you want [detect_product_short] to exclude from the BOM (Dev and Optional dependencies) use the detect.pnpm.dependency.types.excluded property.

The pnpm detector extracts the project's name and version from the package.json file. If it does not find a package.json file, it will defer to a project name derived by git, from the project's directory, or defaults.
