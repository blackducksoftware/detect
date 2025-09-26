# [detect_product_long] Version Management

[detect_product_short] self-updating feature will allow customers who choose to enable Centralized [detect_product_short] Version Management in [bd_product_short] to automate the update of [detect_product_short] across their pipelines.

## Self updating [detect_product_short] scenarios

The Self Update feature will call the `/api/tools/detect` API end point to check for the existence of a specified [detect_product_short] version in [bd_product_short] under the **Admin > System Settings > [detect_product_short] > [detect_product_short] Version** drop-down. If a version that is eligible for upgrade or downgrade has been specified, the request to download that version of the [detect_product_short] .jar will execute and the current run of [detect_product_short] will invoke it for the requested scan. 

[detect_product_short] will download the required version from the repository when the service is hosted, or from a custom URL as configured in [bd_product_short], when internally hosted. To support self-update via internal hosting, the [detect_product_short] binary must be in a location accessible via https to all executing [detect_product_short] instances.   
Centralized [detect_product_short] Version Management feature support in [bd_product_short] is available from [bd_product_short] version 2023.4.0 onwards.

<!-- Variables do not resolve when in a note format hence the hardcoding below -->
<note type="information">
<ul>
<li>
If the Black Duck **Internally Hosted** option has been configured, Detect will be downloaded via https on the client side from the fully formatted URL specified under the "Hosting Location for Detect" setting. This setting over-rides version and integrity checks that would otherwise be performed by Detect.
<li>
If the Black Duck **Hosted** option has been configued, Detect will be downloaded on the client side from the repository.
</li>
</ul>
</note>

## Scenarios where [detect_product_short] self update will not execute

If there exists no mapping in [bd_product_short], or if the current version of [detect_product_short] matches the mapped version in [bd_product_short], or any issue occurs during the execution of the Self Update feature, then [detect_product_short] will continue with the current version to execute the scan.

If the [detect_product_short] URL of the [detect_product_short] .jar file to download and run has been hardcoded via [detect_product_short] property `DETECT_SOURCE` environment variable or the [detect_product_short] version set by the `DETECT_LATEST_RELEASE_VERSION` or `DETECT_VERSION_KEY` variables, self update will not occur. These are optional System environment properties used by [detect_product_short] upgrade scripts.

If the [bd_product_short] “Internally Hosted” option has been selected and a [detect_product_short] download location has not been provided, the feature will not be enabled.

For further [bd_product_short] configuration information, refer to the documentation provided under the topic:
<xref href="DetectLocation.dita" scope="peer"> Hosting location for [detect_product_short].
<data name="facets" value="pubname=bd-hub"/>

## [detect_product_short] log examples for self update

Downgrade to prior version blocked:  

``` 
2024-10-31 12:20:57 EDT INFO  \[main] - Detect-Self-Updater:  Checking https://test1.blackduck‎.com/api/tools/detect API for centrally managed Detect version to download to /Users/testuser/tmp.   

2024-10-31 12:21:03 EDT WARN  \[main] - Detect-Self-Updater:  The Detect version 8.7.0 mapped at Black Duck SCA server is not eligible for downgrade as it lacks the self-update feature. The self-update feature is available from 8.9.0 onwards.
```

Update to version allowed (8.9.0+):   

```
2024-10-31 12:33:52 EDT INFO  \[main] - Detect-Self-Updater:  Checking https://test1.blackduck‎.com/api/tools/detect API for centrally managed Detect version to download to /Users/testuser/tmp.  

2024-10-31 12:33:53 EDT WARN  \[main] - Detect-Self-Updater:  The Detect version 10.0.0 mapped at Black Duck SCA server is eligible for downgrade from the current version of 10.0.1. The self-update feature is available from 8.9.0 onwards.

2024-10-31 12:33:53 EDT INFO  \[main] - Detect-Self-Updater:  Centrally managed version of Detect was downloaded successfully and is ready to be run: /Users/testuser/tmp/detect-10.0.0.jar.
```

Current version of [detect_product_short] matches the mapped version or there is no mapped version in [bd_product_short]:   

```
2024-10-31 12:33:52 EDT INFO  \[main] - Detect-Self-Updater:  Checking https://test1.blackduck‎.com/api/tools/detect API for centrally managed Detect version to download to /Users/testuser/tmp.  

2024-10-31 12:33:53 EDT INFO  \[main] - Detect-Self-Updater:  Present Detect installation is up to date - skipping download.
```
<!-- Variables do not resolve when in a note format hence the hardcoding below -->
<note type="important">
<ul>
<li>
Downgrading to versions earlier than 8.9.0 is not supported. 
</li>
<li>  
This feature is not available in offline 'blackduck.offline.mode=true' or air gap configurations or if the [bd_product_short] URL has not been provided via the `blackduck.url` variable.
<li>
When running an "Internally Hosted" instance of [detect_product_short] and using custom scripts, checks should be made to prevent [detect_product_short] from querying [bd_product_short] for version management and re-downloading itself.
<li>
Self update makes it easy to switch to a new major [detect_product_short] version, so care should be taken to validate that automated scanning is not impacted.
</li>
</ul>
</note>

