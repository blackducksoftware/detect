# CPAN Support

## Related properties

[Detector properties](../properties/detectors/cpan.md)

## Overview

The CPAN detector will run if it finds a Makefile.PL file.

The detector requires the following executables:

* cpan - used to determine the list of direct dependencies required by the project.
* cpanm - used to assign versions to the dependencies found by cpan by determining the list of Perl modules installed on the system.

When executing the cpan command, [detect_product_short] will set the PERL_MM_USE_DEFAULT environment variable to true. This ensures that if cpan has not been configured on the system before, default configuration settings will be accepted.

The CPAN detector reports only direct dependencies and not transitive ones.
