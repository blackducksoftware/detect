# Small, self-contained fixture for AutonomousScanTests.autonomousScanModeOFFLINETest.
#
# The test only asserts that when autonomous scan mode is enabled under RAPID with
# offline mode, Detect produces:
#   - one BDIO file,
#   - a scan-settings JSON, and
#   - the scan-settings JSON contains a GRADLE detector entry that includes the
#     `detect.gradle.configuration.types.excluded` property.
#
# Any trivial Gradle project where Detect's Gradle detector fires satisfies all
# of the above. This dockerfile inlines a minimal single-module Gradle project
# so image build takes seconds, replacing the previous full clone + `gradlew build`
# of the Detect 9.8 repository which caused CI timeouts.
FROM gradle:8.10.2-jdk17

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src
ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

USER root

RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}

# Inline the fixture Gradle project.
RUN printf 'rootProject.name = "autonomous-scan-fixture"\n' > settings.gradle \
 && printf 'plugins { id "java" }\nrepositories { mavenCentral() }\ndependencies { implementation "org.apache.commons:commons-lang3:3.14.0" }\n' > build.gradle

# Warm the Gradle cache at image-build time so the scan itself does not pay
# the plugin/dependency download cost and is less network-dependent at runtime.
RUN gradle --no-daemon --quiet dependencies

