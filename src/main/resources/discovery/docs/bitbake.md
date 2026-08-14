# BitBake support

## Related properties

[Detector properties](../properties/detectors/bitbake.md)

## Requirements

The BitBake detector will run if it finds a BitBake build environment setup script (which defaults to *oe-init-build-env*, but can be configured
using property *detect.bitbake.build.env.name*)
and at least one package (target image) name is provided using property *detect.bitbake.package.names*.

If you are excluding build dependencies using the *detect.bitbake.dependency.types.excluded* property, the {builddir}/tmp directory must be left intact since a file (license.manifest)
that [detect_product_short] uses in that scenario resides in that tmp directory.

## Processing

The BitBake detector builds a dependency graph (codelocation) for each given package (target image). It sources your project's build environment setup
script (by default: oe-init-build-env), and executes BitBake commands to collect project and dependency details.

The BitBake detector generates one codelocation for each given package (target image) name by performing the following steps:
1. Determines the build directory path by sourcing the given build environment setup script and determining the resulting working directory.
1. Runs 'bitbake --environment' to determine the currently-configured target machine architecture and licenses directory path.
1. Runs 'bitbake-layers show-recipes' to derive the list of layers and collect recipe layer information.
1. For each given package (target image) name:
  * If the user requested that build dependencies be excluded, [detect_product_short] locates and reads the license.manifest file for the given package (target image) and the currently-configured target machine architecture. This provides a list of recipes that are included in the target image (the non-build dependencies).
  * Runs 'bitbake -g {package}' to generate task-depends.dot, and reads recipes and dependency relationships from it.
  * If the user requested that build dependencies be excluded: [detect_product_short] excludes recipes not declared in license.manifest, as well as native recipes. [detect_product_short] always excludes virtual recipes (recipes with names prefixed with "virtual/").
  * [detect_product_short] adds at the root level of the graph for the package (target image) each recipe found in task-depends.dot that is not excluded as described above.
  * Child (transitive) relationships are created from those root dependencies to their children (as specified in task-depends.dot).

Before running each BitBake command, [detect_product_short] sources the build environment init script,
passing any arguments the user has provided via the *detect.bitbake.source.arguments* property.

## Configuration

[detect_product_short] properties provide a number of different ways to customize the BitBake detector's behavior for your project. A few of the most important are:

1. You can configure the build environment setup script name using the *detect.bitbake.build.env.name property*.
1. You can add arguments (such as the path to your build directory) to the 'source {build env setup script}' command that [detect_product_short] executes using the *detect.bitbake.source.arguments* property.
1. You can exclude build dependencies from results using the *detect.bitbake.dependency.types.excluded* property.

See the BitBake properties page for a complete list of BitBake detector-related properties and details on how to use them.


## Troubleshooting Tips

### Missing components for projects using the Yocto Package Revision Service

Symptom: Components are missing from the [bd_product_short] BOM.

Problem: The Yocto Package Revision Service can increment a package revision to a value not present in the [bd_product_short] Knowledge Base, causing
a package to fail to match.
