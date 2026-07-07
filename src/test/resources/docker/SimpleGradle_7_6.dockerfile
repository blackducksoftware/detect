FROM gradle:7.6-jdk11

# Do not change SRC_DIR, value is expected by tests
ENV SRC_DIR=/opt/project/src
ENV JAVA_TOOL_OPTIONS="-Dhttps.protocols=TLSv1.2"

RUN mkdir -p ${SRC_DIR}

RUN cd ${SRC_DIR} && gradle init \
  --type java-application \
  --dsl groovy \
  --test-framework junit \
  --package my.project \
  --project-name my-project