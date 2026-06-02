FROM maven:3.9.9-eclipse-temurin-17

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Install git wget and unzip
RUN apt-get update
RUN apt-get install -y git wget unzip

# Set up the test project
RUN mkdir -p ${SRC_DIR}

ARG ARTIFACTORY_URL
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/shaded-test-10deps.zip \
&& unzip shaded-test-10deps.zip -d /tmp/shaded \
&& rm shaded-test-10deps.zip \
&& cp -a /tmp/shaded/shaded-test-10deps/. ${SRC_DIR}/ \
&& rm -rf /tmp/shaded

RUN cd ${SRC_DIR}