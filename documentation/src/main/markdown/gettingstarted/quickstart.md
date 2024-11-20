# Quickstart guide

The following is a simple example to help you get started using [detect_product_long].

<note type="hint">For another quick path to scanning with [detect_product_short], see [Autonomous Scanning](../runningdetect/autonomousscan.dita).

## Step 1: Locate or acquire a source code project on which you will run [detect_product_short].

To run [detect_product_short] on junit4, which is an open source project written in Java and built with Maven, you can acquire junit4 by running the following commands:
```
git clone https://github.com/junit-team/junit4.git
cd junit4
```

To understand what [detect_product_short] does, it can be helpful to think about what you would do if you wanted to discover a project's dependencies without using [detect_product_short]. You might do the following:

1. Look in the project directory (junit4) for hints about how dependencies are managed. In this case, the *mvnw* and *pom.xml* files are hints that dependencies are managed using Maven.
1. Since it's a Maven project, you would likely run `./mvnw dependency:tree` to reveal the project's dependencies; both direct and transitive.

This is basically what [detect_product_short] does on this project. In addition, [detect_product_short] runs the
[blackduck_signature_scanner_name] on the directory, which may discover additional dependencies
added to the project by means other than the package manager.

## Step 2: Run [detect_product_short] connected to [blackduck_product_name].

To run [detect_product_short], you will need to provide login credentials for your [bd_product_short]
server. One way to do that is to add the following arguments to the command line:

* `--blackduck.url={your [bd_product_short] server URL}`
* `--blackduck.api.token={your [bd_product_short] access token}`

The command you run looks like this:

On Linux or Mac:

````
bash <(curl -s -L https://detect.blackduck.com/detect10.sh) --blackduck.url={your Black Duck server URL} --blackduck.api.token={your Black Duck access token}
````

On Windows:

````
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect10.ps1?$(Get-Random) | iex; detect" --blackduck.url={your Black Duck server URL} --blackduck.api.token={your Black Duck access token}
````

The operations performed by [detect_product_short] depends on what it finds in your source directory.
By default, [detect_product_short] considers the current working directory to be your source directory.

In the junit4 case, [detect_product_short] will:

1. Run the Maven detector, which uses Maven to discover dependencies.
2. Run the [blackduck_signature_scanner_name] which scans the files in the source directory to discover dependencies.
3. Upload the discovered dependencies to [bd_product_short].
4. Provide in the log a Black Duck Project BOM URL that you can use to view the results in [bd_product_short].

Point your browser to the Black Duck Project BOM URL to see the Bill Of Materials for junit4.

## Next steps

[detect_product_short] can be used on a variety of project types, and in a variety of ways, due to it's behavior being highly configurable.
For more detailed information on how to configure [detect_product_short] for your needs, see [Configuring Detect](../configuring/overview.md).

