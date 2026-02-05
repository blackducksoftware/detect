# User role requirements when running with [blackduck_product_name]

Any user can download [detect_product_short] and run a scan, however you must configure a user/API token in [bd_product_short] for the [detect_product_short] scan to be analyzed by [bd_product_short].  
 
For more information on creating a [bd_product_short] user token, please consult the documentation provided by [bd_product_short] under the topic:
<xref href="MyAccessTokens.dita" scope="peer"> Managing user access tokens.
<data name="facets" value="pubname=bd-hub"/></xref>   
   
   
**The following user roles are required for the user that you create in [bd_product_short]**     

* The user must have the Project Creator overall role in order to create [bd_product_short] projects.
* The user must have the Global Project Viewer overall role, or be a member of the project, in order to create [bd_product_short] project versions.
* The user must have the Project Code Scanner project role, or the Global Code Scanner overall role, in order to populate the project BOM.
