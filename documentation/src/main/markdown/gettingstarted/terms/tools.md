# Tools

[detect_product_long] tools are components that enable the scanning of your source code. [detect_product_short] uses several underlying tools to perform scanning including:   
* Detector (for inspecting package manager dependencies)   
* Signature Scanner (for inspecting the file system)   
* Docker Inspector (for inspecting Docker container content)   
* Bazel detector ( to discover dependencies in Bazel projects)   
* Binary Analysis (used to determine components within binary files)   
* Vulnerability Impact Analysis Tool (generates a Vulnerability Impact Analysis Report)   
* IaC Scanner (supports infrastructure as code scanning)   
* Container Scanning (scanning container images to provide component risk details)   

Optional properties can be specified to explicitly enable or disable these underlying tools, but by default, [detect_product_short] will run both the Detector and Signature Scanner on the code being analyzed when a valid path with analyzable content is provided.

The default tools that are run by [detect_product_short]:

* Detector (--detect.tools=DETECTOR).
The detector tool runs the appropriate detectors that are used to find and extract dependencies by using package manager inspection.
* [blackduck_signature_scanner_name] (--detect.tools=SIGNATURE_SCAN).
The [blackduck_signature_scanner_name] tool runs by default when [bd_product_short] connection details are provided. A file/folder (Signature) scan is performed on the built project to examine all project files for open-source software.

Other [detect_product_short] tools such as Docker Inspector or [blackduck_binary_scan_capability] are not run by default in most scenarios but can be added to a run by specifying their properties on the command line.
