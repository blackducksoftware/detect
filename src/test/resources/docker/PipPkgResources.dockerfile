FROM python:3.7-slim

ARG ARTIFACTORY_URL
ARG PIP_VERSION="23.3.1"

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

# Set up test environment with Python 3.7 and Java
RUN apt-get update -y
RUN apt-get install -y git bash wget unzip default-jdk-headless build-essential libssl-dev libffi-dev
RUN pip install --upgrade "pip==${PIP_VERSION}"

# Set up test project
RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/pip-test-project-pkg.zip
RUN unzip pip-test-project-pkg.zip -d /opt/project/src
RUN mv pip-test-project-pkg/* .
RUN rm -r pip-test-project-pkg pip-test-project-pkg.zip
RUN pip install -r requirements.txt