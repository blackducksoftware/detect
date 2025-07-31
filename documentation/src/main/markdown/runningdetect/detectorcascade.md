# Detector search and accuracy

## Detector search

Detector search is performed by the Detector tool on the source directory (`detect.source.path`).
If `detect.detector.search.depth` is greater than zero, detector search is expanded to include subdirectories
to the depth indicated (0 means top level only).

Detector search:

1. Finds project root directories, and
2. Determines which detector(s) should run on each project root directory that it found.

A project's root directory is the project's top-level directory viewed from the perspective of the project's package manager.
There may be more than one project in your source directory.

In contrast, graph building is performed by the individual detector(s) that run on a root project. This involves building a graph that
starts with the root project found by detector search. The graph includes subprojects, direct dependencies, and transitive dependencies.
The graph is eventually included in [detect_product_short]'s output: the BDIO.
Exclusion of subsets of the graph (subprojects, configurations, etc.) is optionally done as 
part of graph building (controlled by properties such as `detect.{pkgmgr}.excluded.*` and `detect.{pkgmgr}.included.*`).
It is important to recognize that detector search and graph building are completely separate processes
controlled by different properties.

For example, directory /src/myproject might be the root directory of a Gradle project. There might also be Gradle subprojects
in subdirectories underneath it (e.g. /src/myproject/subproject1), but subprojects will be discovered during graph building.
/src/myproject is the *only* directory that detector search must find.

Properties that affect detector search:

* detect.detector.search.depth
* detect.detector.search.continue

Detector search considers all the following when choosing which detector(s) to run:

1. Detector type filtering.
1. Yielding rules.
1. Nesting rules.
1. [detector_cascade].

## Detector type filtering

Detector type filtering honors your requests to exclude certain detector types (via properties `detect.excluded.detector.types` and `detect.included.detector.types`).

## Yielding rules

Yielding rules cause some detectors to have precedence over others for a given directory.    

Examples;    
* If both the YARN and NPM detector types apply to a directory, only the YARN detector will apply
because NPM yields to YARN.    
* If PMPN and NPM detectors apply, NPM will yield to PNPM detector.    

Yielding rules cannot be disabled.

## Nesting rules

While yielding rules consider which other detectors apply to the *current* directory,
nesting rules consider which other detectors applied to *ancestor* directories
(one or more levels up the directory path).

When detect.detector.search.depth is greater than 0,
nesting rules may prevent a detector from applying on a subdirectory of the source directory (say, src/a/b/c/d)
based on which detectors applied on any of its ancestor directories (src/a/b/c, src/a/b, src/a, or src).

Here are examples of nesting rules:
1. If any GRADLE detector applied on any ancestor directory, no GRADLE detector will apply on the current directory.
1. If any XCODE detector applied on any ancestor directory, neither SWIFT detector will apply on the current directory.
1. If an XCODE project has already processed, ignore XCODE project files that are nested inside.

Nesting rules can be disabled by setting property `detect.detector.search.continue` to true.

## [detector_cascade]

[detector_cascade] is a strategy designed to produce the most accurate results possible.
For a given project root directory, [detector_cascade] first tries the detector that would produce the most accurate results.
If the first detector is unable to run (if, for example, the package manager executable it needs is not on the PATH),
[detector_cascade] will try the next-best detector. This process continues until one of the applicable detector's extraction method succeeds
or [detect_product_short] runs out of detectors that apply.

[detect_product_short] will always try the more accurate detectors first, falling back to less accurate detectors only if the more accurate
detectors fail (or can't be run). 

Cascade sequences are not configurable.

[detector_cascade] in combination with detector accuracy (described below) replace the previous (pre-[detect_product_short] 8) distinction between "build mode" and "buildless mode",

## Entry points

When multiple detectors are defined for a Detector Type, we consider those to be detection entry points.
This allows for the definition of different nesting rules within the same Detector Type for different scenarios.

Detectors are attempted in the order listed until one applies and succeeds.
If none succeed, [detect_product_short] proceeds to the next (if there is one) for the Detector Type.

## Troubleshooting detector search

For more insight into the decisions [detect_product_short] made during detector search, generate
a diagnostic zip file (run with `-d`) and read the reports/search_detailed_report.txt file.

## Detector execution phases

A detector has three methods:

1. The applicable method determines whether the detector applies to the current directory, based on files that it finds in the directory. For example, if a Gradle detector would look for a build.gradle file.
1. The extractable method determines whether other prerequisites are met. For example, a detector that runs a package manager executable would check to see if that executable is available.
1. The extract method discovers dependencies and returns a graph. In a few cases extraction is performed with the help of a separate [detect_product_short] component called an inspector.

## Detector accuracy

Accuracy is an assessment of how complete and reliable a detector's results are. Each detector has one of two possible accuracy values: HIGH, or LOW.
A detector's accuracy value is not configurable.
You can find the accuracy for each detector in the [detector table](../components/detectors.dita).

Detectors that run the project's package manager and
discover dependencies from its output are generally assigned high accuracy because the package manager is typically a reliable source of truth
about dependencies.
Detectors that parse package manager-generated lockfiles also tend to be highly accurate.
Detectors that parse human-editable files are generally assigned low accuracy due to challenges and limitations that are inherent in that approach.

Consider, for example, a Gradle project.
[detect_product_short] could run the Gradle Native Inspector detector
(which discovers dependencies by running the Gradle CLI), or the
Gradle Project Inspector detector (which discovers dependencies by parsing Gradle files).
If the Gradle Native Inspector succeeds, it would produce higher accuracy results than the Gradle Project Inspector detector.
However, the Gradle Native Inspector may not succeed (since, for example, it must be able to find and execute a Gradle executable),
and (depending on the user's preference) low accuracy might be better than nothing.

## Specifying accuracy requirements

You choose the list of detector types from which you require the most accurate results using the `detect.accuracy.required` property.
This property accepts a list of detector types (MAVEN, GRADLE, ...).
This property defaults to ALL, which means you want [detect_product_short] to exit if any detector type applies, but
only low accuracy results could be generated.
When [detect_product_short] exits due to accuracy requirements not being met, it returns the FAILURE_ACCURACY_NOT_MET exit code.
This default produces behavior roughly similar to the default mode (detect.detector.buildless=false) prior to [detect_product_short] 8.

To get the best results available regardless of accuracy, set this property to NONE.
This value produces behavior roughly similar to buildless mode (detect.detector.buildless=true) prior to [detect_product_short] 8,
except that high accuracy results will be produced where possible (buildless mode used to prevent that).

To specify that you require accurate results from some (but not all) detector types, set property
`detect.accuracy.required` to the list of detector types from which you require the most accurate results.

## Evaluation of accuracy

After executing detectors, an actual result accuracy is known, at which point [detect_product_short] evaluates whether the detector results it was able to generate meet the user's accuracy requirements.
If the user's accuracy requirements were not met, [detect_product_short]
fails with the FAILURE_ACCURACY_NOT_MET exit code.
