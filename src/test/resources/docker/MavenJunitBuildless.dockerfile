FROM maven:3-eclipse-temurin-11

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Install git
RUN apt-get update
RUN apt-get install -y git

# Set up the test project
RUN mkdir -p ${SRC_DIR}

RUN git clone --depth 1 -b r4.13.2 https://github.com/junit-team/junit4.git ${SRC_DIR}

RUN cd ${SRC_DIR}