#This tests a customer issue from IDETECT-2714
FROM maven:3-eclipse-temurin-11-alpine

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Install git: https://github.com/nodejs/docker-node/issues/586
RUN apk update && apk upgrade && \
    apk add --no-cache bash git openssh

#Install SBT
WORKDIR /home/app
RUN wget -q "https://github.com/sbt/sbt/releases/download/v1.5.2/sbt-1.5.2.tgz"
RUN tar -xzf sbt-1.5.2.tgz
RUN rm sbt-1.5.2.tgz
ENV PATH="/home/app/sbt/bin:${PATH}"

# Set up the test project
RUN mkdir -p ${SRC_DIR}

RUN git clone --depth 1 https://github.com/aiyanbo/sbt-simple-project.git ${SRC_DIR} \
    && sed -i 's/2\.12\.7/2.12.19/' ${SRC_DIR}/project/Dependencies.scala \
    && sed -i 's/sbt\.version=.*/sbt.version=1.5.2/' ${SRC_DIR}/project/build.properties \
    && echo 'addDependencyTreePlugin' > ${SRC_DIR}/project/plugins.sbt \
    && sed -i '/enablePlugins(PackPlugin)/d' ${SRC_DIR}/build.sbt

RUN cd ${SRC_DIR} \
   && sbt compile