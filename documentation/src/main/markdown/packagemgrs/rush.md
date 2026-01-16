# Rush support

## Related properties

[Detector properties](../properties/detectors/rush.md)

## Overview

The Rush Detector will run in the presence of a rush.json file.

Each package has a location within the project structure defined in the `rush.json` file. [detect_product_short] will parse it to find the type of package manager used and all the projects and its location.

It is expected to find some type of lock file within `common/config/rush` directory.
Supported lockfile types are pnpm-lock.yaml, npm-shrinkwrap.json, and yarn.lock.

If no lockfile is present at the above-mentioned directory in the project, Rush extraction will fail.

## Extracting from pnpm-lock.yaml

The Rush detector will execute the same code as the [PNPM detector](pnpm.md).

The [PNPM detector](../properties/detectors/pnpm.md) related properties also apply.

### Exclude/Include Subspaces

Since PNPM package manager supports concept of subspaces in Rush, individual subspaces can be excluded/included in the scan result using properties defined here [Rush Subspaces](../properties/detectors/rush.md)

## Extracting from npm-shrinkwrap.json

The Rush detector will execute the same code as the [NPM shrinkwrap detector](npm.md#npm-shrinkwrap).

The [NPM shrinkwrap detector](../properties/detectors/npm.md/) related properties also apply.

Since the Rush detector is currently not using the NPM Cli, the only property that applies is [detect.npm.dependency.types.excluded](../properties/detectors/npm.md#npm-dependency-types-excluded).

## Extracting from yarn.lock

The Rush detector will execute the same code as the [Yarn detector](yarn.md#yarn-support).

The [Yarn detector related properties](../properties/detectors/yarn.md) also apply.

Yarn workspaces are not currently supported by the Rush detector.
