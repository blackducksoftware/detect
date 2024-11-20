FROM gradle:7.3.1-jdk11

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src
ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

USER root

# Install git
RUN apt-get update
RUN apt-get install -y git

# Set up the test project
RUN mkdir -p ${SRC_DIR}

RUN git clone --depth 1 -b 9.8.z https://github.com/blackducksoftware/detect ${SRC_DIR}

RUN cd ${SRC_DIR} \
   && ./gradlew build \