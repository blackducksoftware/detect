# Dart Support

## Related properties

[Detector properties](../properties/detectors/dart.md)

## Overview

[detect_product_short] has two detectors for Dart:

* Dart CLI detector
* Dart PubSpec Lock detector

Both detectors will run if they find the following files:

* pubspec.yaml
* pubspeck.lock

If [detect_product_short] cannot find a pubspec.lock file, but it finds a pubspec.yaml file, it will prompt the user to run the 'pub get' command to generate the pubspec.lock file, and then run [detect_product_short] again.

Both detectors parse the pubspec.yaml file to determine project name and version information.

The Dart PubSpec Lock detector parses the pubspec.lock file for dependency information. Since the file does not indicate relationships between components, results from this detector will be less accurate than those from the Dart CLI detector.

The Dart CLI detector runs the command 'pub deps' (which requires a pubspec.lock file to be present), and then parses the command's output for dependency information.  The detector will first try to run the command using a dart executable (if found), but if it is unsuccessful because the target project requires the Flutter SDK then it will try using a flutter executable (if found).

You may specify the location of dart and flutter executables using the [detect.dart.path](../properties/detectors/dart.md#dart-executable) and [detect.flutter.path](../properties/detectors/dart.md#flutter-executable) properties, respectively.

If you wish to exclude dev dependencies, you may do so using the [detect.pub.dependency.types.excluded](../properties/detectors/dart.md#dart-pub-dependency-types-excluded) property, which will cause the detector to pass the --no-dev option when running 'pub deps'.
