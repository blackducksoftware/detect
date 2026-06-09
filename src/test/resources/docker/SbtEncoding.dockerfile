#This tests a customer issue from IDETECT-2714
FROM maven:3-eclipse-temurin-11-alpine

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Install git: https://github.com/nodejs/docker-node/issues/586
RUN apk update && apk upgrade && \
    apk add --no-cache bash git openssh

#Install SBT
WORKDIR /home/app
RUN wget -q "https://github.com/sbt/sbt/releases/download/v1.3.13/sbt-1.3.13.tgz"
RUN tar -xzf sbt-1.3.13.tgz
RUN rm sbt-1.3.13.tgz
ENV PATH="/home/app/sbt/bin:${PATH}"

# Set up the test project
RUN mkdir -p ${SRC_DIR}

RUN git clone --depth 1 https://github.com/aiyanbo/sbt-simple-project.git ${SRC_DIR} \
    && sed -i 's/2\.12\.7/2.12.19/' ${SRC_DIR}/project/Dependencies.scala \
    && sed -i 's/sbt\.version=.*/sbt.version=1.3.13/' ${SRC_DIR}/project/build.properties

RUN cd ${SRC_DIR} \
   && sbt compile