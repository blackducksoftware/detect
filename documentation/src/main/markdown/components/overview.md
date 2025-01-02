# [detect_product_long] Components

This topic introduces the components in [detect_product_short] that are used to examine your code and produce analyzable output.

The components comprise the following:

## Tools

Each [detect_product_short] run consists of running any applicable [detect_product_short] [tools](tools.md) used in the analysis of code.

## Detectors

[detect_product_short] uses [detectors](detectors.md), appropriate to your package manager ecosystem, to find and extract dependencies from all supported package managers.

## Inspectors

An [inspector](inspectors.md) is typically a plugin that [detect_product_short] uses to access the internal resources of a package manager through its API.
