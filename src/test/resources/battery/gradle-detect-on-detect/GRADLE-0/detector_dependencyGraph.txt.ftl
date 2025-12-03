
------------------------------------------------------------
Project ':detector'
------------------------------------------------------------

annotationProcessor - Annotation processors and their dependencies for source set 'main'.
No dependencies

api - API dependencies for source set 'main'. (n)
No dependencies

apiElements - API elements for main. (n)
No dependencies

archives - Configuration for archive artifacts. (n)
No dependencies

compileClasspath - Compile classpath for source set 'main'.
+--- com.google.guava:guava:32.1.2-jre
|    +--- com.google.guava:failureaccess:1.0.1
|    +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    +--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.checkerframework:checker-qual:3.33.0
|    +--- com.google.errorprone:error_prone_annotations:2.18.0
|    \--- com.google.j2objc:j2objc-annotations:2.8
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0
|         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 (c)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :detectable
+--- project :common
|    \--- com.blackduck.integration:blackduck-common:67.0.20
|         +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|         |    \--- com.blackduck.integration:integration-rest:11.1.2
|         |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|         |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|         |         |    +--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|         |         |    +--- org.apache.commons:commons-text:1.10.0
|         |         |    |    \--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|         |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|         |         |    +--- org.apache.commons:commons-compress:1.26.1
|         |         |    |    +--- commons-codec:commons-codec:1.16.1
|         |         |    |    +--- commons-io:commons-io:2.15.1
|         |         |    |    \--- org.apache.commons:commons-lang3:3.14.0
|         |         |    +--- commons-codec:commons-codec:1.15 -> 1.16.1
|         |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|         |         |    |    +--- commons-logging:commons-logging:1.3.5
|         |         |    |    \--- commons-collections:commons-collections:3.2.2
|         |         |    +--- org.apache.commons:commons-collections4:4.4
|         |         |    +--- com.google.code.gson:gson:2.10.1
|         |         |    +--- org.jetbrains:annotations:24.0.1
|         |         |    +--- com.jayway.jsonpath:json-path:2.9.0
|         |         |    +--- org.slf4j:slf4j-api:2.0.7
|         |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|         |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.15.0 (*)
|         |         |         +--- com.fasterxml.jackson.core:jackson-core:2.14.0 -> 2.15.0
|         |         |         \--- org.apache.commons:commons-collections4:4.4
|         |         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         |         \--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|         +--- com.blackduck.integration:phone-home-client:7.0.1
|         |    \--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3 (*)
|         +--- com.blackduck.integration:integration-bdio:27.0.4
|         |    \--- com.blackduck.integration:integration-common:27.0.3 (*)
|         \--- com.blackducksoftware.bdio:bdio2:3.2.12
|              +--- com.blackducksoftware.magpie:magpie:0.6.0
|              |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|              |    \--- com.google.guava:guava:23.3-jre -> 32.1.2-jre (*)
|              +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|              +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|              +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (*)
|              +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|              +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|              +--- com.github.jsonld-java:jsonld-java:0.12.3
|              |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|              |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.15.0 (*)
|              |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|              |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|              |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|              |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10
|              |    |         \--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|              |    +--- org.slf4j:slf4j-api:1.7.25 -> 2.0.7
|              |    \--- commons-io:commons-io:2.6 -> 2.15.1
|              \--- org.reactivestreams:reactive-streams:1.0.2
\--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)

compileOnly - Compile only dependencies for source set 'main'. (n)
No dependencies

compileOnlyApi - Compile only API dependencies for source set 'main'. (n)
No dependencies

default - Configuration for default artifacts. (n)
No dependencies

implementation - Implementation only dependencies for source set 'main'. (n)
+--- com.google.guava:guava:32.1.2-jre (n)
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (n)
+--- org.yaml:snakeyaml:2.0 (n)
+--- com.fasterxml.jackson.core:jackson-core:2.15.0 (n)
+--- org.freemarker:freemarker:2.3.31 (n)
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (n)
+--- project detectable (n)
+--- project common (n)
\--- com.blackduck.integration:integration-bdio (n)

jacocoAgent - The Jacoco agent to use to get coverage data.
\--- org.jacoco:org.jacoco.agent:0.8.7

jacocoAnt - The Jacoco ant tasks to use to get execute Gradle tasks.
\--- org.jacoco:org.jacoco.ant:0.8.7
     +--- org.jacoco:org.jacoco.core:0.8.7
     |    +--- org.ow2.asm:asm:9.1
     |    +--- org.ow2.asm:asm-commons:9.1
     |    |    +--- org.ow2.asm:asm:9.1
     |    |    +--- org.ow2.asm:asm-tree:9.1
     |    |    |    \--- org.ow2.asm:asm:9.1
     |    |    \--- org.ow2.asm:asm-analysis:9.1
     |    |         \--- org.ow2.asm:asm-tree:9.1 (*)
     |    \--- org.ow2.asm:asm-tree:9.1 (*)
     +--- org.jacoco:org.jacoco.report:0.8.7
     |    \--- org.jacoco:org.jacoco.core:0.8.7 (*)
     \--- org.jacoco:org.jacoco.agent:0.8.7

runtimeClasspath - Runtime classpath of source set 'main'.
+--- com.google.guava:guava:32.1.2-jre
|    +--- com.google.guava:failureaccess:1.0.1
|    +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    +--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.checkerframework:checker-qual:3.33.0
|    +--- com.google.errorprone:error_prone_annotations:2.18.0
|    \--- com.google.j2objc:j2objc-annotations:2.8
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0
|         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 (c)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :detectable
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common
|    |    +--- com.blackduck.integration:blackduck-common:67.0.20
|    |    |    +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|    |    |    |    \--- com.blackduck.integration:integration-rest:11.1.2
|    |    |    |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|    |    |    |         |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    |    |    |         |    |    +--- org.apiguardian:apiguardian-api:1.1.0
|    |    |    |         |    |    +--- org.opentest4j:opentest4j:1.2.0
|    |    |    |         |    |    \--- org.junit.platform:junit-platform-commons:1.7.1
|    |    |    |         |    |         \--- org.apiguardian:apiguardian-api:1.1.0
|    |    |    |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |    |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    |    |         |    +--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|    |    |    |         |    +--- org.apache.commons:commons-text:1.10.0
|    |    |    |         |    |    \--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|    |    |    |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |         |    +--- org.apache.commons:commons-compress:1.26.1
|    |    |    |         |    |    +--- commons-codec:commons-codec:1.16.1
|    |    |    |         |    |    +--- commons-io:commons-io:2.15.1
|    |    |    |         |    |    \--- org.apache.commons:commons-lang3:3.14.0
|    |    |    |         |    +--- commons-codec:commons-codec:1.15 -> 1.16.1
|    |    |    |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|    |    |    |         |    |    +--- commons-logging:commons-logging:1.3.5
|    |    |    |         |    |    \--- commons-collections:commons-collections:3.2.2
|    |    |    |         |    +--- org.apache.commons:commons-collections4:4.4
|    |    |    |         |    +--- com.google.code.gson:gson:2.10.1
|    |    |    |         |    +--- org.jetbrains:annotations:24.0.1
|    |    |    |         |    +--- com.jayway.jsonpath:json-path:2.9.0
|    |    |    |         |    |    +--- net.minidev:json-smart:2.5.0
|    |    |    |         |    |    |    \--- net.minidev:accessors-smart:2.5.0
|    |    |    |         |    |    |         \--- org.ow2.asm:asm:9.3
|    |    |    |         |    |    \--- org.slf4j:slf4j-api:2.0.11
|    |    |    |         |    +--- org.slf4j:slf4j-api:2.0.7 -> 2.0.11
|    |    |    |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|    |    |    |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.15.0 (*)
|    |    |    |         |         +--- com.fasterxml.jackson.core:jackson-core:2.14.0 -> 2.15.0
|    |    |    |         |         \--- org.apache.commons:commons-collections4:4.4
|    |    |    |         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |    |         \--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    |    +--- com.blackduck.integration:phone-home-client:7.0.1
|    |    |    |    \--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3 (*)
|    |    |    +--- com.blackduck.integration:integration-bdio:27.0.4
|    |    |    |    \--- com.blackduck.integration:integration-common:27.0.3 (*)
|    |    |    \--- com.blackducksoftware.bdio:bdio2:3.2.12
|    |    |         +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |    |         |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    |         |    \--- com.google.guava:guava:23.3-jre -> 32.1.2-jre (*)
|    |    |         +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (*)
|    |    |         +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    |         +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    |    |         +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |    |         |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|    |    |         |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.15.0 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |    |         |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|    |    |         |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10
|    |    |         |    |         \--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|    |    |         |    +--- org.slf4j:slf4j-api:1.7.25 -> 2.0.11
|    |    |         |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.30
|    |    |         |    |    \--- org.slf4j:slf4j-api:1.7.30 -> 2.0.11
|    |    |         |    \--- commons-io:commons-io:2.6 -> 2.15.1
|    |    |         \--- org.reactivestreams:reactive-streams:1.0.2
|    |    +--- com.google.guava:guava:32.1.2-jre (*)
|    |    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    |    +--- org.yaml:snakeyaml:2.0
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    |    +--- org.freemarker:freemarker:2.3.31
|    |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    |    \--- com.blackduck.integration:integration-common -> 27.0.3 (*)
|    +--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
|    +--- com.blackduck.integration:integration-rest -> 11.1.2 (*)
|    +--- org.tomlj:tomlj:1.1.1
|    |    +--- org.antlr:antlr4-runtime:4.11.1
|    |    \--- org.checkerframework:checker-qual:3.21.2 -> 3.33.0
|    +--- com.moandjiezana.toml:toml4j:0.7.2
|    |    \--- com.google.code.gson:gson:2.8.1 -> 2.10.1
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.11.1
|    \--- guru.nidi:graphviz-java:0.18.1
|         +--- org.webjars.npm:viz.js-graphviz-java:2.1.3
|         +--- guru.nidi.com.kitfox:svgSalamander:1.1.3
|         +--- net.arnx:nashorn-promise:0.1.1
|         +--- org.apache.commons:commons-exec:1.3
|         +--- com.google.code.findbugs:jsr305:3.0.2
|         +--- org.slf4j:jcl-over-slf4j:1.7.30 (*)
|         +--- org.slf4j:jul-to-slf4j:1.7.30
|         |    \--- org.slf4j:slf4j-api:1.7.30 -> 2.0.11
|         \--- org.slf4j:slf4j-api:1.7.30 -> 2.0.11
+--- project :common (*)
\--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)

runtimeElements - Elements of runtime for main. (n)
No dependencies

runtimeOnly - Runtime only dependencies for source set 'main'. (n)
No dependencies

testAnnotationProcessor - Annotation processors and their dependencies for source set 'test'.
No dependencies

testCompileClasspath - Compile classpath for source set 'test'.
+--- com.google.guava:guava:32.1.2-jre
|    +--- com.google.guava:failureaccess:1.0.1
|    +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    +--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.checkerframework:checker-qual:3.33.0
|    +--- com.google.errorprone:error_prone_annotations:2.18.0
|    \--- com.google.j2objc:j2objc-annotations:2.8
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0
|         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 (c)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :detectable
+--- project :common
|    \--- com.blackduck.integration:blackduck-common:67.0.20
|         +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|         |    \--- com.blackduck.integration:integration-rest:11.1.2
|         |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|         |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|         |         |    +--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|         |         |    +--- org.apache.commons:commons-text:1.10.0
|         |         |    |    \--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|         |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|         |         |    +--- org.apache.commons:commons-compress:1.26.1
|         |         |    |    +--- commons-codec:commons-codec:1.16.1
|         |         |    |    +--- commons-io:commons-io:2.15.1
|         |         |    |    \--- org.apache.commons:commons-lang3:3.14.0
|         |         |    +--- commons-codec:commons-codec:1.15 -> 1.16.1
|         |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|         |         |    |    +--- commons-logging:commons-logging:1.3.5
|         |         |    |    \--- commons-collections:commons-collections:3.2.2
|         |         |    +--- org.apache.commons:commons-collections4:4.4
|         |         |    +--- com.google.code.gson:gson:2.10.1
|         |         |    +--- org.jetbrains:annotations:24.0.1
|         |         |    +--- com.jayway.jsonpath:json-path:2.9.0
|         |         |    +--- org.slf4j:slf4j-api:2.0.7
|         |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|         |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.15.0 (*)
|         |         |         +--- com.fasterxml.jackson.core:jackson-core:2.14.0 -> 2.15.0
|         |         |         \--- org.apache.commons:commons-collections4:4.4
|         |         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         |         \--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|         +--- com.blackduck.integration:phone-home-client:7.0.1
|         |    \--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3 (*)
|         +--- com.blackduck.integration:integration-bdio:27.0.4
|         |    \--- com.blackduck.integration:integration-common:27.0.3 (*)
|         \--- com.blackducksoftware.bdio:bdio2:3.2.12
|              +--- com.blackducksoftware.magpie:magpie:0.6.0
|              |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|              |    \--- com.google.guava:guava:23.3-jre -> 32.1.2-jre (*)
|              +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|              +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|              +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (*)
|              +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|              +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|              +--- com.github.jsonld-java:jsonld-java:0.12.3
|              |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|              |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.15.0 (*)
|              |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|              |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|              |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|              |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10
|              |    |         \--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|              |    +--- org.slf4j:slf4j-api:1.7.25 -> 2.0.7
|              |    \--- commons-io:commons-io:2.6 -> 2.15.1
|              \--- org.reactivestreams:reactive-streams:1.0.2
+--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
+--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    +--- org.opentest4j:opentest4j:1.2.0
|    \--- org.junit.platform:junit-platform-commons:1.7.1
|         \--- org.apiguardian:apiguardian-api:1.1.0
+--- org.junit-pioneer:junit-pioneer:0.3.3
+--- org.junit.jupiter:junit-jupiter-params:5.4.2
|    +--- org.apiguardian:apiguardian-api:1.0.0 -> 1.1.0
|    \--- org.junit.jupiter:junit-jupiter-api:5.4.2 -> 5.7.1 (*)
\--- org.mockito:mockito-core:2.+ -> 2.28.2
     +--- net.bytebuddy:byte-buddy:1.9.10
     +--- net.bytebuddy:byte-buddy-agent:1.9.10
     \--- org.objenesis:objenesis:2.6

testCompileOnly - Compile only dependencies for source set 'test'. (n)
No dependencies

testImplementation - Implementation only dependencies for source set 'test'. (n)
+--- org.junit.jupiter:junit-jupiter-api:5.7.1 (n)
+--- org.junit-pioneer:junit-pioneer:0.3.3 (n)
+--- org.junit.jupiter:junit-jupiter-params:5.4.2 (n)
\--- org.mockito:mockito-core:2.+ (n)

testRuntimeClasspath - Runtime classpath of source set 'test'.
+--- com.google.guava:guava:32.1.2-jre
|    +--- com.google.guava:failureaccess:1.0.1
|    +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    +--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.checkerframework:checker-qual:3.33.0
|    +--- com.google.errorprone:error_prone_annotations:2.18.0
|    \--- com.google.j2objc:j2objc-annotations:2.8
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0
|         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 (c)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- commons-codec:commons-codec:1.11 -> 1.16.1
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :detectable
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common
|    |    +--- com.blackduck.integration:blackduck-common:67.0.20
|    |    |    +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|    |    |    |    \--- com.blackduck.integration:integration-rest:11.1.2
|    |    |    |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|    |    |    |         |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    |    |    |         |    |    +--- org.apiguardian:apiguardian-api:1.1.0
|    |    |    |         |    |    +--- org.opentest4j:opentest4j:1.2.0
|    |    |    |         |    |    \--- org.junit.platform:junit-platform-commons:1.7.1
|    |    |    |         |    |         \--- org.apiguardian:apiguardian-api:1.1.0
|    |    |    |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |    |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    |    |         |    +--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|    |    |    |         |    +--- org.apache.commons:commons-text:1.10.0
|    |    |    |         |    |    \--- org.apache.commons:commons-lang3:3.12.0 -> 3.14.0
|    |    |    |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |         |    +--- org.apache.commons:commons-compress:1.26.1
|    |    |    |         |    |    +--- commons-codec:commons-codec:1.16.1
|    |    |    |         |    |    +--- commons-io:commons-io:2.15.1
|    |    |    |         |    |    \--- org.apache.commons:commons-lang3:3.14.0
|    |    |    |         |    +--- commons-codec:commons-codec:1.15 -> 1.16.1
|    |    |    |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|    |    |    |         |    |    +--- commons-logging:commons-logging:1.3.5
|    |    |    |         |    |    \--- commons-collections:commons-collections:3.2.2
|    |    |    |         |    +--- org.apache.commons:commons-collections4:4.4
|    |    |    |         |    +--- com.google.code.gson:gson:2.10.1
|    |    |    |         |    +--- org.jetbrains:annotations:24.0.1
|    |    |    |         |    +--- com.jayway.jsonpath:json-path:2.9.0
|    |    |    |         |    |    +--- net.minidev:json-smart:2.5.0
|    |    |    |         |    |    |    \--- net.minidev:accessors-smart:2.5.0
|    |    |    |         |    |    |         \--- org.ow2.asm:asm:9.3
|    |    |    |         |    |    \--- org.slf4j:slf4j-api:2.0.11
|    |    |    |         |    +--- org.slf4j:slf4j-api:2.0.7 -> 2.0.11
|    |    |    |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|    |    |    |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.15.0 (*)
|    |    |    |         |         +--- com.fasterxml.jackson.core:jackson-core:2.14.0 -> 2.15.0
|    |    |    |         |         \--- org.apache.commons:commons-collections4:4.4
|    |    |    |         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |    |         \--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    |    +--- com.blackduck.integration:phone-home-client:7.0.1
|    |    |    |    \--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3 (*)
|    |    |    +--- com.blackduck.integration:integration-bdio:27.0.4
|    |    |    |    \--- com.blackduck.integration:integration-common:27.0.3 (*)
|    |    |    \--- com.blackducksoftware.bdio:bdio2:3.2.12
|    |    |         +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |    |         |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    |         |    \--- com.google.guava:guava:23.3-jre -> 32.1.2-jre (*)
|    |    |         +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 (*)
|    |    |         +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    |         +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    |    |         +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |    |         |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|    |    |         |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.15.0 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |    |         |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|    |    |         |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10
|    |    |         |    |         \--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|    |    |         |    +--- org.slf4j:slf4j-api:1.7.25 -> 2.0.11
|    |    |         |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.30
|    |    |         |    |    \--- org.slf4j:slf4j-api:1.7.30 -> 2.0.11
|    |    |         |    \--- commons-io:commons-io:2.6 -> 2.15.1
|    |    |         \--- org.reactivestreams:reactive-streams:1.0.2
|    |    +--- com.google.guava:guava:32.1.2-jre (*)
|    |    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    |    +--- org.yaml:snakeyaml:2.0
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    |    +--- org.freemarker:freemarker:2.3.31
|    |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    |    \--- com.blackduck.integration:integration-common -> 27.0.3 (*)
|    +--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
|    +--- com.blackduck.integration:integration-rest -> 11.1.2 (*)
|    +--- org.tomlj:tomlj:1.1.1
|    |    +--- org.antlr:antlr4-runtime:4.11.1
|    |    \--- org.checkerframework:checker-qual:3.21.2 -> 3.33.0
|    +--- com.moandjiezana.toml:toml4j:0.7.2
|    |    \--- com.google.code.gson:gson:2.8.1 -> 2.10.1
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.11.1
|    \--- guru.nidi:graphviz-java:0.18.1
|         +--- org.webjars.npm:viz.js-graphviz-java:2.1.3
|         +--- guru.nidi.com.kitfox:svgSalamander:1.1.3
|         +--- net.arnx:nashorn-promise:0.1.1
|         +--- org.apache.commons:commons-exec:1.3
|         +--- com.google.code.findbugs:jsr305:3.0.2
|         +--- org.slf4j:jcl-over-slf4j:1.7.30 (*)
|         +--- org.slf4j:jul-to-slf4j:1.7.30
|         |    \--- org.slf4j:slf4j-api:1.7.30 -> 2.0.11
|         \--- org.slf4j:slf4j-api:1.7.30 -> 2.0.11
+--- project :common (*)
+--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
+--- org.junit.jupiter:junit-jupiter-api:5.7.1 (*)
+--- org.junit-pioneer:junit-pioneer:0.3.3
|    \--- org.junit.jupiter:junit-jupiter-api:5.1.1 -> 5.7.1 (*)
+--- org.junit.jupiter:junit-jupiter-params:5.4.2
|    +--- org.apiguardian:apiguardian-api:1.0.0 -> 1.1.0
|    \--- org.junit.jupiter:junit-jupiter-api:5.4.2 -> 5.7.1 (*)
+--- org.mockito:mockito-core:2.+ -> 2.28.2
|    +--- net.bytebuddy:byte-buddy:1.9.10
|    +--- net.bytebuddy:byte-buddy-agent:1.9.10
|    \--- org.objenesis:objenesis:2.6
\--- org.junit.jupiter:junit-jupiter-engine:5.7.1
     +--- org.apiguardian:apiguardian-api:1.1.0
     +--- org.junit.platform:junit-platform-engine:1.7.1
     |    +--- org.apiguardian:apiguardian-api:1.1.0
     |    +--- org.opentest4j:opentest4j:1.2.0
     |    \--- org.junit.platform:junit-platform-commons:1.7.1 (*)
     \--- org.junit.jupiter:junit-jupiter-api:5.7.1 (*)

testRuntimeOnly - Runtime only dependencies for source set 'test'. (n)
\--- org.junit.jupiter:junit-jupiter-engine:5.7.1 (n)

(c) - dependency constraint
(*) - dependencies omitted (listed previously)

(n) - Not resolved (configuration is not meant to be resolved)

A web-based, searchable dependency report is available by adding the --scan option.

DETECT META DATA START
rootProjectDirectory:${sourcePath?replace("\\", "/")}
rootProjectGroup:com.blackduck.integration
rootProjectName:detect
rootProjectVersion:11.1.0-SIGQA10-SNAPSHOT
projectDirectory:${sourcePath?replace("\\", "/")}/common
projectGroup:com.blackduck.integration
projectName:detector
projectVersion:11.1.0-SIGQA10-SNAPSHOT
projectParent:root project 'detect'
DETECT META DATA END
