# Azure Container Registry scanning with [detect_product_short]

[detect_product_long] supports scanning images stored in the Azure Container Registry (ACR). Image scan results are sent to your dedicated [bd_product_short] instance providing vulnerability, license, and operational risk results on the open source software components identified in the ECR image.

There are two ways that you can use [detect_product_short] to scan container images in ACR:

- Using an Azure DevOps Pipeline, see [Azure DevOps (ADO) Plugin](../../integrations/azureplugin/azure.md)
- Using [detect_product_short] on a local workstation

## Prerequisites

Azure Container Registry Scanning requires a fully configured instance of [detect_product_short].   
   
For prerequisite information refer to [Requirements and release information](../../gettingstarted/requirements.md)
	
## [detect_product_short] ACR scanning on a local workstation

Before you can scan images in ACR using [detect_product_short], ensure that you satisfy the following requirements:

- One or more container images stored in ACR. 
	- For more information about publishing and storing images in ACR, refer to the [container registry topic about pushing images](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-get-started-docker-cli).
- Azure CLI is installed
- Docker is installed

<note type="tip">To run [detect_product_short], you will need to provide login credentials for your [bd_product_short]
server by adding the following arguments to the command line:

* `--blackduck.url={your [bd_product_short] server URL}`
* `--blackduck.api.token={your [bd_product_short] access token}`
</note>

To locally scan container images stored in ECR, follow these steps:

1\. Authenticate with ACR.   
The *az acr login* command generates an authentication token and authenticates with your registry.   
  
**Generate Docker Login for ECR (Linux)**   

``` az acr login --name <acrName> ```  

2\. Invoke [detect_product_short], and provide the following paramaters at a minimum.

**[detect_product_short] - Scanning Images**   
```
bash <(curl -s -L https:‎ //detect.blackduck.com/detect10.sh) \
--blackduck.url=<URL> \
--blackduck.api.token=<token> \
--detect.docker.image=<Image URI> \
--detect.project.name=<Project Name>
```

## Invoking [detect_product_short] as a script to scan a Docker image stored in ACR

If you would rather run [detect_product_short] as a script than an extension, follow these steps:

In this example, follow the steps to create your first application using the [Azure Portal.](https://docs.microsoft.com/en-us/azure/devops/pipelines/get-started-azure-devops-project?view=vsts)   
 
From the available options, select: **Node.js sample app > Simple Node.js app > Web App for Containers.**   
 
You must authenticate with ACR; refer to [Authenticate with Azure Container Registry](https://docs.microsoft.com/en-us/azure/container-registry/container-registry-authentication).

Start in **Pipelines > Library** inside Azure DevOps. 

1\. Refer to [Variable Groups for Builds and Releases](https://docs.microsoft.com/en-us/azure/devops/pipelines/library/variable-groups?view=vsts) page for how to create a Variable Group. 

2\. Create a variable group for your [bd_product_short] instance:

- blackduck.url (value is the url of your [bd_product_short] instance).

- blackduck.api.token (value is your generated API token, secret).

3\. Create a second variable group for your ACR Credentials:
- acr.username (value is your ACR username).

- acr.password (value is your ACR password).

4\. Access your build(CI) pipeline by expanding the **Pipelines** sidebar item, and then choosing **Builds**.  

5\. Select the Pipeline you want to add [detect_product_short] to, then click **Edit**.   

6\. Link your variable groups by following the steps in [Use a Variable Group](https://docs.microsoft.com/en-us/azure/devops/pipelines/library/variable-groups?view=vsts#use-a-variable-group).  
 
7\. Add a Pipeline task for running [detect_product_short]:   
- After you click **Edit**, the **Tasks** screen of your CI Pipeline opens.
- In the **Build** task, click the plus (**+**) sign to add a new task.
- Use the search bar to search for bash.
- Click **Add** to add the step to your pipeline.

8\. Configure the bash step to run after the image has been pushed to ACR.   
- Select to run an inline script.
- Reference the following example for the script to run [detect_product_short].   

```
#/bin/bash
#Log in to ACR using the configured Variable Group
docker login <registryname>.azurecr.io -u $(acr.username) -p $(acr.password)
#Call Detect, passing the Docker Image location
bash <(curl -s -L https:‎ //detect.blackduck.com/detect10.sh) \
--blackduck.url=$(blackduck.url) \
--blackduck.api.token=$(blackduck.api.token) \ 
--detect.docker.image=<registryname>.azurecr.io/<containername>:$(Build.BuildId) \ 
--detect.project.name=$(Build.DefinitionName) \ 
--detect.project.version.name=$(Build.BuildNumber)
```
9\. Save and Queue the Pipeline, and then view the Pipeline Run Results.  

10\. View Scan Results in your instance of [bd_product_short].
