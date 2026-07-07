FROM eclipse-temurin:11-jdk-jammy

# Default value of "goVersion" below is overridden by value passed as argument when building the image
ARG goVersion=1.16.6

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Install git
RUN apt-get update -y
RUN apt-get install -y git bash wget

## Install Go
WORKDIR /usr/local
RUN wget -q "https://golang.org/dl/go${goVersion}.linux-amd64.tar.gz"
RUN mkdir "go${goVersion}"
RUN tar -C "go${goVersion}" -xzf "go${goVersion}.linux-amd64.tar.gz"
RUN rm "go${goVersion}.linux-amd64.tar.gz"

# Set up the test project
RUN mkdir -p ${SRC_DIR}
WORKDIR ${SRC_DIR}
RUN git clone -b v1.5.4 --depth 1 https://github.com/Masterminds/squirrel.git ${SRC_DIR}