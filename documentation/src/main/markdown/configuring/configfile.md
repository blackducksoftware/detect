# Using a configuration file

A common way to configure [detect_product_short] is to use a configuration file. The configuration file can be either a Java properties (`.properties`) file or a YAML (`.yml`) file.

<note type="note">
Configuration files support all properties; only the formatting, as noted below, changes based on the type of configuration file used. See [All Properties](../properties/all-properties.md).
</note>

Spring Boot looks for a configuration file named `application.properties` or `application.yml` in the current working directory or in the `./config` subdirectory. If a configuration file is found, Spring Boot reads its property values.

For example, if you want to set the `detect.project.name` property using a configuration (`.properties`) file, you can do so as follows:

````
echo "detect.project.name=myproject" &gt; application.properties
bash &lt;(curl -s -L https://detect.blackduck.com/detect12.sh) --detect.source.path=/opt/projects/project1
````

Because the configuration file has one of the file names that Spring looks for by default (in this case, `application.properties`) and exists in one of the locations that Spring looks in by default (in this case, the current directory), there is no need to specify the path to the configuration file on the command line.

Additional details can be found in the [Spring Boot documentation](https://docs.spring.io/spring-boot/docs/2.4.5/reference/html/howto.html#howto-externalize-configuration).

## Properties file

When setting a property value in a `.properties` file, do not prefix the property name with hyphens. Follow Java `.properties` file syntax: `propertyName=propertyValue`, with one property per line.

## YAML file

When setting a property value in a `.yml` file, do not prefix the property name with hyphens. Follow YAML dictionary syntax: `propertyName: propertyValue`, with one property per line.

The [detect_product_short] [command line help](../gettingstarted/gettinghelp.md) option `-hyaml` can be used to generate a template YAML configuration file.

## Running [detect_product_short] from a directory that contains a file named *config*

If the directory from which you run [detect_product_short] contains a file named *config*, you must override the default value of the Spring Boot property `spring.config.location`.

By default, Spring Boot may attempt to use *config* as a configuration directory. If *config* is a file instead of a directory, this can prevent [detect_product_short] from starting correctly.

Set `spring.config.location` according to whether you use a Spring Boot configuration file.

**If you use a Spring Boot configuration file**, such as `application.properties` or `application.yml`, set `spring.config.location` to one of the following:

- The directory containing the configuration file. Include a trailing slash (`/`) to indicate that the location is a directory.
- The path to the configuration file itself.

**If you do not use a Spring Boot configuration file**, set `spring.config.location` to an empty string:

````
--spring.config.location=""
````
