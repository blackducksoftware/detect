# Choosing the Detect target type

[detect_product_short] will select a workflow based in part on the target type you select via the [detect.target.type](../../properties/all-properties.md) property.

When running [detect_product_short] on project source code, you can set *detect.target.type* to *SOURCE*, or leave *detect.target.type* unset (since *SOURCE* is the default value).

When running [detect_product_short] on a Docker image, you will want to set *detect.target.type* to *IMAGE*.

## Common workflows

By default (detect.target.type=SOURCE), [detect_product_short] will run the following on the source directory:

1. Any applicable detectors
1. [blackduck_signature_scanner_name]

When a Docker image is provided and property *detect.target.type* is set to IMAGE, [detect_product_short] will run the following on the image:

1. Docker Inspector
1. [blackduck_signature_scanner_name]
1. [bdba_product_short]
