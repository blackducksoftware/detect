# Yarn support

## Related properties

[Detector properties](../properties/detectors/yarn.md)

## Overview

[detect_product_short] runs the Yarn detector if it finds both of the following files in your project:

* yarn.lock
* package.json

The Yarn detector reads both `yard.lock`, and `package.json` files, (see *Yarn workspace support* below for information on other files the Yarn detector might read) to derive project and dependency information.   
* The yarn.lock file must be up-to-date before [detect_product_short] runs.   
* The package.json file specifies the direct dependencies for the project. [detect_product_short] adds these
dependencies to the top level of the dependency graph that it builds.   
The yarn.lock file contains necessary details about those
direct dependencies and their transient dependencies, enabling [detect_product_short]
to build the complete graph of direct and transient dependencies.

<note type="note">If any definition has a dependency as a non-dev direct or transitive reference, it and any transitives cease to be dev-only dependencies. If a component is a transitive to a non-dev dependency, it ceases to be a dev-only dependency, irrespective of any explicit references as a dev dependency.</note>

## Yarn workspace support

In addition to the codelocation generated for the project (showing its direct
and transitive dependencies),
[detect_product_short] also generates a codelocation per included workspace
(all workspaces are included by default).

### Referencing workspaces

When you use the workspace exclude/include properties, refer to workspaces
the same way Yarn refers to them in the *workspaces* list in the declaring package.json file:
use the relative path of the workspace directory (relative to the declaring workspace).

For example, if your project package.json contains:
````
"workspaces": [
"packages/workspace-a",
"packages/workspace-b"
],
````
[detect_product_short] will expect you to refer to these workspaces as "packages/workspace-a" and "packages/workspace-b".
This naming convention remains the same even at deeper workspace nesting levels. As an example, if your project
has a workspace packages/workspace-a, and packages/workspace-a's package.json contains:
````
"workspaces": [
"child1-of-workspace-a",
"child2-of-workspace-a"
],
````
[detect_product_short] will expect you to refer to these workspaces as "child1-of-workspace-a" and "child2-of-workspace-a".

### Excluding workspaces

By default, [detect_product_short] includes all workspaces in the results regardless of whether they
are declared as dependencies of the project.
You can specify a subset of workspaces to include or exclude using the workspace exclude/include properties
*detect.yarn.excluded.workspaces* and *detect.yarn.included.workspaces*.

When using the workspace exclude and include properties, use the workspace
referencing guidelines described above. You can also use
filename globbing-style wildcards and specify multiple values separated
by commas.

### Enable workspace ignore

To speed up scanning by building the dependency graph without analysis of workspaces, 
set the parameter`--detect.yarn.ignore.all.workspaces=true`. The default setting 
for this parameter is false and must be set to true to enable.

Dependencies in workspaces that are not in the Yarn lock file will not be included 
in the Bill of Materials.

If the Yarn lock file has been generated to include non-production dependencies,
with the command `yarn install --production=false` as an example, then those dependencies 
will be included in the Bill of materials.

<note type="note">The properties `--detect.yarn.dependency.types.excluded=NON_PRODUCTION`, 
`detect.yarn.excluded.workspaces` and `detect.yarn.included.workspaces` do not apply 
if `detect.yarn.ignore.all.workspaces=true` has been set.</note>

See [Yarn monorepo](https://yarnpkg.com/advanced/lexicon#monorepo) for further 
information about workspaces and monorepo configuration.
