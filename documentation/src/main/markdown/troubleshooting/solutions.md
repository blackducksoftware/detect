# Common solutions

## DETECT_SOURCE was not set or computed correctly

### Symptom

detect10.sh fails with: DETECT_SOURCE was not set or computed correctly, please check your configuration and environment.

### Possible cause

detect10.sh is trying to execute this command:
````
curl --silent --header \"X-Result-Detail: info\" https://repo.blackduck.com/api/storage/bds-integrations-release/com/blackduck/integration/detect?properties=DETECT_LATEST_10
````
The response to this command should be similar to the following:
```
{
"properties" : {
"DETECT_LATEST_10" : [ "https://repo.blackduck.com/bds-integrations-release/com/blackduck/integration/detect/10.0.0/detect-10.0.0.jar" ]
},
"uri" : "https://repo.blackduck.com/api/storage/bds-integrations-release/com/blackduck/integration/detect"
}
```
When that command does not successfully return a value for property DETECT_LATEST, detect10.sh reports:
````
DETECT_SOURCE was not set or computed correctly, please check your configuration and environment.
````

<note type="note">[detect_product_short] releases prior to 10.0.0 will be located under https://repo.blackduck.com/bds-integrations-release/com/synopsys/integration/synopsys-detect</note>

### Solution

If the curl command described above does not successfully return a value for property DETECT_LATEST, you must determine why, and make the changes necessary so that curl command works.

## [detect_product_short] succeeds, but the results are incomplete because package managers or subprojects were overlooked

### Symptom

In this scenario, everything succeeds, but many or all components are missed. Examining the log shows that
package managers were not recognized and/or subprojects were overlooked.

### Possible cause

The detector search depth needs to be increased. The default value (0) limits the search for package manager files to the project directory. If project manager files
are located in subdirectories and/or there are subprojects, this depth should be increased to enable [detect_product_short] to find the relevant files, so it
will run the appropriate detector(s).

See [detector search depth](../properties/configuration/paths.md#detector-search-depth) for more details.

## [detect_product_short] fails and a TRACE log shows an HTTP response from [bd_product_short] of "402 Payment Required" or "502 Bad Gateway"

### Symptom

[detect_product_short] fails, and a TRACE log contains "402 Payment Required" or "502 Bad Gateway".

### Possible cause

[bd_product_short] does not have a required feature (notifications, binary analysis, etc.) enabled.

### Solution

Enable the required feature on the [bd_product_short] server.

## Unexpected behavior running [detect_product_short] on a project that uses Spring Boot

### Symptom

Unexpected behavior, and/or unexpected property values shown in the log.

### Possible cause

If your source directory contains Spring Framework configuration files named application.properties, application.yml,
or application.xml that are written for any application other than [detect_product_short], you should not run [detect_product_short] from your source directory.

### Solution

To prevent [detect_product_short] from reading those files, run [detect_product_short] from a different directory. Use the following property to point to your source directory.
```
--detect.source.path={project directory path}
```

## PKIX error connecting to [bd_product_short]

### Symptom

Exception: Could not communicate with [bd_product_short]: Could not perform the authorization request: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target

### Possible cause

The [bd_product_short] server certificate is not in Java's keystore.

### Solution

1. Acquire the certificate file for your [bd_product_short] server.
1. Determine which *java* executable is being used to run [detect_product_short]. If you run [bash_script_name], that is either $JAVA_HOME/bin/java (the default) or the first *java* found on your $PATH.
1. Determine the Java home directory for that *java* executable.
1. Run [keytool](https://docs.oracle.com/en/java/javase/11/tools/keytool.html) to install the [bd_product_short] server certificate into the keystore in that Java home directory.

Although not recommended, it is possible to disable the certificate check with the [trust cert property](../properties/configuration/blackduck-server.md#trust-all-ssl-certificates-advanced).

## Not Extractable: NUGET - Solution INFO \[main\] -- Exception occurred: java.nio.file.InvalidPathException

### Symptom

Running [detect_product_short] on a NuGet project on Windows, a message similar to the following appears in the [detect_product_short] log:

````
Not Extractable: NUGET - Solution INFO [main] -- Exception occurred: java.nio.file.InvalidPathException: Illegal char
<:> at index 2: C:\...
````

### Possible cause

The value of $PATH contains a whitespace character after a semicolon and the path mentioned in the log message.

### Solution

Remove spaces immediately following semicolons in the value of $PATH.

## No project name/version provided or derived

### Symptom

Upload to [bd_product_short] fails with a message similar to the following in the log:

````
ERROR [main] -- createProject.arg0.name can't be blank [HTTP Error]: There was a problem trying to POST https://.../api/projects, response was 412 Precondition Failed.
````

### Possible cause

No project name and version were provided via properties and no [detect_product_short] tool capable of deriving a project name and version was included in the run. For example,
you will get this (or a similar) error if you run with --detect.tools.BINARY_SCANNER and do not set --detect.project.name or --detect.project.version.name.

### Solution

Set --detect.project.name and --detect.project.version.name.

## [blackduck_signature_scanner_name] fails on Alpine Linux

### Symptom

The [blackduck_signature_scanner_name] fails on Alpine Linux with an error similar to:

````
There was a problem scanning target '/opt/projects/myproject': Cannot run program "/home/me/blackduck/tools/Black_Duck_Scan_Installation/scan.cli-2020.6.0/jre/bin/java": error=2, No such file or directory
````

### Possible cause

The Java bundled with the [blackduck_signature_scanner_name] does not work on Alpine Linux (it relies on libraries not usually present on an Alpine system).

### Solution

Install an appropriate version of Java and tell [detect_product_short] to invoke the [blackduck_signature_scanner_name] using that
version of Java by setting environment variable BDS_JAVA_HOME to the JAVA_HOME value for that Java installation.

For example:

````
export BDS_JAVA_HOME=$JAVA_HOME
````

Or:

````
export BDS_JAVA_HOME=/usr/lib/jvm/java-11-openjdk/jre
````

## Detector scan fails with Java compatibility issues

### Symptom

Detector scans may fail with an error indicating Java incompatibility.

### Possible cause

[detect_product_short] might use package manager executables and CLI commands to investigate projects and the Java version in use by [detect_product_short] might not be compatible with the Java version used for the project.

### Solution

Install an appropriate [detect_product_short] supported version of Java and configure [detect_product_short] to use that version of Java by setting environment variable JAVA_HOME value for that Java installation.

## On Windows: Error trying cleanup

### Symptom

When running on Windows, inspecting a Docker image (e.g. using --detect.docker.image or --detect.docker.tar),
during shutdown, [detect_product_short] logs messages similar to the following:
````
2020-08-14 14:31:04 DEBUG [main] --- Error trying cleanup:

java.io.IOException: Unable to delete file: C:\Users\Administrator\blackduck\runs\2020-08-14-21-28-40-106\extractions
...
Caused by: java.nio.file.FileSystemException: C:\Users\Administrator\blackduck\runs\2020-08-14-21-28-40-106\extractions\DOCKER-0\application.properties: The process cannot access the file because it is being used by another process.
````

### Possible cause

This happens when Docker fails to release its lock on the volume mounted directory when it shuts down the image inspector service container
due to [Docker for Windows issue 394](https://github.com/docker/for-win/issues/394).
The result is that [detect_product_short] cannot fully clean up its output directory and leaves behind empty subdirectories.
The problem may be intermittent.

### Solution

There is no harm in leaving the directories behind in the short term but we recommend periodically removing them if the problem occurs frequently.
Restarting Docker will force Docker to release the locks, and enable you to remove the directories.

## Encoding Problems with PIP Requirements File

### Symptom

`requirements.txt` files created using encoding systems other than UTF-8 cause certain Unicode characters in the component names to be unreadable when inspected through [detect_product_short]. The system does not recognize these component entries, resulting in unmatched components. 

Here is an example:
````
{
    "@id" : "http:pypi/%EF%BF%BD%EF%BF%BDa%00p%00p%00d%00i%00r%00s%00%3D%00%3D%001%00.%004%00.%004",
    "@type" : "https://blackducksoftware.github.io/bdio#Component",
    "https://blackducksoftware.github.io/bdio#hasName" : "��a\u0000p\u0000p\u0000d\u0000i\u0000r\u0000s\u0000=\u0000=\u00001\u0000.\u00004\u0000.\u00004",
    "https://blackducksoftware.github.io/bdio#hasVersion" : "",
    "https://blackducksoftware.github.io/bdio#hasIdentifier" : "��a\u0000p\u0000p\u0000d\u0000i\u0000r\u0000s\u0000=\u0000=\u00001\u0000.\u00004\u0000.\u00004",
    "https://blackducksoftware.github.io/bdio#hasNamespace" : "pypi"
}
````

### Possible cause

The requirements.txt file was created using encoding systems other than UTF-8.

### Solution

To resolve this issue, the requirements.txt file must be created using UTF-8 encoding before the [detect_product_short] inspection is run on the source code.

<note type="note">See [PIP uses UTF-8 as the default encoding when creating requirements.txt files](https://pip.pypa.io/en/stable/reference/requirements-file-format/#encoding).</note>
