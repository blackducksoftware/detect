FROM mcr.microsoft.com/dotnet/sdk:7.0

ENV SRC_DIR=/opt/project/src

RUN apt-get update

# Install java
RUN mkdir -p /usr/share/man/man1/
# Above due to: https://github.com/geerlingguy/ansible-role-java/issues/64
RUN apt-get install -y openjdk-11-jre

# Install git
RUN apt-get install -y git

RUN mkdir -p air-gap-docker

# Set up the test project
RUN wget ${ARTIFACTORY_URL}/artifactory/detect-generic-qa-local/nuget-artifacts-path.zip
RUN unzip nuget-artifacts-path.zip -d /opt/project/src
RUN rm nuget-artifacts-path.zip

RUN cd ${SRC_DIR}