# Detectors

By default, all detectors are eligible to run to find and extract dependencies from supported package managers.
The set of detectors that actually run depends on the files existing in your project directory, the properties you set, and whether the detector requirements are met.

To limit the eligible detectors to a given list of detector types, use:

````
--detect.included.detector.types={comma-separated list of detector type names}
````

To exclude entire detector types, use:

````
--detect.excluded.detector.types={comma-separated list of detector type names}
````

To exclude individual detectors instead of entire detector types, use:

````
--detect.excluded.detectors={comma-separated list of detector names}
````

<note type="note">Exclusions take precedence over inclusions.</note>

Refer to [Detectors](../../components/detectors.dita) for the list of detector names.

Refer to [Valid detect.excluded.detectors values](excluded-detectors-values.dita) for the accepted values, grouped by ecosystem.

Refer to [Properties](../../properties/all-properties.md) for details.
