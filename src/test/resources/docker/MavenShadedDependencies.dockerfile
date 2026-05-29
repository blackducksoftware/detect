FROM maven:3.9.9-eclipse-temurin-17

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Install git
RUN apt-get update
RUN apt-get install -y git wget unzip

# Set up the test project
RUN mkdir -p ${SRC_DIR}

# RUN git clone --depth 1 https://github.com/crate/crate.git ${SRC_DIR}

RUN wget https://artifactory.tools.duckutil.net/artifactory/detect-generic-qa-local/shaded-test-10deps.zip \
&& unzip shaded-test-10deps.zip -d /tmp/shaded \
&& rm shaded-test-10deps.zip \
&& cp -a /tmp/shaded/shaded-test-10deps/. ${SRC_DIR}/ \
&& rm -rf /tmp/shaded

RUN cd ${SRC_DIR}