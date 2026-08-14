# Lerna support

## Related properties

[Detector properties](../properties/detectors/lerna.md)

## Overview

The Lerna detector will register in the presence of a lerna.json file.

It will then execute a lerna command to retrieve all the packages defined in the project.

Each package has a location within the project structure.

It is expected to find a package.json and some type of lock file.
Supported lockfile types are package-lock.json, npm-shrinkwrap.json, and yarn.lock.

If no lockfile is present in the package, it will be assumed that all the dependencies defined within the package's package.json file will be resolved in the lockfile at the root of the project.
If no lockfile is present at the root of the project, Lerna extraction will fail.

## Extracting from package-lock.json

The Lerna detector will execute the same code as the [NPM package lock detector](npm.md#npm-package-lock).

The [NPM package lock detector](../properties/detectors/npm.md) related properties also apply.

Since the Lerna detector is currently not using the NPM Cli, the only property that applies is [detect.npm.dependency.types.excluded](../properties/detectors/npm.md#npm-dependency-types-excluded).

## Extracting from npm-shrinkwrap.json

The Lerna detector will execute the same code as the [NPM shrinkwrap detector](npm.md#npm-shrinkwrap).

The [NPM shrinkwrap detector](../properties/detectors/npm.md/) related properties also apply.

Since the Lerna detector is currently not using the NPM Cli, the only property that applies is [detect.npm.dependency.types.excluded](../properties/detectors/npm.md#npm-dependency-types-excluded).

## Extracting from yarn.lock

The Lerna detector will execute the same code as the [Yarn detector](yarn.md#yarn-support).

The [Yarn detector related properties](../properties/detectors/yarn.md) also apply.

Yarn workspaces are not currently supported by the Lerna detector.

## Private packages

With the [detect.lerna.package.types.excluded](../properties/detectors/lerna.md#lerna-dependency-types-excluded) property, users can specify whether or not to include private packages as defined by Lerna.

## Lerna path

[detect_product_short] executes commands against the Lerna executable to determine package information.

[detect_product_short] will attempt to find the Lerna executable, but if the user wishes to override the executable [detect_product_short] uses, they can supply a path to the executable using [detect.lerna.path](../properties/detectors/lerna.md#lerna-executable)

## Excluding Packages

The Lerna detector includes/excludes Lerna packages found when it runs `lerna ls --all --json` as specified by [detect.lerna.packages.included](../properties/detectors/lerna.md#lerna-packages-included-advanced) and [detect.lerna.packages.excluded](../properties/detectors/lerna.md#lerna-packages-excluded-advanced).
