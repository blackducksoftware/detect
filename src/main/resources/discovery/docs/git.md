# Git project support

Unlike most detectors, the Git detectors do not discover dependencies;
they only discover project information.
Regardless of which Git detector runs, it
discovers as many of the following as it can find: project name, project version (branch), git repository URL, and commit hash.

Ideally a Git detector runs in combination with a package manager detector that discovers dependencies.
If the package manager detector is unable to derive project and project version names,
the Git detector may be able to provide them.

A Git detector will run if [detect_product_short] finds a .git subdirectory in your source directory.

If [detect_product_short] finds a git executable
(see the [detect git executable](../properties/configuration/paths.md#git-executable)
property)
the Git CLI detector will run git commands and derive project information from the output. 

Otherwise the Git Parse detector will attempt to parse (and derive project information from)
*config*, *HEAD*, and *ORIGIN_HEAD* files within the .git subdirectory.
The Git Parse detector will only be able to discover (and supply to [bd_product_short]) the
git commit hash if it finds an *ORIGIN_HEAD* file in the .git directory.
