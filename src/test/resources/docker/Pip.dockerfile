FROM eclipse-temurin:8-jdk-jammy

ARG ARTIFACTORY_URL
ENV PIP_INDEX_URL="${ARTIFACTORY_URL}/artifactory/api/pypi/pypi-virtual/simple/"
ENV PIP_BREAK_SYSTEM_PACKAGES=1
ARG PIP_VERSION="25.2"

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

# Set up test environment
RUN apt-get update -y
RUN apt-get install -y git bash wget unzip build-essential python3-dev
RUN apt-get install -y python3 python3-pip
RUN pip install --ignore-installed "pip==${PIP_VERSION}"

# Set up test project
RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/pip-test-project.zip
RUN unzip pip-test-project.zip -d /opt/project/src
RUN mv pip-test-project/* .
RUN rm -r pip-test-project pip-test-project.zip
RUN pip install -r requirements.txt