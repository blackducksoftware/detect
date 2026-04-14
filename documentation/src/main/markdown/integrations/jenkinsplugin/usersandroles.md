# Users and roles for Jenkins Plugin
First you must configure a user/API token in [bd_product_short] so that the [detect_product_short] findings are analyzed in [bd_product_short].

## Generating an API token
1. Log in into your [bd_product_short] instance.
2. From the user menu located on the top navigation bar, select My Access Tokens. The **My Access Tokens** page appears.
4. Click **Create New Token**. The Create New Token dialog box appears
5. Type your name in the **Name** field.
6. Optional: in the **Description** field, you can type a description or definition.
7. Select **Read and Write Access**.
8. Click **Create**. The API token displays in a pop-up window. For security reasons, this is the only time your user API token displays. Please save this token. If the token is lost, you must regenerate it.
9. Optional: To modify an access token that you created, click the arrow in the same row as the access
token name to open a drop-down menu and select **Edit**, **Delete**, or **Regenerate**.
10. Configure the plugin with your [bd_product_short] url and the API token you just generated.

The following user roles are required for the user that you create in [bd_product_short]:

| Role     | Action |
| ----------- | ----------- |
| Project Creator     | Creates [bd_product_short] projects |
| Project Code Scanner   | Populates project BOM<br>Global Code Scanner can also be used to populate Project BOM |
| Global Code Scanner   | Populates the project BOM, generates reports, checks for policy violations. |
| Project Manager     | Generates reports |

**Note:** A user with the Global Code Scanner overall role can generate a report, but cannot delete the report. The Project Manager project role is required to delete the report.
