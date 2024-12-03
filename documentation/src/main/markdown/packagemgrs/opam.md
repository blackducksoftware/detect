# Opam Support

## Related properties

[Detector properties](../properties/detectors/opam.md)

[detect_product_short] has two detectors for Opam:

* OPAM CLI Detector
* OPAM Lock Detector

Reference for [Opam](https://opam.ocaml.org/)

## OPAM CLI Detector

* This detector executes opam commands to discover dependencies of opam projects.

* This OPAM detector will be executed on your project if [detect_product_short] finds `<pkgname>.opam` file in your top level directory. It requires `opam`
exe to be present on your $PATH. You can also override the location for `opam` exe by the OPAM path property.

The OPAM Build Detector will work in the following way on your project:

1. [detect_product_short] OPAM Build Detector will run `opam --version` to get the version of opam on your machine.
2. If the version of opam is greater than or equal to 2.2.0, then [detect_product_short] would run `opam tree . --with-test --with-doc --with-dev --recursive`
to get the list of resolved packages installed in the current [switch](https://ocaml.org/docs/opam-switch-introduction#opam-switch-introduction) for the project.
<note type="note">You must have all prerequisites for the project set up on your machine (e.g., the opam switch where your packages for the project are installed), before running [detect_product_short].</note>
3. If the version constraint for 2.2.0 is not satisfied, or the tree commands fails for an unknown reason, [detect_product_short] will parse all dependencies found in the `<pkgname>.opam` files.
   For each of the parsed dependencies, [detect_product_short]  will run `opam show <pkgname>` recursively to find all transitive dependencies of the project.
   <note type="tip">Selecting the switch where all the packages are installed will help speed up the process.
   Run `opam install . --with-test --with-doc` commands to help store packages in opam cache.</note>

## OPAM Lock Detector

The OPAM Lock Detector does not find transitives for the project and is considered a LOW accuracy Detector. OPAM Lock Detector will run if HIGH accuracy Detectors cannot, and the project contains `<pkgname>.opam.locked` and `<pkgname>.opam` files in the top level directory.

OPAM Lock Detector will parse both `<pkgname>.opam.` and `<pkgname>.opam.locked` files to gather the list of direct project dependencies.