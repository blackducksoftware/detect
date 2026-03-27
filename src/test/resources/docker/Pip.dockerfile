FROM eclipse-temurin:8-jdk

ARG ARTIFACTORY_URL
ARG PIP_VERSION

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src
ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

# Set up test environment
RUN apt-get update -y
RUN apt-get install -y git bash wget unzip curl software-properties-common
RUN add-apt-repository ppa:deadsnakes/ppa -y
RUN apt-get install -y python3.9 python3.9-distutils
RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.9 1

# Install pip at /usr/local/bin/pip
RUN curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
RUN python3.9 get-pip.py "pip==${PIP_VERSION}"
RUN rm get-pip.py

# Set up test project
RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/pip-test-project.zip
RUN unzip pip-test-project.zip -d /opt/project/src
RUN mv pip-test-project/* .
RUN rm -r pip-test-project pip-test-project.zip
RUN pip install -r requirements.txt
