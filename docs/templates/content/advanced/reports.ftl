# Risk Report Generation

${solution_name} can generate a Black Duck risk report in PDF form.
${solution_name} looks for risk report generation details in the properties whose names start with detect.risk.report,
including:

* detect.risk.report.pdf (enable report generation)
* detect.risk.report.path (path where report will be located)

## Fonts

Loaded font files are used to create the risk report pdf. 

You may specify a custom regular font and/or a custom bold font by placing a .ttf font file in a directory called "custom-regular" and/or "custom-bold", respectively, that is a child to the directory at detect-output-directory/tools/fonts, where 'detect-output-directory' is determined by [detect.output.path](../../properties/configuration/paths/#detect-output-path)

Examples

* /path-I-passed-to-detect-output-path/tools/fonts/custom-regular/my-custom-regular-font.ttf
* /Users/user/blackduck/tools/fonts/custom-regular/my-custom-regular-font.ttf on Unix
* C:\Users\blackduck\tools\fonts\custom-bold\my-custom-bold-font.ttf on Windows

### Air Gap

Normally font files used in creating the risk report pdf are downloaded from Artifactory. If you are using the ${solution_name} air gap, the font files are looked for in a directory called 'fonts' that is a child to the root of the air gap directory.

To specify custom fonts when using the ${solution_name} air gap zip, you must unzip the produced airgap zip file, and then place a .ttf font file in a directory called "custom-regular" and/or "custom-bold" that is a child to the directory airGapRoot/fonts.

Example

* synopsys-detect-detect_version-air-gap/fonts/custom-regular/my-custom-regular-font.ttf
