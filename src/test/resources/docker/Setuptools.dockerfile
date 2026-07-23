FROM eclipse-temurin:11-jdk-jammy

ARG ARTIFACTORY_URL
ENV PIP_INDEX_URL="${ARTIFACTORY_URL}/artifactory/api/pypi/pypi-virtual/simple/"
ARG PIP_VERSION="24.2"
ARG SETUPTOOLS_VERSION="74.0.0"

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

# Set up test environment
RUN apt-get update -y
RUN apt-get install -y git bash wget unzip
RUN apt-get install -y python3 python3-pip
RUN ln -s /usr/bin/pip3 /usr/local/bin/pip
RUN pip install --upgrade "setuptools==${SETUPTOOLS_VERSION}"

# Set up test project
RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/setuptools-test-project.zip
RUN unzip setuptools-test-project.zip -d /opt/project/src
RUN mv setuptools-test-project/* .
RUN rm -r setuptools-test-project setuptools-test-project.zip
RUN /bin/sh -c "pip install ."
