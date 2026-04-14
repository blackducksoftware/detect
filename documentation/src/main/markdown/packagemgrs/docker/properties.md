# Passing Docker Inspector property values to Docker Inspector from [detect_product_short]

For more complex use cases, you may need to pass Docker Inspector property values to Docker Inspector using [detect_product_short]. To do this, construct the [detect_product_short] property name by prefixing the Docker Inspector property name with ```detect.docker.passthrough.```.

For example, suppose you need to set Docker Inspector's `service.timeout` value (the length of time Docker Inspector waits for a response from the Image Inspector services that it uses) to 480000 milliseconds. You add the prefix to the Docker Inspector property name to derive the [detect_product_short] property name ```detect.docker.passthrough.service.timeout```. Therefore, add ```--detect.docker.passthrough.service.timeout=480000``` to the [detect_product_short] command line.

For example:
```
./detect11.sh --detect.docker.image=ubuntu:latest --detect.docker.passthrough.service.timeout=480000
```

You can set any Docker Inspector property using this method.
However, you usually should not override the values of the following Docker Inspector properties (which [detect_product_short] sets)
because changing their values is likely to interfere with [detect_product_short]'s ability to work with Docker Inspector:

* output.path
* output.include.squashedimage
* output.include.containerfilesystem
* upload.bdio
