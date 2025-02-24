# Configuring a Build Agent
To configure a build agent in your pipeline do the following under the **Tasks** tab on your pipeline page.

The default option for the build agent is the Microsoft hosted agent. To be able to select a self-hosted agent, you must have installed the agent and ensure that it's available to your project before you can use it in your pipeline. Click the ellipsis (**…**) next to Pipeline to Add an agent job.

* Click the ellipsis (**…**) next to Pipeline to Add an agent job.

   <figure>
    <img src="../azureplugin/images/configuringagent.png"
         alt="Configuring an Agent">
    <figcaption>Configuring an Agent</figcaption>
</figure>

* On the Agent job configuration screen, do the following:
   * Select a self-hosted agent from your Agent pool or select Azure Pipelines for an Azure-hosted agent.
   * If you select a hosted agent, then you must select an operating system such as macOS, Windows, or a version of Linux for the hosted agent VM.
   
<note type="tip">This is not an air gap option as internet connections are still required for downloading other tools and the script will still download new content if needed.</note>

<note type="note">If the agent is behind a proxy, [detect_product_short] Azure plug-in will utilize the agent proxy by default.</note>

## Configuring with a proxy

You can configure the build agent for [detect_product_short] Azure Plugin to use a proxy when running jobs.

### Proxy configuration scenarios

* If both an agent proxy and [bd_product_short] Poxy Service Endpoint are set through ADO Plugin parameter, the [bd_product_short] proxy url endpoint takes precedence.

* If agent proxy is configured, and the [bd_product_short] Poxy Service Endpoint is not set through ADO Plugin parameter, the [detect_product_short] Azure Plugin utilizes the agent proxy.
