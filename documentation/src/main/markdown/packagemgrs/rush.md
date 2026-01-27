# Rush support

## Related properties

[Detector properties](../properties/detectors/rush.md)

## Overview

When a rush.json file is present in the analyzed code, the Rush Detector will run.

Included packages have a location within the project structure defined in the `rush.json` file. [detect_product_short] will parse the `rush.json` file to determine the type of package manager used, included projects, and their locations.

It is expected to find some type of lock file within `common/config/rush` directory.
Supported lockfile types are pnpm-lock.yaml, npm-shrinkwrap.json, and yarn.lock.

Rush Detector requires one of `pnpm-lock.yaml`, `npm-shrinkwrap.json`, or `yarn.lock` be present in the `common/config/rush` directory of the analyzed project. If no lockfile is present in the `common/config/rush` directory, Rush Detector will exit.

## Extracting from pnpm-lock.yaml

When a pnpm-lock.yaml file is located in the analyzed directory, the Rush Detector will take [PNPM detector](../properties/detectors/pnpm.md) related properties as input.

### Exclude/Include Subspaces

Rush Detector supports inclusion and exclusions of individual subspaces in the scan results using properties defined here: [Rush Subspaces](../properties/detectors/rush.md)

## Extracting from npm-shrinkwrap.json

When a npm-shrinkwrap.json file is located in the analyzed directory, the Rush Detector will take [NPM shrinkwrap detector](../properties/detectors/npm.md) related properties as input.

Since the Rush detector is currently not using the NPM Cli, the only property that applies is [detect.npm.dependency.types.excluded](../properties/detectors/npm.md#npm-dependency-types-excluded).

## Extracting from yarn.lock

When a yarn.lock file is located in the analyzed directory, the Rush Detector will take [Yarn detector](../properties/detectors/yarn.md) related properties as input.

Yarn workspaces are not currently supported by the Rush detector.
