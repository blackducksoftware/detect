# Conda Support

## Related properties

[Detector properties](../properties/detectors/conda.md)

## Overview

The Conda detector discovers dependencies of python projects utilizing the Conda package and environment manager.

[detect_product_short] runs the Conda detector if an environment.yml or environment.yaml file is found in your project.

The Conda detector requires that the *conda* executable is on the PATH, or that its path is passed in via `--detect.conda.path`.

The Conda detector runs `conda list -n [environment_name] --json` and `conda info --json`, and parses the output of both commands to discover dependencies.

Note: To specify a Conda environment to be referenced when running `conda list`, pass the name of the environment using `--detect.conda.environment.name` (if not passed, `-n` flag is omitted).
Refer to [Properties](../properties/detectors/conda.md) for details.
