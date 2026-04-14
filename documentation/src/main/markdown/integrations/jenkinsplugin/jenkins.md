# [detect_product_short] Jenkins Plugin

The [detect_product_long] for Jenkins plugin enables you to install and run [detect_product_short] in your Jenkins instance. 

[detect_product_short] scans code bases in your projects and folders to perform compositional analysis and functions as a [bd_product_short] intelligent scan client. [detect_product_short] sends scan results to [bd_product_short], which generates risk analysis when identifying open source components, licenses, and security vulnerabilities.

[detect_product_short] is designed to run in the native build environment of the project that you want to scan. It uses the same global configuration as your Jenkins instance and provides a pass-through for [detect_product_short]. You can run as a post-build action in a Jenkins Freestyle job or run as a Pipeline step using a Pipeline script in a PipeLine job.
After running a [detect_product_short] scan following the Jenkins build, you can view the scan results in your [bd_product_short] instance.

Refer to [How it Works](../../gettingstarted/howitworks.md) to learn more about [detect_product_short].

## Basic workflow
1.      Make sure you satisfy system and other requirements.
*   Install the [detect_product_short] plugin in Jenkins.
*   Configure [blackduck_product_name] connection and plugin.

2.      Run a Jenkins build on your project.

3.      [detect_product_short] scans the project, for example, the scan might be a step in a Jenkins Pipeline job or post-build action in a Freestyle job.

4.      [detect_product_short] sends the scan results to [bd_product_short] for analysis.

After running a [detect_product_short] scan following the Jenkins build, you can view the scan results in your [bd_product_short] instance.
