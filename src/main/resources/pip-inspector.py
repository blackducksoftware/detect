# pylint: disable=fixme, line-too-long, import-error, no-name-in-module
#
# Copyright (c) 2025 Black Duck Software Inc.
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#

# Uncomment for debugging. Can't use localhost.
# See: https://www.jetbrains.com/help/pycharm/remote-debugging-with-product.html#remote-debug-config
# import pydevd_pycharm
# pydevd_pycharm.settrace('<Host IP Address>', port=5002, stdoutToServer=True, stderrToServer=True)

"""
A script that inspects the pip cache to determine the hierarchy of dependencies.

This script is invoked by Black Duck Detect as a subprocess inside the target Python
environment. Installed dependencies are read and a dependency tree is printed to stdout
which Detect parses for Black Duck SCA analysis.

IMPORTANT: This script does NOT install packages. It only reads what is already
installed. Run 'pip install' before invoking this script.

Usage: pip-inspector.py --projectname=<project_name> --requirements=<requirements_path>

Output format:
  - Dependency tree: indented plain text, 4 spaces per level
  - Sentinel prefixes parsed by Detect's Java side:
      n?==v?       root project package not found in environment
      --<name>     package not found in environment
      r?<path>     requirements file does not exist
      p?<path>     requirements file could not be parsed
      [PIP_INSPECTOR] ...  informational log, ignored by Detect
"""

from getopt import getopt, GetoptError
from os import path
import sys
from re import split, match

# ---------------------------------------------------------------------------
# pip version-aware imports
#
# pip reorganized its internal package structure across major versions.
# We import parse_requirements and PipSession from the correct location
# depending on which pip version is installed.
#
#   pip >= 20  ->  network.session replaced download
#   pip 10-19  ->  pip._internal restructure, download still present
#   pip < 10   ->  flat structure under pip (not pip._internal)
# ---------------------------------------------------------------------------
import pip
pip_major_version = int(pip.__version__.split(".")[0])
if pip_major_version >= 20:
    from pip._internal.req import parse_requirements
    from pip._internal.network.session import PipSession
elif pip_major_version >= 10:
    from pip._internal.req import parse_requirements
    from pip._internal.download import PipSession
else:
    from pip.req import parse_requirements
    from pip.download import PipSession


def main():
    """Handles commandline args, executes the inspector, and prints the resulting dependency tree"""
    try:
        opts, __ = getopt(sys.argv[1:], 'p:r:', ['projectname=', 'requirements='])
    except GetoptError as error:
        print(str(error))
        print('integration-pip-inspector.py --projectname=<project_name> --requirements=<requirements_path>')
        sys.exit(2)

    project_name = None
    requirements_path = None

    for opt, arg in opts:
        if opt in ('-p', '--projectname'):
            project_name = arg
        elif opt in ('-r', '--requirements'):
            requirements_path = arg

    project_dependency_node = resolve_project_node(project_name)

    if requirements_path is not None:
        try:
            assert path.exists(requirements_path), ("The requirements file %s does not exist." % requirements_path)
            populate_dependency_tree(project_dependency_node, requirements_path)
        except AssertionError:
            print('r?' + requirements_path)

    print(project_dependency_node.render())


def resolve_project_node(project_name):
    """Attempts to resolve the root DependencyNode from the user provided --projectname argument.
    If it can't, produces a DependencyNode with name 'n?' and version 'v?'"""
    project_dependency_node = None

    if project_name is not None:
        project_dependency_node = recursively_resolve_dependencies(project_name, [])

    if project_dependency_node is None:
        project_dependency_node = DependencyNode('n?', 'v?')

    return project_dependency_node


def normalize_package_name(package_name):
    """Extract just the package name from a raw requirement string.

    A raw requirement string from pip can look like any of these:
      'requests>=2.0'                         version specifier
      'kopf[dev]>=1.37.3'                     extras with version
      'requests @ git+https://...'            PEP 508 direct URL reference
      'boto3 ; extra == "aws"'                conditional extras marker
      'colorama ; platform_system == "Windows"'  environment marker

    In all cases we only want the package name at the start. Valid package name
    characters are letters, digits, dots, hyphens, underscores (PEP 508).
    The regex stops at the first character outside that set (space, [, @, ;, >, < etc.)
    leaving us with just the clean name.

    Examples:
      'requests>=2.0'                      -> 'requests'
      'kopf[dev]>=1.37.3'                  -> 'kopf'
      'requests @ git+https://...'         -> 'requests'
      'boto3 ; extra == "aws"'             -> 'boto3'
    """
    if package_name is None:
        return None
    name_match = match(r'^([a-zA-Z0-9._-]+)', package_name.strip())
    if name_match:
        return name_match.group(1)
    return None


def populate_dependency_tree(project_root_node, requirements_path):
    """Reads requirements.txt and adds each package as a child of the root node.

    Extracting the package name from a requirements.txt entry:

    Modern pip (>= 20.1) returns a ParsedRequirement object from parse_requirements().
    This object has a .requirement attribute (the raw string from the file) but does
    NOT have a .req attribute. So hasattr(..., 'req') is always False on modern pip
    and we always fall through to the split fallback.

    Older pip (< 20.1) returned an InstallRequirement which had a .req attribute
    with a .name property. That path is kept for backwards compatibility.

    The split fallback splits the raw string on version comparators to isolate the
    package name, then normalize_package_name() cleans it further. This handles:
      - Standard pins:        'flask==3.0.0'           -> split on == -> 'flask'
      - Version ranges:       'requests>=2.0'          -> split on >= -> 'requests'
      - Extras with version:  'kopf[dev]>=1.37.3'      -> split on >= -> 'kopf[dev]'
                                                           normalize  -> 'kopf'
      - PEP 508 URL refs:     'requests @ git+https://'-> no comparator match
                                                           normalize  -> 'requests'
    """
    try:
        parsed_requirements = parse_requirements(requirements_path, session=PipSession())
        for parsed_requirement in parsed_requirements:
            package_name = None

            # Path 1: old pip (< 20.1) — InstallRequirement has .req.name directly
            if hasattr(parsed_requirement, 'req'):
                package_name = parsed_requirement.req.name

            # Path 2: modern pip fallback — split on version comparators then normalize.
            # Comparators ordered longest-first to avoid partial matches (e.g. === before ==).
            # See: https://www.python.org/dev/peps/pep-0508/#grammar
            if package_name is None:
                package_name = normalize_package_name(
                    split('===|<=|!=|==|>=|~=|<|>', parsed_requirement.requirement)[0]
                )

            # normalize_package_name() returns None for non-package entries such as
            # include directives (-r other.txt) or index options (-i https://...).
            # parse_requirements() skips blank lines and comments but passes these through.
            if package_name is None:
                continue

            dependency_node = recursively_resolve_dependencies(package_name, [])

            if dependency_node is not None:
                project_root_node.children = project_root_node.children + [dependency_node]
            else:
                print('--' + package_name)
    except Exception as e:
        print(e)
        print('p?' + requirements_path)


def recursively_resolve_dependencies(package_name, history):
    """Builds a DependencyNode tree by recursively resolving transitive dependencies.

    For each package:
      1. Look it up in the installed environment via get_package_by_name()
      2. get_package_by_name() returns the node (name + version) and a list of
         its direct dependency names (already normalized, ready for lookup)
      3. Recurse into each dependency to build their subtrees

    The history list tracks packages already visited in the current traversal.
    If a package appears again (cycle or diamond dependency), it is added as a
    leaf node without re-expanding its children. This prevents infinite loops.
    """
    dependency_node, child_names = get_package_by_name(package_name)

    if dependency_node is None:
        return None

    if dependency_node.name not in history:
        history.append(dependency_node.name)
        for child_name in child_names:
            child_node = recursively_resolve_dependencies(child_name, history)
            if child_node is not None:
                dependency_node.children = dependency_node.children + [child_node]

    return dependency_node


# ---------------------------------------------------------------------------
# get_package_by_name — two implementations, one chosen at startup
#
# The right implementation is selected once at module load time based on
# which libraries are available. Only one definition ends up active.
#
# WHY TWO IMPLEMENTATIONS:
#
#   Python 3.8+  ->  Flow 1: importlib.metadata (standard library)
#                    Available since Python 3.8, present on all modern Python
#                    versions including 3.12+. Reads package metadata directly
#                    from .dist-info directories on disk.
#                    dist.requires returns ALL Requires-Dist entries including
#                    extras markers ('; extra == "dev"'), giving us the full
#                    transitive dependency picture.
#
#   Python < 3.8  ->  Flow 2: pkg_resources (legacy fallback)
#                    importlib.metadata not available. Uses setuptools'
#                    pkg_resources.working_set. Only returns unconditional
#                    dependencies (environment markers evaluated at call time,
#                    extras-only dependencies not included unless activated).
#
# BOTH FLOWS follow the same contract:
#   Input:  package_name (str) — may be a raw string with version/extras/markers,
#           normalize_package_name() is applied internally before any lookup.
#   Output: (DependencyNode, list_of_child_names)
#           child names are already normalized clean package names, ready for
#           the next recursion. None is returned if the package is not found.
# ---------------------------------------------------------------------------

use_importlib_metadata = False
use_pkg_resources = False

# Priority 1: importlib.metadata — standard library since Python 3.8, covers all modern versions
try:
    import importlib.metadata as metadata
    use_importlib_metadata = True
    print("[PIP_INSPECTOR] Using importlib.metadata route")
except ImportError:
    # Priority 2: pkg_resources — legacy fallback for Python < 3.8
    try:
        from pkg_resources import working_set, Requirement
        use_pkg_resources = True
        print("[PIP_INSPECTOR] Using pkg_resources route")
    except ImportError:
        print("[PIP_INSPECTOR] WARNING: Neither importlib.metadata nor pkg_resources is available. Dependencies cannot be resolved and will be missing from the output.")

if not use_importlib_metadata and not use_pkg_resources:
    # Neither library is available — define a no-op fallback so the script
    # degrades gracefully instead of raising NameError on the first lookup.
    def get_package_by_name(package_name):
        return None, None

if use_importlib_metadata:
    # ---------------------------------------------------------------------------
    # Flow 1: importlib.metadata (Python 3.8+, including 3.12+)
    #
    # Reads installed package metadata directly from .dist-info directories.
    # dist.requires returns ALL Requires-Dist entries including extras markers
    # such as '; extra == "dev"'. normalize_package_name() strips the markers
    # so only the package name is passed to the next lookup.
    #
    # Name variants are tried (lowercase, hyphens vs underscores) because
    # package names are stored inconsistently across environments.
    # ---------------------------------------------------------------------------
    def get_package_by_name(package_name):
        if package_name is None:
            return None, None

        # Normalize incoming name — may contain version specifiers, extras, or
        # URL syntax (e.g. 'requests @ git+https://...') from the requirements
        # file or from a parent package's Requires-Dist list.
        normalized_name = normalize_package_name(package_name)
        if normalized_name is None:
            return None, None

        package_info = None
        try:
            package_info = metadata.distribution(normalized_name)
        except metadata.PackageNotFoundError:
            # Try common name variants — pip normalizes hyphens/underscores/dots
            # inconsistently, so the stored name may differ from what was requested
            name_variants = (normalized_name, normalized_name.lower(), normalized_name.replace('-', '_'), normalized_name.replace('_', '-'), normalized_name.replace('.', '-'))
            for name_variant in name_variants:
                try:
                    package_info = metadata.distribution(name_variant)
                    break
                except metadata.PackageNotFoundError:
                    continue

        if package_info is None:
            return None, None

        # Normalize each child requirement string to a plain package name.
        # Entries that are not installed will return None from the next lookup
        # and are silently skipped by recursively_resolve_dependencies.
        requires = []
        if package_info.requires:
            for requirement in package_info.requires:
                req_name = normalize_package_name(requirement)
                requires.append(req_name)

        # Remove duplicates while preserving order
        requires = list(dict.fromkeys(requires))

        return DependencyNode(package_info.metadata['Name'], package_info.metadata['Version']), requires

elif use_pkg_resources:
    # ---------------------------------------------------------------------------
    # Flow 2: pkg_resources (Python < 3.8, legacy fallback)
    #
    # Uses setuptools' WorkingSet which is built at import time by scanning
    # all installed .dist-info and .egg-info directories.
    #
    # NOTE: package.requires() evaluates environment markers at call time,
    # meaning only dependencies applicable to the current platform/Python
    # version are returned. Extras-only dependencies are NOT included unless
    # the extras were explicitly activated. This differs from Flow 1 which
    # returns all Requires-Dist entries unconditionally.
    # ---------------------------------------------------------------------------
    def get_package_by_name(package_name):
        if package_name is None:
            return None, None

        package = None

        package_dict = working_set.by_key
        try:
            # Requirement.parse normalizes the name to a lookup key
            package = package_dict[Requirement.parse(package_name).key]
        except:
            # Try common name variants if the normalized key lookup fails
            name_variants = (package_name, package_name.lower(), package_name.replace('-', '_'), package_name.replace('_', '-'), package_name.replace('.', '-'))
            for name_variant in name_variants:
                if name_variant in package_dict:
                    package = package_dict[name_variant]
                    break

        if package is None:
            return None, None

        # package.requires() returns Requirement objects with environment
        # markers already evaluated — .key gives the normalized package name
        return DependencyNode(package.project_name, package.version), [requirement.key for requirement in package.requires()]


class DependencyNode(object):
    """A node in the dependency tree, holding a package name, version, and its children."""
    def __init__(self, name, version):
        self.name = name
        self.version = version
        self.children = []

    def render(self, layer=1):
        """Recursively builds the indented dependency tree string printed to stdout.
        Each child level is indented by 4 spaces. Detect parses this output."""
        result = self.name + "==" + self.version
        for child in self.children:
            result += "\n" + (" " * 4 * layer)
            result += child.render(layer + 1)
        return result


if __name__ == '__main__':
    main()
