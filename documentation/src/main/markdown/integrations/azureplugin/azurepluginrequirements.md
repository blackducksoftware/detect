# Requirements for Azure DevOps

The following is a list of requirements for the [detect_product_long] in Azure DevOps integration.

* [bd_product_long] server.
  For the supported versions of [bd_product_short], refer to [Black Duck Release Compatibility](https://synopsys.atlassian.net/wiki/spaces/INTDOCS/pages/177799187/Black+Duck+Release+Compatibility).
* [bd_product_short] API token to use with Azure.
* Azure DevOps Services or Azure DevOps Server 17 or later
* Java.
  OpenJDK versions 8 and 11 are supported. Other Java development kits may be compatible, but only OpenJDK is officially supported for [bd_product_short].
* Access to the internet is required to download components from GitHub and other locations.

The [detect_product_short] plugin for Azure DevOps is supported on the same operating systems and browsers as [bd_product_short].

For scanning NuGet projects, verify that you have the NuGet tool installer set up in the build job definition.  For further information see [NuGet tool](https://learn.microsoft.com/en-us/azure/devops/pipelines/tasks/tool/nuget?view=azure-devops&viewFallbackFrom=vsts%3Fview%3Dvsts)

You can get the [detect_product_short] for Azure DevOps plugin at [VisualStudio Marketplace](https://marketplace.visualstudio.com/items?itemName=synopsys-detect.synopsys-detect).
