FROM maven:3.9.11-eclipse-temurin-17

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Install git
RUN apt-get update
RUN apt-get install -y git

# Set up the test project
RUN mkdir -p ${SRC_DIR}

RUN git clone --depth 1 https://github.com/crate/crate.git ${SRC_DIR}

RUN cd ${SRC_DIR}