# Tools

Each [detect_product_long] run consists of running any applicable [detect_product_short] tools.

The available [detect_product_short] tools in order of potential execution, with the corresponding [detect tools property](../properties/configuration/paths.md#detect-tools-included)
value specified in parentheses are:

* [Docker Inspector](../packagemgrs/docker/intro.md) (--detect.tools=DOCKER)
* [Bazel](../packagemgrs/bazel.md) (--detect.tools=BAZEL)
* [Detector](../components/detectors.dita) (--detect.tools=DETECTOR)
* [Black Duck Signature Scanner](../runningdetect/basics/runningwithblackduck.md) (--detect.tools=SIGNATURE_SCAN)
* [Black Duck - Binary Analysis](../runningdetect/basics/runningwithblackduck.md) (--detect.tools=BINARY_SCAN)
* [IaC Scanner](../runningdetect/basics/runningwithblackduck.md) (--detect.tools=IAC_SCAN)
* [Container Scan](../runningdetect/containerscanning.md) (--detect.tools=CONTAINER_SCAN)
* ReversingLabs Scan (--detect.tools=THREAT_INTEL) **Deprecated**

The detector tool runs any applicable [detectors](../components/detectors.dita).
