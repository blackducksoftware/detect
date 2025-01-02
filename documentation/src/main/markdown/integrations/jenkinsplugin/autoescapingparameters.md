# Auto-escaping Parameters

In [detect_product_long] for Jenkins, several special parameters are automatically escaped. 
The workflows pertaining to quotation marks and spaces are as follows.

- Detect properties must be separated by spaces or carriage returns/line feeds.
- Values containing spaces must be surrounded by either single or double quotation marks ('single' or "double") for Linux and Mac agents while for Windows you must use single quotes ('single').
- Values containing single quotes must be surrounded with double quotation marks.
- Values containing double quotes must be surrounded with single quotation marks.

## Considerations for name escaping conventions

You can turn off auto escaping by setting the environment variable *DETECT\_PLUGIN\_ESCAPING* to false.
Jenkins enables you to set an environment variable at different levels, such as globally or on a per-job basis. If you set the environment variable globally to one value, you can set it at the job level to another value. It is recommended to set the environment variable globally to skip escaping (ensuring past jobs work as expected), and then if you want to make jobs with the auto escaping enabled, you modify the environment variable flag in that job's configuration to enable escaping the characters. The easiest way to accomplish this is to install the ["Environment Injector" Jenkins plugin](https://plugins.jenkins.io/envinject/).

**Note:** In [detect_product_long] plugin version 10, the above recommendations remain the same for agents on Windows systems.  For those agents running on 'NIX systems, *DETECT\_PLUGIN\_ESCAPING* should be set to false.  Ensure that you adhere to the quoting conventions described above. Any input with spaces in the Jenkins configuration should be enclosed in quotes.

[detect_product_long] for Jenkins allows some special characters when *DETECT\_PLUGIN\_ESCAPING* is set to false, and spaces can be included without escape sequences provided that they are enclosed in single or double quotes as described above for different agents. Therefore, instead of `My\ Test\ Project1`, you can pass it as `'My Test Project1'`, the project will be created and uploaded to [bd_product_short] as `My Test Project1*.*`

If *DETECT\_PLUGIN\_ESCAPING* is set to true, then you can use backtick (\`) to escape spaces which are passed within argument values. Therefore, you can pass values as "Windows` Project" in the arguments.

