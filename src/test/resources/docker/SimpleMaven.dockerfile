FROM maven:3-eclipse-temurin-11

# Install git
RUN apt-get update
RUN apt-get install -y git

# Set up the test project
RUN mkdir -p /opt/project

RUN cd /opt/project \
   && git clone --depth 1 https://github.com/pdurbin/maven-hello-world

RUN mv /opt/project/maven-hello-world/my-app /opt/project/src

RUN cd /opt/project/src \
   && mvn dependency:resolve