FROM eclipse-temurin:8-jdk

ARG ARTIFACTORY_URL

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

# Set up test environment
RUN apt-get update -y
RUN apt-get install -y git bash wget unzip
RUN apt-get install -y python3 python3-pip
RUN pip3 install --break-system-packages --ignore-installed --upgrade "pip==25.2"
RUN pip3 install --break-system-packages --upgrade "uv==0.8.15"
RUN pip3 install --break-system-packages --upgrade "setuptools==80.9.0"

# Set up test project
RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/uv-getting-started.zip
RUN unzip uv-getting-started.zip -d /opt/project/src
RUN mv uv-getting-started/* .
RUN rm -r uv-getting-started uv-getting-started.zip
