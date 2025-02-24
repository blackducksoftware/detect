# Running [detect_product_long] in Jenkins

By default, [detect_product_short] for Jenkins downloads either the latest [detect_product_short] shell script when run on a UNIX node, or PowerShell script when it's run on a Windows node, to the Jenkins tools directory, and then executes that script. Note that you can also use the JAR option to run [detect_product_short].

The [detect_product_short] PowerShell or shell script is downloaded once and placed in the [detect_product_short] working directory. If you want to force the plugin to fetch the latest script, clear out the Detect directory in your Jenkins tools directory.

# **JAR option**
If you do not want to download [detect_product_short], you can manually put the JAR on the node where you want [detect_product_short] to run and specify the DETECT\_JAR environment variable that points to your provided JAR, and that JAR will be executed instead. 

To use the JAR option, perform the following steps:

1. Navigate to **Dashboard > Manage Jenkins > Configure System > Global properties > Environment variables**. 
1. Click **Add**.
1. Set an environment variable with the following properties:
   1. **Name**: `DETECT\_JAR`.
   1. **Value:** `<path to the Detect jar file on your Jenkins node>`.
   
**Note:** When your build runs, Jenkins looks for configured environment variables, and if it locates DETECT\_JAR, it uses that instead of pulling the latest [detect_product_short] shell script.
## Air Gap option
[detect_product_short] can be configured to run in an air gap fashion, see: [Air Gap](../../downloadingandinstalling/airgap.md).

In freestyle and Pipeline jobs, you can toggle between the different modes for running [detect_product_short] in the plugin such as pulling the Detect.jar from scripts or $DETECT\_JAR\_PATH, or from a specified Tool Installation.
## Running [detect_product_short] in a job
You can run [detect_product_short] as a post-build action or a Pipeline step.
### Pipeline step
You can configure the scan as a pipeline step in a Pipeline job.

Refer to the [pipeline example](../../integrations/jenkinsplugin/jenkinspipelinejob.md)
### Post-build actions
You can configure the scan as a post-build action in a freestyle job. You can have multiple post-build actions, but only one [detect_product_short] post-build action.

Refer to the [freestyle example](../../integrations/jenkinsplugin/jenkinsfreestylejob.md).
## DSL considerations
The [detect_product_short] for Jenkins plugin provides Dynamic DSL for both freestyle steps and pipeline steps. Read more at [Dynamic DSL](https://github.com/jenkinsci/job-dsl-plugin/wiki/Dynamic-DSL).

**Note:** that versions 1.83 and later of the DSL plugin do not support the [detect_product_short] for Jenkins plugin pipeline steps.
