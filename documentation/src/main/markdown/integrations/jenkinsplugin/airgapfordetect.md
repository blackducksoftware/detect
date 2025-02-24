# Jenkins Air Gap mode
The [detect_product_long] for Jenkins plugin enables you to configure an air gap option to run [detect_product_short]. 

Before you can see the **[detect_product_short] Air Gap** option on the Global Tool Configuration page, you must install the [detect_product_short] plugin.

Use the following process to make the **[detect_product_short] Air Gap** option globally available when you're configuring a [detect_product_short] job:

1. In Jenkins, Click **Manage Jenkins** on the left navigation and then click  **Global Tool Configuration**.
1. In the **[detect_product_short] Air Gap** section, click **Add Detect Air Gap** and then complete the following:
   1. **[detect_product_short] Air Gap Name**: A name for the air gap installation.
   1. **Installation directory**: The directory for the air gap installation files.
   1. **Install automatically**: Select this checkbox to enable Jenkins to install the air gap files on demand.

When you check this option, you have to configure an installer for this tool, where each installer defines how Jenkins will attempt to install this tool.

For a platform-dependent tool, multiple installer configurations enable you to run a different setup script depending on the agent environment, but for a platform-independent tool such as Ant, configuring multiple installers for a single tool wouldn't be suggested.

   <figure>
    <img src="../jenkinsplugin/images/AirGap.png"
         alt="Air Gap mode">
    <figcaption>Air Gap mode.</figcaption>
</figure>

1. Optionally, add another air gap version. You can use the **Add Installer** menu to choose other install methods such as **Run Batch Command** or **Run Shell Command**.
1. Click **Save**.
