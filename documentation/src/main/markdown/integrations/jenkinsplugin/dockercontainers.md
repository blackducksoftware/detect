# Using Docker Containers - Best Practice
When running [detect_product_short] for Jenkins using the *DETECT\_JAR* environment variable in a pipeline that has a Docker agent, remember to mount the Detect JAR as a volume.

For example, if you've installed the jar on a node, the JAR won't be accessible from your docker agent unless you either put it somewhere you regularly
include, or if you mount the path to the jar. Refer to [Docker documentation](https://docs.docker.com/storage/bind-mounts/#choose-the--v-or---mount-flag) for more information.

This is accomplished by adding:
`-v $DETECT_JAR:$DETECT_JAR` to your Docker arguments, which is shown in the following example. 

```
pipeline {
    agent {
        docker {
            ...
            args '-v $DETECT_JAR:$DETECT_JAR'
        }
    ...
...
```

Applies when running [detect_product_short] for Jenkins with a local JAR (which requires setting the *DETECT\_JAR* environment variable).
