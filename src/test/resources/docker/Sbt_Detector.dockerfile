FROM maven:3-jdk-8-alpine

ENV SRC_DIR=/opt/project/src

ARG ARTIFACTORY_URL

# Install git: https://github.com/nodejs/docker-node/issues/586
RUN apk update && apk upgrade && \
    apk add --no-cache bash git openssh

#Install SBT
WORKDIR /home/app
RUN wget -q "https://github.com/sbt/sbt/releases/download/v1.10.11/sbt-1.10.11.tgz"
RUN tar -xzf sbt-1.10.11.tgz
RUN rm sbt-1.10.11.tgz
ENV PATH="/home/app/sbt/bin:${PATH}"

# Set up the test project
RUN mkdir -p ${SRC_DIR}

RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/new_sbt_graphviz_java.zip
RUN unzip new_sbt_graphviz_java.zip -d /opt/project/src/
RUN rm new_sbt_graphviz_java.zip

RUN cd ${SRC_DIR}