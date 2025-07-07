# Risk Report Generation via [detect_product_short]

[detect_product_long] can generate a [bd_product_long] risk report in PDF and JSON format.
[detect_product_short] looks for risk report generation details in the properties whose names start with detect.risk.report, including:

* detect.risk.report.pdf (enable report generation in pdf format by setting to "true")
* detect.risk.report.pdf.path (path where the generated pdf report will be located)
* detect.risk.report.json (enable report generation in json format by setting to "true")
* detect.risk.report.json.path (path where the generated json report will be located)

## Fonts

Default font files are used to create the risk report pdf. 

You may specify a custom regular font and/or a custom bold font by placing a .ttf font file in a directory called "custom-regular" and/or "custom-bold", respectively, that is a child to the directory at ```detect-output-directory/tools/fonts```, where 'detect-output-directory' is determined by [detect.output.path](../properties/configuration/paths.md#detect-output-path)

Examples

* ```/path-I-passed-to-detect-output-path/tools/fonts/custom-regular/my-custom-regular-font.ttf```
* ```/Users/user/blackduck/tools/fonts/custom-regular/my-custom-regular-font.ttf``` on Unix
* ```C:\Users\blackduck\tools\fonts\custom-bold\my-custom-bold-font.ttf``` on Windows

## File Naming

When generating the risk report file, non-alphanumeric characters separating portions of the project name or version will be replaced with underscores. For example, in a case with hyphens and periods like "Project-Name" and "Project.Version.Name", the resulting file name would be ```Project_Name_Project_Version_Name_BlackDuck_RiskReport.pdf```

### Air Gap

Normally, font files used in creating the risk report PDF are downloaded from Artifactory. If you are using the [detect_product_short] air gap zip, the font files are retrieved from a directory called 'fonts' that is a child to the root of the air gap directory

To specify custom fonts when using the [detect_product_short] air gap zip, you must unzip the produced airgap zip file and then place a .ttf font file in a directory called "custom-regular" and/or "custom-bold" that is a child to the directory airGapRoot/fonts.

Example

* ```detect-detect_version-air-gap/fonts/custom-regular/my-custom-regular-font.ttf```
