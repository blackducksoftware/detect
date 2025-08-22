# Maven support

## Related properties

[Detector properties](../properties/detectors/maven.md)

## Overview

[detect_product_short] has three detectors for Maven:

* Maven CLI
* Maven Wrapper CLI
* Maven Project Inspector

<note type="Note">

* Maven Project Inspector relies on Project Inspector thus does not accept Maven specific configuration properties.
</note>

## Maven CLI

* Discovers dependencies of Maven projects by executing mvn commands.

* Will run on your project if it finds any file matching the pattern *pom.xml in the top-level source directory and requires either `mvnw` or `mvn`.

1. [detect_product_short] looks for `mvnw` in the source directory (top level). You can override this by setting the Maven path property.  
1.  If `mvnw` path is not overridden and `mvnw` is not found:[detect_product_short] looks for mvn on $PATH.

The Maven CLI detector runs `mvn dependency:tree` to get a list of the project's dependencies and then parses the output.
[detect_product_short] assumes the output of this command will be in Maven's default logging format. Customizations of Maven's logging format can break [detect_product_short]'s parsing.

Scope inclusion/exclusion is performed during the parsing of the output of the `mvn dependency:tree` command.

### Components with unknown graph locations

The output of `mvn dependency:tree` does not always provide information
on a component's positions in the dependency graph. When this information is missing,
[detect_product_short] will place the component under a placeholder "component" named *Additional_Components*.

For example: Imagine a project with two scopes (compile and test), two direct dependencies A and B,
both of which depend transitively on C, and imagine we want [detect_product_short] to exclude test scope from
its output. The actual dependency graphs for this project look like:
````
compile scope:
A
\- C

test scope:
B
\- C
````

For this project, the output of `mvn dependency:tree` may only show:
````
compile scope:
A

test scope:
B
\- C (compile)
````
From that output we can tell that C is part of the compile scope, but there is no information about where in the compile scope
graph C belongs. Its position in the test scope is irrelevant since test scope is being excluded. Rather than excluding C in this case,
[detect_product_short] puts it under the placeholder "component" named *Additional_Components*.

### Maven Wrapper CLI

The Maven Wrapper CLI detector attempts to run on your project if it finds a pom.groovy file in the source directory (top level), and then operates exactly as the Maven CLI detector does.

### Maven Project Inspector

The Maven Project Inspector detector uses Project Inspector, which currently does not support plugins.
The Maven Project Inspector includes the shaded dependencies as part of the BOM.

As of [detect_product_short] 9.5.0 the version of Project Inspector in use supports the `--build-system MAVEN` argument in place of `--strategy MAVEN`.
The `--force-maven-repos "url"` argument will be removed from support in the next [detect_product_short] major release and replaced with the `--conf "maven.repo:url"` argument.