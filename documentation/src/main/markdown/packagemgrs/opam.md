# Opam Support

## Related properties

[Detector properties](../properties/detectors/opam.md)

[detect_product_short] has two detectors for Opam:

* OPAM Build Detector
* OPAM Lockfile Detector

## OPAM Build Detector

* This detectable executes opam commands to discover dependencies of opam projects.

* The OPAM detector will be executed on your project if [detect_product_short] finds `.opam` file in your top level directory. It requires `opam`
exe to be present on your $PATH. You can also override the location for `opam` exe by the OPAM path property.

The OPAM Build Detector will work in the following way on your project:

1. [detect_product_short] OPAM Build Detector will run `opam --version` to get version of opam on your machine.
2. If the version of opam is greater than or equal to 2.2.0, then [detect_product_short] would run `opam tree . --with-test --with-doc --with-dev --recursive`
to get list of resolved packages installed in the current switch for the project.
<note type="note">User must have all the pre-requisites for the project already set up on your machine (e.g the opam switch where your packages for project are installed)
before running [detect_product_short] on your installed.</note>
3. If the version constraint for 2.2.0 is not satisfied or the tree commands fails for unknown reason, then [detect_product_short] will parse all the dependencies found in the `.opam` files. 
Then for each of the parsed dependencies, it will run `opam show <package-name>` recursively to find all the transitives dependencies of the project.
<note type="tip">Selecting the switch where all the packages are installed will help speed up the process. 
It would be helpful to run `opam install . --with-test --with-doc` commands to help store packages in opam cache.</note>

## OPAM Lockfile Detector

The OPAM Lockfile Detector will run if HIGH accurate Detectors above cannot be run, and project contains `.opam.locked` and `.opam` files in the top level directory.

OPAM Lockfile Detector will parse both `.opam.` and `.opam.locked` to gather list of direct dependencies of the project. 

This Detector will not be able to find the transitives for the project, and therefore it will be a LOW accuracy Detector. 