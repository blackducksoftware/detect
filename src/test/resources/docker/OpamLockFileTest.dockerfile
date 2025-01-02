FROM openjdk:8-jdk

ARG ARTIFACTORY_URL

RUN apt update \
   && apt install -y vim

RUN apt-get install -y git bash wget unzip

ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

RUN mkdir -p ${SRC_DIR}

RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/opam-monorepo.zip
RUN unzip opam-monorepo.zip -d /opt/project/src
RUN rm opam-monorepo.zip

RUN cd ${SRC_DIR}