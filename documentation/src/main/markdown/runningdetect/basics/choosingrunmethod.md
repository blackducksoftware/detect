# Choosing a run method (script, .jar, or Docker container)

There are three ways to run [detect_product_long]:

1. Download and run a [detect_product_short] [script](runningscript.md).
1. Download and run a [detect_product_short] [.jar file](runningjar.md).
1. Run [detect_product_short] from [within a Docker container](../runincontainer.md).

# Scripts

Running one of the [detect_product_short] scripts provides the convenience of an auto-update feature, keeping you at the latest version with the associated improvements.     
Auto-update provides the following default behaviour:

Running the unversioned `detect.sh/ps1` script will use the latest version of the [detect_product_short] .jar file; downloading it for you if necessary.   
	
Running a versioned `detect.sh/ps1` script such as `detect11.sh/ps1` will use the latest version of the [detect_product_short] .jar file within that specific major version; downloading it for you if necessary.   
	
To override the auto-update functionality by specifying an exact [detect_product_short] version, see: [To run a specific version of Detect](runningscript.md#to-run-a-specific-version-of-detect).   
	
<note type="tip">When you run [detect_product_short] via one of the provided scripts, you automatically pick up fixes and new features as they are released.</note>

| [detect_product_short] version | Script Type | Script Name | Notes |
|---|---|-------------|---|
| Latest | Bash | detect.sh  | Runs latest Detect |
| Latest | PowerShell | detect.ps1 | Runs latest Detect |
| 11 | Bash | detect11.sh  | Runs latest Detect 11 |
| 11 | PowerShell | detect11.ps1 | Runs latest Detect 11 |
| 10 | Bash | detect10.sh  | Runs latest Detect 10 |
| 10 | PowerShell | detect10.ps1 | Runs latest Detect 10 |
| 9 | Bash | detect9.sh  | Runs latest Detect 9 |
| 9 | PowerShell | detect9.ps1 | Runs latest Detect 9 |

<note type="note">References to [detect_product_short] scripts within this documentation assume you are running the current release.</note>

# JAR file

The primary reason to run the [detect_product_short] .jar directly is that this method provides
direct control over the exact [detect_product_short] version. [detect_product_short] does not automatically update in this scenario.

# Docker container

The primary reason to run [detect_product_short] from within a Docker container is to take advantage of Docker container benefits, which include standardized run environment configuration. [detect_product_short] does not automatically update in this scenario.
