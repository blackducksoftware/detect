# Project settings via JSON

The `detect.project.settings` property allows for submission of several project related properties to [detect_product_short] in one JSON file. 

The JSON file can include a subset of fields supported by [bd_product_short] SCA for the projects and versions API endpoints.

<note type="note">For more information about [bd_product_short] SCA API endpoints, please refer to the REST API Developers Guide available via the [bd_product_short] SCA UI.</note>

Adding the following parameter to a run of [detect_product_short] allows you to use a JSON file for specifying multiple project-related property settings:
````
--detect.project.settings=<Path to .json file containing project property settings>
````

The `.json` file should contain a JSON object with any of the following `detect.project` properties, linked to their respective documentation content, specified as key-value pairs:
* [detect.project.name](../properties/configuration/project.md#project-name)
* [detect.project.description](../properties/configuration/project.md#project-description)
* [detect.project.tier](../properties/configuration/project.md#project-tier)
* [detect.project.level.adjustments](../properties/configuration/project.md#allow-project-level-adjustments-advanced)
* [detect.project.clone.categories](../properties/configuration/project.md#clone-project-categories-advanced)
* [detect.project.deep.license](../properties/configuration/project.md#deep-license-analysis) 
* [detect.project.version.name](../properties/configuration/project.md#version-name)
* [detect.project.version.nickname](../properties/configuration/project.md#version-nickname)
* [detect.project.version.phase](../properties/configuration/project.md#version-phase)
* [detect.project.version.distribution](../properties/configuration/project.md#version-distribution-advanced)
* [detect.project.version.update](../properties/configuration/project.md#update-project-version)

<note type="important">`detect.project` properties specified on the command line take precedence over values specified in the JSON file.</note>

## JSON file example
```
{
  "name": "project-name",                 // detect.project.name
  "description": "project description",   // detect.project.description
  "projectTier": 3,                       // detect.project.tier
  "projectLevelAdjustments": true,        // detect.project.level.adjustments
  "cloneCategories": "ALL",               // detect.project.clone.categories
  "deepLicenseDataEnabled": true,         // detect.project.deep.license
  "versionRequest": {
    "versionName": "1.0.0",               // detect.project.version.name
    "nickname": "nickname",               // detect.project.version.nickname
    "phase": "DEVELOPMENT",               // detect.project.version.phase
    "distribution": "EXTERNAL",           // detect.project.version.distribution
    "update": false                       // detect.project.version.update
  }
} 
```
