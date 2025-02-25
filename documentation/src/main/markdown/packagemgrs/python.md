# Python support

## Related properties

[Detector properties](../properties/detectors/python.md)

## Overview

[detect_product_short] detectors for discovery of dependencies in Python:

* Setuptools detector
* PIPENV detectors
	* Pipenv lock detector
	* Pipfile lock detector
* PIP detectors
	* Pip Native Inspector
	* Pip Requirements File Parse
* Poetry detector

## Setuptools detector

Setuptools detector attempts to run on your project if a pyproject.toml file containing a build section with `requires = ["setuptools"]` or equivalent line is located, and a pip installation is found. (Setuptools scans can be run in both build, if a pip installation is available, and buildless mode, if not.)

<note type="note">Setuptools build detector should be run in a virtual environment, or environment with a clean global pip cache, where a pip install has only been performed for the project being scanned.</note>

[detect_product_short] parses the pyproject.toml file determining if the `[build-system]` section has been configured for Setuptools Pip via the `requries= ["setuptools"]` setting. If the setting is located and pip is installed in the environment, either in the default location or specified via the `--detect.pip.path` property, the detector will execute in a virtual environment, if configured as suggested, and analyze the pyproject.toml, setup.cfg, or setup.py files for dependencies. If the detector discovers a configured pyproject.toml file but not a pip executible, it will execute in buildless mode where it will parse dependencies from the pyproject.toml, setup.cfg, or setup.py files but may not be able to specify exact package versions. If no dependencies are located in the pyproject.toml, setup.cfg, or setup.py files, or if the detector fails, the BDIO file output will not be generated in build or buildless mode. [detect_product_short] will also attempt to run additional detectors if their execution requirements are met.

For setup.cfg and setup.py file parsing, the Setuptools detector supports direct mentioning of dependency files. For reference, see 
[Dependency Management in Setuptools](https://setuptools.pypa.io/en/latest/userguide/dependency_management.html).

<note type="tip">URL references, optional dependencies and `file: \<path to file\>` parameters found in setup.cfg are not supported. For setup.py files, programmatic population of the `install_requires` parameter is not supported.</note>

<note type="note">The `--detect.pip.only.project.tree`, `--detect.pip.project.name`, and `--detect.pip.project.version.name` properties do not apply to the Setuptools detector.</note>

## PIPENV Detectors

## Pipenv lock detector

The Pipenv lock detector attempts to run on your project if either a Pipfile or Pipfile.lock file is found.

Pipenv detector requires Python and Pipenv executables.

* [detect_product_short] looks for python on $PATH. You can override this by setting the python path property.
* [detect_product_short] looks for pipenv on $PATH.

The Pipenv detector runs `pipenv run pip freeze` and `pipenv graph --bare --json-tree` and derives dependency information from the output. The dependency hierarchy is derived from the output of `pipenv graph --bare --json-tree`. The output of `pipenv run pip freeze` is used to improve the accuracy of dependency versions.

To troubleshoot of the Pipenv detector, start by running `pipenv graph --bare --json-tree`, and making sure that the output looks correct since this is the basis from which [detect_product_short] constructs the BDIO. If the output of `pipenv graph --bare --json-tree` does not look correct, make sure the packages (dependencies) are installed into the Pipenv virtual environment (`pipenv install`).

<note type="note">The [detect.pipfile.dependency.types.excluded](../properties/detectors/pip.md#pipfile-dependency-types-excluded) property does not apply to the Pipenv detector.</note>

## Pipfile lock detector

The Pipfile lock detector attempts to run on your project if either a Pipfile.lock or Pipfile file is found AND neither of the Pip or Pipenv detectors applied.

Pipfile lock detector parses the Pipfile.lock file for dependency information. If the detector discovers a Pipfile file but not a Pipfile.lock file, it will prompt the user to generate a Pipfile.lock file by running `pipenv lock` and then run [detect_product_short] again.
Pipfile.lock dependencies can be filtered using the [detect.pipfile.dependency.types.excluded](../properties/detectors/pip.md#pipfile-dependency-types-excluded) property.

## PIP Detectors

## Pip Native Inspector

Pip Native Inspector attempts to run on your project if any of the following are true: a setup.py file is found, a requirements.txt is found, or a requirements file is provided using the [--detect.pip.requirements.path](../properties/detectors/pip.md#pip-requirements-path) property.

Pip Native Inspector requires Python and pip executables.

* [detect_product_short] looks for python on $PATH. You can override this by setting [--detect.python.path](../properties/detectors/python.md#python-executable)
* [detect_product_short] looks for pip on $PATH. You can override this by setting [--detect.pip.path](../properties/detectors/pip.md#pip-executable)

Pip Native Inspector runs the [pip-inspector.py script](https://github.com/blackducksoftware/detect/blob/master/src/main/resources/pip-inspector.py), which uses Python/pip libraries to query the pip cache for the project, which may or may not be a virtual environment, for dependency information:

1. pip-inspector.py queries for the project dependencies by project name which can be discovered using setup.py, or provided using the detect.pip.project.name property. If your project is installed into the pip cache, this discovers dependencies specified in setup.py.
1. If one or more requirements files are found or provided, pip-inspector.py queries each requirements file for possible additional dependencies and details of each.

<note type="tip">Only those packages which have been installed; using, for example, `pip install`, into the pip cache and appearing in the output of `pip list`, are included in the output of pip-inspector.py. There must be a match between the package version on which your project depends and the package version installed in the pip cache.</note>
<note type="note">If the packages are installed into a virtual environment for your project, you must run [detect_product_short] from within that virtual environment.</note>

### Recommendations for Pip Detector

* Be sure that [detect_product_short] is locating the correct verion of the Python executable; this can be done by running the logging level at DEBUG and then reading the log. This is a particular concern if your system has multiple versions of Python installed.
* Create a setup.py file for your project.
* Install your project and dependencies into the pip cache:
````
python setup.py install
pip install -r requirements.txt
````
* Pip detector derives your project name using your setup.py file if you have one. If you do not have a setup.py file, you must provide the correct project name using the propety --detect.pip.project.name.
* If there are any dependencies specified in requirements.txt that are not specified in setup.py, then provide the requirements.txt file using the [detect_product_short] property.   
<note type="tip">If you are using a virtual environment, be sure to switch to that virtual environment when you run [detect_product_short]. This also applies when you are using a tool such as Poetry that sets up a Python virtual environment.</note>

## PIP Requirements File Parse

Pip Requirements File Parse is a buildless detector that acts as a LOW accuracy fallback for the Pip Native Inspector. This detector gets triggered for Pip projects that contain one or more requirements.txt files but [detect_product_short] doesn't have access to a Pip executable in the environment where the scan is executed.
 
### Requirements file selection (Default)

In the default case, the detector will search for a file named `requirements.txt` for parsing. If the file has references to other `requirements.txt` files in the project provided via the `-r` or `--requirement` option, the detector will try to resolve these file references and also include them for parsing. 
<note type="note">The options `-r` or `--requirement` expect a UNIX path (relative or absolute) to the other requirements files. If an invalid path is provided via these options, [detect_product_short] will display a warning and continue parsing the initial requirements file.</note>

### Requirements file selection (Override)

The `--detect.pip.requirements.path` property can be used to provide an explicit comma-separated list of paths to requirements files that the parser should include. These files may be present in directories other than the source directory and may have file names other than the default file name "requirements.txt". Since this property is an override, only the files referenced via this property are considered for parsing, and the detector will not include any other file references in the given files content via the `-r` or `--requirement` option.

### Parsing

This parser is a LOW accuracy, best-effort detector. In most cases, it can extract dependency information from the file when entries are in the format `<dependency_name> == <version_name>`. This is the typical format in requirements files generated with the Pip CLI using the `pip freeze > requirements.txt` command. The parser does not resolve any special wildcard characters in the version string, for example a version string present as `1.2.*` is extracted exactly as it is.
In cases where a range of versions is provided for a dependency, for example `>= 1.2.3, <1.3`, the parser will extract the lowest version in the range i.e. `1.2.3`.
Any extras added after the package name are not resolved by the parser. For example, for a package name declared as `requests[security]`, only the requests package is extracted and not the extra option specified as `[security]`.
<note type="note">If any URL to a component is present in the requirements file being parsed, the parser will extract the entire URL as a component without a version.</note>

## Poetry detector

Poetry detector attempts to run on your project if either a poetry.lock or pyproject.toml file containing a tool.poetry section is found.

The Poetry detector parses poetry.lock for dependency information. If the detector discovers a pyproject.toml file but not a poetry.lock file, it will prompt the user to generate a poetry.lock by running `poetry install` and then run [detect_product_short] again.
The Poetry detector extracts the project's name and version from the pyproject.toml file.  If it does not find a pyproject.toml file, it will defer to values derived by git, from the project's directory, or defaults.

When the `--detect.poetry.dependency.groups.excluded` property is specified, presence of both poetry.lock and pyproject.toml files is required for this detector to run successfully.
