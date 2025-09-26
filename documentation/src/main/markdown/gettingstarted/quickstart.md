# [detect_product_short] Quickstart guide

The following is a simple example to help you get started using [detect_product_long].

<note type="hint">For another quick path to scanning with [detect_product_short], see [Autonomous Scanning](../runningdetect/autonomousscan.dita).</note>

## Step 1: Locate or acquire a source code project on which you will run [detect_product_short].

To run [detect_product_short] on junit4, which is an open source project written in Java and built with Maven, you can acquire junit4 by running the following commands:
```
git clone https://github.com/junit-team/junit4.git
cd junit4
```

To understand what [detect_product_short] does, it can be helpful to think about what you would do if you wanted to discover a project's dependencies without using [detect_product_short]. You might do the following:

1. Look in the project directory (junit4) for hints about how dependencies are managed. In this case, the *mvnw* and *pom.xml* files are hints that dependencies are managed using Maven.
1. Since it's a Maven project, you would likely run `./mvnw dependency:tree` to reveal the project's dependencies; both direct and transitive.
1. Examine files in the project directory, which might identify additional dependencies not known to the package manager such as a .jar file copied in.

This is essentially the process that [detect_product_short] expands upon and automates when it executes project manager tools and runs the [blackduck_signature_scanner_name] on the directory. Using detectors, inspectors, and other tools, [detect_product_short] may discover not only dependencies managed at the package level, but additional dependencies added to the project by means other than the package manager.

## Step 2: Run [detect_product_short] connected to [blackduck_product_name].

<note type="note">Downloading and running the latest unversioned `detect.sh/ps1` script will use the latest version of the [detect_product_short] .jar file, whereas running a versioned script such as `detect11.sh/ps1` will use the latest version of the [detect_product_short] .jar file within that specific major version.</note>  

To run [detect_product_short], you will need to provide login credentials for your [bd_product_short]
server. One way to do that is to add the following arguments to the command line:

* `--blackduck.url={your [bd_product_short] server URL}`
* `--blackduck.api.token={your [bd_product_short] access token}`

The command you run looks like this:

On Linux or Mac:
bash <(curl -s -L https://detect.blackduck.com/detect10.sh) --blackduck.url={your Black Duck SCA server URL} --blackduck.api.token={your Black Duck access token}


On Windows:
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect10.ps1?$(Get-Random) | iex; detect" --blackduck.url={your Black Duck SCA server URL} --blackduck.api.token={your Black Duck access token}


The operations performed by [detect_product_short] depends on what it finds in your source directory.
By default, [detect_product_short] considers the current working directory to be your source directory.

In the junit4 case, [detect_product_short] will:

1. Run the Maven detector, which uses Maven to discover dependencies.
2. Run the [blackduck_signature_scanner_name] which scans the files in the source directory to discover dependencies.
3. Upload the discovered dependencies to [bd_product_short].
4. Add a log entry for the [bd_product_long] Project BOM URL that you can use to view the results in [bd_product_short].

Once the scan is complete, navigate with your browser to the [bd_product_short] Project BOM URL to see the Bill Of Materials for junit4.  

For guidance on getting started using, and viewing results in [bd_product_short], check out [Getting Started with Black Duck SCA](https://documentation.blackduck.com/bundle/bd-hub/page/Administration/Hub101.html)

## Next steps

[detect_product_short] can be used on a variety of project types, and in a variety of ways, due to it's behavior being highly configurable.
For more detailed information on how to configure [detect_product_short] for your needs, see [Configuring Detect](../configuring/overview.md).
