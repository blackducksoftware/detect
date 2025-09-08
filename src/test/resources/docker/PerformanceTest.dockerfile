FROM openjdk:8-jdk-slim

RUN apt-get update && apt-get -y install openjdk-17-jdk

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

# Set up the test project
RUN mkdir -p ${SRC_DIR}

# Create 12000 unique subdirectories with empty files
RUN cd ${SRC_DIR} && \
    for i in $(seq 1 12000); do \
        mkdir -p "subdir_${i}" && \
        touch "subdir_${i}/empty_file.txt"; \
    done

# Create one subdirectory with a simple requirements.txt file
RUN cd ${SRC_DIR} && \
    mkdir -p "subdir_with_requirements" && \
    echo "requests==2.28.1\nnumpy==1.24.3\npandas==1.5.3" > "subdir_with_requirements/requirements.txt"