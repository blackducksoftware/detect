
------------------------------------------------------------
Project ':documentation'
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
\--- org.apache.httpcomponents:httpclient-osgi:4.5.14
     +--- org.apache.httpcomponents:httpclient:4.5.14
     |    +--- org.apache.httpcomponents:httpcore:4.4.16
     |    +--- commons-logging:commons-logging:1.2
     |    \--- commons-codec:commons-codec:1.11
     +--- commons-codec:commons-codec:1.11
     +--- org.apache.httpcomponents:httpmime:4.5.14
     |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
     +--- org.apache.httpcomponents:httpclient-cache:4.5.14
     |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
     |    \--- commons-logging:commons-logging:1.2
     \--- org.apache.httpcomponents:fluent-hc:4.5.14
          +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
          \--- commons-logging:commons-logging:1.2

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
\--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (n)

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
\--- org.apache.httpcomponents:httpclient-osgi:4.5.14
     +--- org.apache.httpcomponents:httpclient:4.5.14
     |    +--- org.apache.httpcomponents:httpcore:4.4.16
     |    +--- commons-logging:commons-logging:1.2
     |    \--- commons-codec:commons-codec:1.11
     +--- commons-codec:commons-codec:1.11
     +--- org.apache.httpcomponents:httpmime:4.5.14
     |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
     +--- org.apache.httpcomponents:httpclient-cache:4.5.14
     |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
     |    \--- commons-logging:commons-logging:1.2
     \--- org.apache.httpcomponents:fluent-hc:4.5.14
          +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
          \--- commons-logging:commons-logging:1.2

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
|    |    +--- commons-logging:commons-logging:1.2
|    |    \--- commons-codec:commons-codec:1.11
|    +--- commons-codec:commons-codec:1.11
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2
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
|    |    +--- commons-logging:commons-logging:1.2
|    |    \--- commons-codec:commons-codec:1.11
|    +--- commons-codec:commons-codec:1.11
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2
+--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    +--- org.opentest4j:opentest4j:1.2.0
|    \--- org.junit.platform:junit-platform-commons:1.7.1
|         \--- org.apiguardian:apiguardian-api:1.1.0
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
rootProjectPath::
rootProjectName:detect
rootProjectVersion:11.1.0-SIGQA11-SNAPSHOT
projectDirectory:${sourcePath?replace("\\", "/")}/documentation
projectGroup:com.blackduck.integration
projectName:documentation
projectVersion:11.1.0-SIGQA11-SNAPSHOT
projectPath::documentation
projectParent:root project 'detect'
DETECT META DATA END
