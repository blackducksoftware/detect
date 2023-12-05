# Current Release notes

## Version 9.2.0

### New features

* Support for pnpm is now extended to 8.9.2.
* Nuget support extended to version 6.2 with Central Package Management now supported for projects and solutions.
* Support for Conan is now extended to 2.0.14.
* Support for Go and Python added to Component Location Analysis.

### Changed features

* pnpm 6, and pnpm 7 using the default v5 pnpm-lock.yaml file, are being deprecated. Support will be removed in [solution_name] 10.

### Resolved Issues

* (IDETECT-3515) Resolved an issue where the Nuget Inspector was not supporting "\<Version\>" tags for "\<PackageReference\>" on the second line and was not cascading to Project Inspector in case of failure.

### Dependency updates

* Released and Upgraded Nuget Inspector to version 1.2.0.