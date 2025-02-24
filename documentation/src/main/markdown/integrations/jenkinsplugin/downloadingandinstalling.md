# Downloading, Installing, and Updating the Plugin

## Downloading and Installing a new instance
To install the [detect_product_short] for Jenkins plugin, perform the following steps:

1. Navigate to **Manage Jenkins** > **Manage Plugins**.
1. Select the **Available** tab. (Note that if the plugin is already installed, it does not appear in the **Available** list.)
1. Select **Blackduck [detect_product_short]**.
1. Click **Download now and install after restart**. This is the recommendation for installing the plugin.
1. After restarting Jenkins, confirm that the plugin is successfully installed by navigating to **Manage Jenkins** > **Manage Plugins > Installed**, and verify that **[company_name] [solution_name]** displays in the list.

[detect_product_short] plugin for Jenkins GitHub page [jenkinsci](https://github.com/jenkinsci/blackduck-detect-plugin).
Additional download locations listed in [Download locations](../../downloadingandinstalling/downloadlocations.html).

## Updating [company_name] [solution_name] Jenkins plugin to [detect_product_long] Jenkins plugin{#updating-existing-jenkins}
For existing [company_name] [solution_name] Jenkins plugin users, the [detect_product_long] Jenkins plugin should be considered a fresh installation as the domain has changed.   
* Take note of your existing system configuration and post-build setup before moving from the [detect_product_short] Jenkins plugin to the [detect_product_long] Jenkins plugin. You will need this information when configuring or reconfiguring your pipelines.    
   * Configuration information can be located under your `JENKINS_HOME` directory.   
* If you are utilizing a [detect_product_short] Post Build Step, before upgrading to the [detect_product_long] plugin, make sure to record the current configuration set in the configurable pipelines, for reuse.
* For Groovy, you will need to update the **Pipeline** script; under **Pipelines** > **Pipeline_Name** > **Configuration**, replacing the `synopsys_detect detectProperties:` portion of the script with `blackduck_detect detectProperties:`
   
   Example:
    ```
    node ('built-in') {

        stage ('Git - Checkout') {
        git 'https://github.com/yarnpkg/example-yarn-package.git'
        }
        stage ('Black Duck - Detect') {
        blackduck_detect detectProperties: '--blackduck.trust.cert=true --detect.wait.for.results=true', downloadStrategyOverride: [$class: 'ScriptOrJarDownloadStrategy']
            
        }
    }
    ```
   
* For the System Configuration and [bd_product_long], before upgrading to the [detect_product_long] plugin, make sure to back up, or record the current configuration set for the Global [bd_product_short] URL and token that you have set in Manage Jenkins > Configure System > [detect_product_long] section.
* If you are using Air Gap mode, before upgrading to the [detect_product_long] plugin, make sure to save the current tool configuration that you have set in Manage Jenkins > Tools > Detect Air Gap mode.

## Updating the [detect_product_long] for Jenkins plugin
You can update the [detect_product_short] for Jenkins plugin when new versions are released.

1. Navigate to **Manage Jenkins** > **Manage Plugins**.
1. Click the **Updates** tab.
1. Select **Blackduck Detect**
   1. If there are updates for the [detect_product_short] for Jenkins plugin, the updates display in the list. If there is not an available update, the [detect_product_short] for Jenkins plugin does not display in this list.
   1. Alternatively, you can force Jenkins to check for plugin updates by clicking **Check now** on the **Updates** tab.
1. If there are updates, select the one you want, and click **Download now and install after restart**.

<!-- Commenting out until the script is updated to support the current releases
## Migrating from plugin version 1.5.0
If you have already configured [company_name] [solution_name] for Jenkins across a number of jobs utilizing plugin version 1.5.0, Synopsys provides a script to make the process easier. The script automatically migrates the [company_name] [solution_name] for Jenkins post-build step in your FreeStyle jobs from the version 1.5.0 format to the new format. 
Run the script by copying and pasting it into the Jenkins Script Console, then clicking **Run**.  For more information, refer to [Jenkins console scripting](https://wiki.jenkins.io/display/JENKINS/Jenkins+Script+Console).

This script is run once by a Jenkins administrator after upgrading the plugin from 1.5.0. The script performs two transformations: one to the [company_name] [solution_name] system configuration, and the other to the post-build steps of FreeStyle jobs that were configured with version 1.5.0 of the plugin. The groovy script only makes changes to the FreeStyle jobs if the data migration is successful for that job because saving a Jenkins job erases unreadable data stored in that job. Therefore, Synopsys recommends migrating any other unreadable data from these jobs before running this script.

The migration script is available on [GitHub](https://github.com/jenkinsci/synopsys-detect-plugin/tree/master/groovy-scripts).

**Note:** Be careful if you use this script as it is an example that makes changes to jobs. -->
