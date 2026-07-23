FROM eclipse-temurin:11-jdk-jammy

# Note: Intentionally using the argument name as PIPENV_VERSION_VAL instead of PIPENV_VERSION as the latter
# conflicts with the `click` Python package's options and causes `pipenv install` to result in an error.
ARG PIPENV_VERSION_VAL="2026.5.2"
ARG ARTIFACTORY_URL
ENV PIP_INDEX_URL="${ARTIFACTORY_URL}/artifactory/api/pypi/pypi-virtual/simple/"

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

# Set up test environment
RUN apt-get update -y
RUN apt-get install -y git bash wget unzip
RUN apt-get install -y python3 python3-pip
RUN pip install --upgrade "pipenv==${PIPENV_VERSION_VAL}"

# Set up test project
RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/pipenv-test-project.zip
RUN unzip pipenv-test-project.zip -d /opt/project/src && mv pipenv-test-project/* .
RUN rm -r pipenv-test-project pipenv-test-project.zip
RUN pipenv lock && pipenv install --dev