# How [detect_product_short] Works

This page provides an overview of how [detect_product_long] works.

[detect_product_short] performs the following basic steps when scanning open source software, assuming you are connected to a [bd_product_short] instance.

1. [detect_product_short] uses the project's package manager to derive the hierarchy of dependencies known to that package manager. For example, on a Maven project, [detect_product_short] executes an mvn dependency:tree command, and derives dependency information from the output.
1. Runs the [blackduck_signature_scanner_name] on the project. This might identify additional dependencies not known to the package manager such as a .jar file copied into the project directory.
1. Uploads both sets of results (dependency details) to [bd_product_short] creating the project/version if it does not already exist. [bd_product_short] uses the uploaded dependency information to build the Bill Of Materials (BOM) for the project/version.

In this case, the user has provided [bd_product_short] connection details through property settings to [detect_product_short], specifying that results (project dependency details) are to be uploaded to [bd_product_short]
By combining all these techniques, [detect_product_short] is capable of scanning a wide range of software projects
utilizing a variety of package managers and programming languages for open source components.
