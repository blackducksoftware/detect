FROM openjdk:8-jdk

ARG ARTIFACTORY_URL

RUN apt update \
   && apt install -y vim

RUN apt-get install -y patch bubblewrap gcc make
RUN apt-get install -y git bash wget unzip

ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

RUN mkdir -p ${SRC_DIR}

RUN curl -s -L -O https://github.com/ocaml/opam/releases/download/2.3.0/opam-2.3.0-x86_64-linux \
    && install opam-2.3.0-x86_64-linux /usr/local/bin/opam \
    && rm opam-2.3.0-x86_64-linux

RUN opam init -y --disable-sandboxing --shell-setup

RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/dream.zip
RUN unzip dream.zip -d /opt/project/src
RUN rm dream.zip

RUN opam install . -y --with-test --with-doc

RUN cd ${SRC_DIR}