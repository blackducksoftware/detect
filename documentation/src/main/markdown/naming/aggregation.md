# BDIO aggregation

[detect_product_short] aggregates all package manager results into a single BDIO file / codelocation.

All dependency graphs produced by any of the following, executed during the [detect_product_short] run, will be aggregated:

* Detectors
* Docker Inspector
* Bazel

This BDIO takes advantage of [bd_product_short] functionality that enables [bd_product_short] to preserve both source information (indicating, for example, from which
subproject a dependency originated) and match type information (direct vs. transitive dependencies).

[detect_product_short] now operates in a way that is similar to [detect_product_short] 7
run with property detect.bom.aggregate.remediation.mode=SUBPROJECT.
The property detect.bom.aggregate.remediation.mode does not exist in [detect_product_short] 8.

## Related properties

* [detect.bdio.output.path](../properties/configuration/paths.md#bdio-output-directory)
* [detect.bdio.file.name](../properties/configuration/paths.md#bdio-file-name)

