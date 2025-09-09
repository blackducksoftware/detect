# Introduction to [detect_product_long]

[detect_product_long] is an intelligent scan client that analyzes code in your projects and associated folders to perform compositional analysis. [detect_product_short] can be configured to send scan results to [bd_product_long], which generates risk analysis when identifying open-source components, licenses, and security vulnerabilities.

[detect_product_short] can be used in both connected and air gap modes depending on the types of scans being run.    

## [detect_product_short] characteristics.     

* [detect_product_short] integrates with development tools used throughout the SDLC (software development life cycle) and automatically detects resources to optimize its scan methodology.

* [detect_product_short] provides scanning capabilities for [bd_product_short] to help identify open-source components, licenses, and security vulnerabilities. This is achieved through a variety of detection methods such as package manager inspection, file system based signature scanning of source directories and files, Docker image inspection, and binary analysis.

* [detect_product_short] provides the source of information for [bd_product_short] to analyze open-source components and find vulnerabilities in open-source components and containers. Using this type of analysis, you can minimize security, compliance, and code quality risks; you can monitor for new vulnerabilities throughout your development cycle, and you can set and enforce open-source use and security policies.

* Runs on Windows, Linux, and macOS. It is available through GitHub, under the permissive Apache License, Version 2.0 and does not require pre-installation or extensive configuration.

* Supports scanning Docker images by identifying open-source libraries and code within the images, using both signature scanning and the package manager analysis techniques.    

## [detect_product_short] functionality consolidation.   

[detect_product_short] consolidates the functionality of [bd_product_short], package managers, and continuous integration plugin tools to perform the following tasks:  

* Discover open-source components in your code.

* Map components to known security vulnerabilities.

* Identify license compliance and component quality risks.

* Set and enforce open-source use and security policies.

* Integrate open-source management into your DevOps environment.

* Monitor and alert users when new security threats are reported.

* Calculate security vulnerability risk in your code.

* Produce reports of the open-source analysis findings.

* Provide malware information if identified.   

<note type="note">Some scan types require specific feature licenses to execute. Contact your [var_company_name] representative for further information.</note>

## How [detect_product_short] functions.   

When looking at vulnerabilities in open source and third-party software, [detect_product_short] performs the following basic steps:

* Uses the project's package manager to derive the hierarchy of dependencies known to that package manager. For example, on a Maven project, [detect_product_short] executes an mvn dependency:tree command and derives dependency information from the output.

* Runs the [bd_product_short] signature scanner on the project. This might identify additional dependencies not known to the package manager (for example, a .jar file copied into the project directory).

* Uploads both sets of results (dependency details) to [bd_product_short] creating the project/version if it does not already exist. [bd_product_short] uses the uploaded dependency information to build the Bill Of Materials (BOM) for the project/version.

* You can view the output and analysis results in [bd_product_short].    
