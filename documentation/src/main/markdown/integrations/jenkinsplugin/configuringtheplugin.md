# Configuring the Jenkins Plugin
Use the following process to configure the [detect_product_long] for Jenkins plugin.  Note that the supported credential format is API token.  SAML is not supported.

1. After installing, navigate to **Manage Jenkins** > **Configure System**.
1. Navigate to the **[detect_product_short]** section, and complete the following.
   1. **Global download strategy**: Depending on your desired deployment method, select either the option to **Install Air Gapped Detect as a Tool Installation** or **Download via scripts or use DETECT\_JAR** from the drop-down list.
   
   <figure>
    <img src="../jenkinsplugin/images/Configuring1.png"
         alt="Global download strategy Air Gap">
    <figcaption>Global download strategy Air Gap.</figcaption>
</figure>

   <figure>
    <img src="../jenkinsplugin/images/Configuring2.png"
         alt="Global download strategy Scripts/Jar">
    <figcaption>Global download strategy Scripts/Jar.</figcaption>
</figure>

1. **[bd_product_short] URL**: URL to your [bd_product_short] server instance.
1. **[bd_product_short] credentials**: To add credentials, click **Add** > **Jenkins**, and then select API Token as the credential that you want to add and populate the relevant fields.
   When you add credentials, you can select those credentials that you want from the drop-down menu to authenticate to the [bd_product_short] server. 
   1. For user API tokens, select **Secret text** from the menu in the **Kind** field, then provide your [bd_product_short] access token in the **Secret** field.
   <figure>
    <img src="../jenkinsplugin/images/Configuring3.png"
         alt="Inputting the access token secret">
    <figcaption>Input access token secret.</figcaption>
</figure>

1. The **Advanced...** option displays for [bd_product_short]. Advanced settings enable you to specify values for:
   1. **[bd_product_short] connection timeout** (in seconds).  The default value is 120.
   1. **Trust [bd_product_short] certificates**: Select the checkbox to allow (SSL) certificates from [bd_product_short].
   <figure>
    <img src="../jenkinsplugin/images/Configuring4.jpg"
         alt="Configure connection timeout and SSL">
    <figcaption>Configure timeout and SSL.</figcaption>
</figure>
1. Click **Test Connection to [bd_product_short]** to verify that your settings are correct. If so, a *Connection successful!* status displays.
1. Click **Save**.
