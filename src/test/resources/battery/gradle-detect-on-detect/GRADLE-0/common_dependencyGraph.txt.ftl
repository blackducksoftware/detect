
------------------------------------------------------------
Project ':common'
------------------------------------------------------------

annotationProcessor - Annotation processors and their dependencies for source set 'main'.
No dependencies

api - API dependencies for source set 'main'. (n)
No dependencies

apiElements - API elements for main. (n)
No dependencies

archives - Configuration for archive artifacts. (n)
No dependencies

compile - Dependencies for source set 'main' (deprecated, use 'implementation' instead). (n)
No dependencies

compileClasspath - Compile classpath for source set 'main'.
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10 -> 3.11
+--- org.jetbrains:annotations:19.0.0 -> 20.1.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (*)
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
\--- com.synopsys.integration:integration-common:24.0.0
     +--- org.apache.httpcomponents:httpclient:4.5.13
     |    +--- org.apache.httpcomponents:httpcore:4.4.13
     |    +--- commons-logging:commons-logging:1.2
     |    \--- commons-codec:commons-codec:1.11 -> 1.15
     +--- org.apache.httpcomponents:httpmime:4.5.13
     |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
     +--- org.apache.commons:commons-lang3:3.11
     +--- org.apache.commons:commons-text:1.9
     |    \--- org.apache.commons:commons-lang3:3.11
     +--- commons-io:commons-io:2.8.0
     +--- org.apache.commons:commons-compress:1.20
     +--- commons-codec:commons-codec:1.15
     +--- commons-beanutils:commons-beanutils:1.9.4
     |    +--- commons-logging:commons-logging:1.2
     |    \--- commons-collections:commons-collections:3.2.2
     +--- com.google.code.gson:gson:2.8.6
     +--- org.jetbrains:annotations:20.1.0
     +--- com.jayway.jsonpath:json-path:2.4.0
     |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
     |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
     +--- org.slf4j:slf4j-api:1.7.30
     \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
          +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.12.3 (*)
          +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.12.3 (*)
          \--- org.apache.commons:commons-collections4:4.2

compileOnly - Compile only dependencies for source set 'main'. (n)
No dependencies

compileOnlyApi - Compile only API dependencies for source set 'main'. (n)
No dependencies

default - Configuration for default artifacts. (n)
No dependencies

implementation - Implementation only dependencies for source set 'main'. (n)
+--- org.slf4j:slf4j-api:1.7.30 (n)
+--- org.apache.commons:commons-lang3:3.10 (n)
+--- org.jetbrains:annotations:19.0.0 (n)
+--- net.minidev:json-smart:2.4.2 (n)
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (n)
\--- com.synopsys.integration:integration-common:24.0.0 (n)

jacocoAgent - The Jacoco agent to use to get coverage data.
\--- org.jacoco:org.jacoco.agent:0.8.6

jacocoAnt - The Jacoco ant tasks to use to get execute Gradle tasks.
\--- org.jacoco:org.jacoco.ant:0.8.6
     +--- org.jacoco:org.jacoco.core:0.8.6
     |    +--- org.ow2.asm:asm:8.0.1
     |    +--- org.ow2.asm:asm-commons:8.0.1
     |    |    +--- org.ow2.asm:asm:8.0.1
     |    |    +--- org.ow2.asm:asm-tree:8.0.1
     |    |    |    \--- org.ow2.asm:asm:8.0.1
     |    |    \--- org.ow2.asm:asm-analysis:8.0.1
     |    |         \--- org.ow2.asm:asm-tree:8.0.1 (*)
     |    \--- org.ow2.asm:asm-tree:8.0.1 (*)
     +--- org.jacoco:org.jacoco.report:0.8.6
     |    \--- org.jacoco:org.jacoco.core:0.8.6 (*)
     \--- org.jacoco:org.jacoco.agent:0.8.6

runtime - Runtime dependencies for source set 'main' (deprecated, use 'runtimeOnly' instead). (n)
No dependencies

runtimeClasspath - Runtime classpath of source set 'main'.
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10 -> 3.11
+--- org.jetbrains:annotations:19.0.0 -> 20.1.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (*)
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
\--- com.synopsys.integration:integration-common:24.0.0
     +--- org.apache.httpcomponents:httpclient:4.5.13
     |    +--- org.apache.httpcomponents:httpcore:4.4.13
     |    +--- commons-logging:commons-logging:1.2
     |    \--- commons-codec:commons-codec:1.11 -> 1.15
     +--- org.apache.httpcomponents:httpmime:4.5.13
     |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
     +--- org.apache.commons:commons-lang3:3.11
     +--- org.apache.commons:commons-text:1.9
     |    \--- org.apache.commons:commons-lang3:3.11
     +--- commons-io:commons-io:2.8.0
     +--- org.apache.commons:commons-compress:1.20
     +--- commons-codec:commons-codec:1.15
     +--- commons-beanutils:commons-beanutils:1.9.4
     |    +--- commons-logging:commons-logging:1.2
     |    \--- commons-collections:commons-collections:3.2.2
     +--- com.google.code.gson:gson:2.8.6
     +--- org.jetbrains:annotations:20.1.0
     +--- com.jayway.jsonpath:json-path:2.4.0
     |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
     |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
     +--- org.slf4j:slf4j-api:1.7.30
     \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
          +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.12.3 (*)
          +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.12.3 (*)
          \--- org.apache.commons:commons-collections4:4.2

runtimeElements - Elements of runtime for main. (n)
No dependencies

runtimeOnly - Runtime only dependencies for source set 'main'. (n)
No dependencies

testAnnotationProcessor - Annotation processors and their dependencies for source set 'test'.
No dependencies

testCompile - Dependencies for source set 'test' (deprecated, use 'testImplementation' instead). (n)
No dependencies

testCompileClasspath - Compile classpath for source set 'test'.
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10 -> 3.11
+--- org.jetbrains:annotations:19.0.0 -> 20.1.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (*)
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
+--- com.synopsys.integration:integration-common:24.0.0
|    +--- org.apache.httpcomponents:httpclient:4.5.13
|    |    +--- org.apache.httpcomponents:httpcore:4.4.13
|    |    +--- commons-logging:commons-logging:1.2
|    |    \--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- org.apache.httpcomponents:httpmime:4.5.13
|    |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    +--- org.apache.commons:commons-lang3:3.11
|    +--- org.apache.commons:commons-text:1.9
|    |    \--- org.apache.commons:commons-lang3:3.11
|    +--- commons-io:commons-io:2.8.0
|    +--- org.apache.commons:commons-compress:1.20
|    +--- commons-codec:commons-codec:1.15
|    +--- commons-beanutils:commons-beanutils:1.9.4
|    |    +--- commons-logging:commons-logging:1.2
|    |    \--- commons-collections:commons-collections:3.2.2
|    +--- com.google.code.gson:gson:2.8.6
|    +--- org.jetbrains:annotations:20.1.0
|    +--- com.jayway.jsonpath:json-path:2.4.0
|    |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
|    |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|    +--- org.slf4j:slf4j-api:1.7.30
|    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
|         +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.12.3 (*)
|         +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.12.3 (*)
|         \--- org.apache.commons:commons-collections4:4.2
+--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    +--- org.junit:junit-bom:5.7.1
|    |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1 (c)
|    |    +--- org.junit.jupiter:junit-jupiter-params:5.7.1 (c)
|    |    \--- org.junit.platform:junit-platform-commons:1.7.1 (c)
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    +--- org.opentest4j:opentest4j:1.2.0
|    \--- org.junit.platform:junit-platform-commons:1.7.1
|         \--- org.apiguardian:apiguardian-api:1.1.0
+--- org.junit-pioneer:junit-pioneer:0.3.3
+--- org.junit.jupiter:junit-jupiter-params:5.4.2 -> 5.7.1
|    +--- org.junit:junit-bom:5.7.1 (*)
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    \--- org.junit.jupiter:junit-jupiter-api:5.7.1 (*)
+--- org.mockito:mockito-core:2.+ -> 2.28.2
|    +--- net.bytebuddy:byte-buddy:1.9.10
|    +--- net.bytebuddy:byte-buddy-agent:1.9.10
|    \--- org.objenesis:objenesis:2.6
\--- org.junit.jupiter:junit-jupiter-api:5.3.1 -> 5.7.1 (*)

testCompileOnly - Compile only dependencies for source set 'test'. (n)
No dependencies

testImplementation - Implementation only dependencies for source set 'test'. (n)
+--- org.junit.jupiter:junit-jupiter-api:5.7.1 (n)
+--- org.junit-pioneer:junit-pioneer:0.3.3 (n)
+--- org.junit.jupiter:junit-jupiter-params:5.4.2 (n)
+--- org.mockito:mockito-core:2.+ (n)
\--- org.junit.jupiter:junit-jupiter-api:5.3.1 (n)

testRuntime - Runtime dependencies for source set 'test' (deprecated, use 'testRuntimeOnly' instead). (n)
No dependencies

testRuntimeClasspath - Runtime classpath of source set 'test'.
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10 -> 3.11
+--- org.jetbrains:annotations:19.0.0 -> 20.1.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.12.3
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    |    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 (*)
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3 (*)
+--- com.synopsys.integration:integration-common:24.0.0
|    +--- org.apache.httpcomponents:httpclient:4.5.13
|    |    +--- org.apache.httpcomponents:httpcore:4.4.13
|    |    +--- commons-logging:commons-logging:1.2
|    |    \--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- org.apache.httpcomponents:httpmime:4.5.13
|    |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    +--- org.apache.commons:commons-lang3:3.11
|    +--- org.apache.commons:commons-text:1.9
|    |    \--- org.apache.commons:commons-lang3:3.11
|    +--- commons-io:commons-io:2.8.0
|    +--- org.apache.commons:commons-compress:1.20
|    +--- commons-codec:commons-codec:1.15
|    +--- commons-beanutils:commons-beanutils:1.9.4
|    |    +--- commons-logging:commons-logging:1.2
|    |    \--- commons-collections:commons-collections:3.2.2
|    +--- com.google.code.gson:gson:2.8.6
|    +--- org.jetbrains:annotations:20.1.0
|    +--- com.jayway.jsonpath:json-path:2.4.0
|    |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
|    |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|    +--- org.slf4j:slf4j-api:1.7.30
|    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
|         +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.12.3 (*)
|         +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.12.3 (*)
|         \--- org.apache.commons:commons-collections4:4.2
+--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    +--- org.junit:junit-bom:5.7.1
|    |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1 (c)
|    |    +--- org.junit.jupiter:junit-jupiter-engine:5.7.1 (c)
|    |    +--- org.junit.jupiter:junit-jupiter-params:5.7.1 (c)
|    |    +--- org.junit.platform:junit-platform-commons:1.7.1 (c)
|    |    \--- org.junit.platform:junit-platform-engine:1.7.1 (c)
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    +--- org.opentest4j:opentest4j:1.2.0
|    \--- org.junit.platform:junit-platform-commons:1.7.1
|         \--- org.apiguardian:apiguardian-api:1.1.0
+--- org.junit-pioneer:junit-pioneer:0.3.3
|    \--- org.junit.jupiter:junit-jupiter-api:5.1.1 -> 5.7.1 (*)
+--- org.junit.jupiter:junit-jupiter-params:5.4.2 -> 5.7.1
|    +--- org.junit:junit-bom:5.7.1 (*)
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    \--- org.junit.jupiter:junit-jupiter-api:5.7.1 (*)
+--- org.mockito:mockito-core:2.+ -> 2.28.2
|    +--- net.bytebuddy:byte-buddy:1.9.10
|    +--- net.bytebuddy:byte-buddy-agent:1.9.10
|    \--- org.objenesis:objenesis:2.6
+--- org.junit.jupiter:junit-jupiter-api:5.3.1 -> 5.7.1 (*)
\--- org.junit.jupiter:junit-jupiter-engine:5.7.1
     +--- org.junit:junit-bom:5.7.1 (*)
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
rootProjectVersion:11.1.0-SIGQA8-SNAPSHOT
projectDirectory:${sourcePath?replace("\\", "/")}/common
projectGroup:com.blackduck.integration
projectName:common
projectVersion:11.1.0-SIGQA8-SNAPSHOT
projectParent:root project 'detect'
DETECT META DATA END
