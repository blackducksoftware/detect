FROM gradle:9.0.0-jdk17 

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src

ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"


# Install git
RUN apt-get update && apt-get install -y git

# Set up the test project
RUN mkdir -p ${SRC_DIR}

RUN cd ${SRC_DIR} && gradle init \
  --type java-application \
  --dsl kotlin \
  --test-framework junit-jupiter \
  --package my.project \
  --project-name my-project  \
  --no-split-project  \
  --java-version 11  \
  --no-incubating
