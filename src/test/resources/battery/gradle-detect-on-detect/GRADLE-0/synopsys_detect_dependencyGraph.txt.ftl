
------------------------------------------------------------
Root project 'synopsys-detect'
------------------------------------------------------------

annotationProcessor - Annotation processors and their dependencies for source set 'main'.
No dependencies

apiElements - API elements for main. (n)
No dependencies

archives - Configuration for archive artifacts. (n)
No dependencies

bootArchives - Configuration for Spring Boot archive artifacts. (n)
No dependencies

compile - Dependencies for source set 'main' (deprecated, use 'implementation' instead). (n)
No dependencies

compileClasspath - Compile classpath for source set 'main'.
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10
+--- org.jetbrains:annotations:19.0.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.11.4
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.11.4
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 -> 2.11.4 (c)
+--- project :common
+--- project :configuration
+--- project :detectable
|    \--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0
|         +--- org.apache.httpcomponents:httpclient:4.5.13
|         |    +--- org.apache.httpcomponents:httpcore:4.4.13 -> 4.4.14
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-codec:commons-codec:1.11 -> 1.15
|         +--- org.apache.httpcomponents:httpmime:4.5.13
|         |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         +--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- org.apache.commons:commons-text:1.9
|         |    \--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- commons-io:commons-io:2.8.0
|         +--- org.apache.commons:commons-compress:1.20
|         +--- commons-codec:commons-codec:1.15
|         +--- commons-beanutils:commons-beanutils:1.9.4
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-collections:commons-collections:3.2.2
|         +--- com.google.code.gson:gson:2.8.6
|         +--- org.jetbrains:annotations:20.1.0 -> 19.0.0
|         +--- com.jayway.jsonpath:json-path:2.6.0 -> 2.4.0
|         |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
|         |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|         +--- org.slf4j:slf4j-api:1.7.30
|         \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
|              +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.11.4 (*)
|              +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.11.4
|              \--- org.apache.commons:commons-collections4:4.2
+--- project :detector
+--- ch.qos.logback:logback-classic:1.2.3
|    +--- ch.qos.logback:logback-core:1.2.3
|    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
+--- com.esotericsoftware.yamlbeans:yamlbeans:1.11
+--- com.synopsys.integration:blackduck-common:57.0.0
|    +--- com.synopsys.integration:blackduck-common-api:2021.6.0.1
|    |    \--- com.synopsys.integration:integration-rest:10.1.0
|    |         +--- com.synopsys.integration:integration-common:25.2.0 (*)
|    |         +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |         \--- org.apache.httpcomponents:httpmime:4.5.13 (*)
|    +--- com.synopsys.integration:integration-bdio:22.1.4
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    +--- com.synopsys.integration:phone-home-client:5.0.0
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    \--- com.blackducksoftware.bdio:bdio2:3.0.0-beta.47
|         +--- com.blackducksoftware.magpie:magpie:0.6.0
|         |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|         |    \--- com.google.guava:guava:23.3-jre -> 29.0-jre
|         |         +--- com.google.guava:failureaccess:1.0.1
|         |         +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|         |         +--- com.google.code.findbugs:jsr305:3.0.2
|         |         +--- org.checkerframework:checker-qual:2.11.1
|         |         +--- com.google.errorprone:error_prone_annotations:2.3.4
|         |         \--- com.google.j2objc:j2objc-annotations:1.3
|         +--- com.fasterxml.jackson.core:jackson-annotations:2.9.0 -> 2.11.4
|         +--- com.fasterxml.jackson.core:jackson-core:2.9.8 -> 2.11.4
|         +--- com.fasterxml.jackson.core:jackson-databind:2.9.8 -> 2.11.4 (*)
|         +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|         +--- com.google.guava:guava:23.3-jre -> 29.0-jre (*)
|         +--- com.github.jsonld-java:jsonld-java:0.12.3
|         |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.11.4
|         |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.11.4 (*)
|         |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.13
|         |    |    +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         |    |    +--- commons-codec:commons-codec:1.11 -> 1.15
|         |    |    +--- org.apache.httpcomponents:httpmime:4.5.13 (*)
|         |    |    +--- org.apache.httpcomponents:httpclient-cache:4.5.13
|         |    |    |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         |    |    \--- org.apache.httpcomponents:fluent-hc:4.5.13
|         |    |         \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|         |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.14
|         |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.14
|         |    |         \--- org.apache.httpcomponents:httpcore:4.4.14
|         |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|         |    \--- commons-io:commons-io:2.6 -> 2.8.0
|         \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.3
+--- com.synopsys:method-analyzer-core:0.1.0
|    +--- com.google.guava:guava:29.0-jre (*)
|    \--- org.ow2.asm:asm:8.0.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework.boot:spring-boot -> 2.4.5
|    +--- org.springframework:spring-core:5.3.6
|    |    \--- org.springframework:spring-jcl:5.3.6
|    \--- org.springframework:spring-context:5.3.6
|         +--- org.springframework:spring-aop:5.3.6
|         |    +--- org.springframework:spring-beans:5.3.6
|         |    |    \--- org.springframework:spring-core:5.3.6 (*)
|         |    \--- org.springframework:spring-core:5.3.6 (*)
|         +--- org.springframework:spring-beans:5.3.6 (*)
|         +--- org.springframework:spring-core:5.3.6 (*)
|         \--- org.springframework:spring-expression:5.3.6
|              \--- org.springframework:spring-core:5.3.6 (*)
+--- org.yaml:snakeyaml:1.27
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.30
+--- org.freemarker:freemarker:2.3.26-incubating
\--- org.apache.pdfbox:pdfbox:2.0.21
     +--- org.apache.pdfbox:fontbox:2.0.21
     |    \--- commons-logging:commons-logging:1.2
     \--- commons-logging:commons-logging:1.2

compileOnly - Compile only dependencies for source set 'main'. (n)
No dependencies

default - Configuration for default artifacts. (n)
No dependencies

developmentOnly - Configuration for development-only dependencies such as Spring Boot's DevTools.
No dependencies

implementation - Implementation only dependencies for source set 'main'. (n)
+--- org.slf4j:slf4j-api:1.7.30 (n)
+--- org.apache.commons:commons-lang3:3.10 (n)
+--- org.jetbrains:annotations:19.0.0 (n)
+--- net.minidev:json-smart:2.4.2 (n)
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (n)
+--- project common (n)
+--- project configuration (n)
+--- project detectable (n)
+--- project detector (n)
+--- ch.qos.logback:logback-classic:1.2.3 (n)
+--- com.esotericsoftware.yamlbeans:yamlbeans:1.11 (n)
+--- com.synopsys.integration:blackduck-common:57.0.0 (n)
+--- com.synopsys:method-analyzer-core:0.1.0 (n)
+--- org.apache.maven.shared:maven-invoker:3.0.0 (n)
+--- org.springframework.boot:spring-boot (n)
+--- org.yaml:snakeyaml:1.27 (n)
+--- org.zeroturnaround:zt-zip:1.13 (n)
+--- org.freemarker:freemarker:2.3.26-incubating (n)
\--- org.apache.pdfbox:pdfbox:2.0.21 (n)

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

productionRuntimeClasspath
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10
+--- org.jetbrains:annotations:19.0.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.11.4
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.11.4
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 -> 2.11.4 (c)
+--- project :common
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    \--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0
|         +--- org.apache.httpcomponents:httpclient:4.5.13
|         |    +--- org.apache.httpcomponents:httpcore:4.4.13 -> 4.4.14
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-codec:commons-codec:1.11 -> 1.15
|         +--- org.apache.httpcomponents:httpmime:4.5.13
|         |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         +--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- org.apache.commons:commons-text:1.9
|         |    \--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- commons-io:commons-io:2.8.0
|         +--- org.apache.commons:commons-compress:1.20
|         +--- commons-codec:commons-codec:1.15
|         +--- commons-beanutils:commons-beanutils:1.9.4
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-collections:commons-collections:3.2.2
|         +--- com.google.code.gson:gson:2.8.6
|         +--- org.jetbrains:annotations:20.1.0 -> 19.0.0
|         +--- com.jayway.jsonpath:json-path:2.6.0 -> 2.4.0
|         |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
|         |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|         +--- org.slf4j:slf4j-api:1.7.30
|         \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
|              +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.11.4 (*)
|              +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.11.4
|              \--- org.apache.commons:commons-collections4:4.2 -> 4.4
+--- project :configuration
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :common (*)
|    +--- org.springframework.boot:spring-boot -> 2.4.5
|    |    +--- org.springframework:spring-core:5.3.6
|    |    |    \--- org.springframework:spring-jcl:5.3.6
|    |    \--- org.springframework:spring-context:5.3.6
|    |         +--- org.springframework:spring-aop:5.3.6
|    |         |    +--- org.springframework:spring-beans:5.3.6
|    |         |    |    \--- org.springframework:spring-core:5.3.6 (*)
|    |         |    \--- org.springframework:spring-core:5.3.6 (*)
|    |         +--- org.springframework:spring-beans:5.3.6 (*)
|    |         +--- org.springframework:spring-core:5.3.6 (*)
|    |         \--- org.springframework:spring-expression:5.3.6
|    |              \--- org.springframework:spring-core:5.3.6 (*)
|    \--- org.springframework:spring-core -> 5.3.6 (*)
+--- project :detectable
|    +--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0 (*)
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :common (*)
|    +--- org.tomlj:tomlj:1.0.0
|    |    +--- org.antlr:antlr4-runtime:4.7.2
|    |    \--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.codehaus.groovy:groovy-astbuilder:3.0.7
|    |    \--- org.codehaus.groovy:groovy:3.0.7 -> 3.0.0
|    +--- com.moandjiezana.toml:toml4j:0.7.1
|    |    \--- com.google.code.gson:gson:2.3.1 -> 2.8.6
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.7.2
|    +--- org.freemarker:freemarker:2.3.26-incubating
|    +--- com.synopsys.integration:integration-bdio:21.2.0 -> 22.1.4
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    \--- com.synopsys.integration:integration-rest:6.0.1 -> 10.1.0
|         +--- com.synopsys.integration:integration-common:25.2.0 (*)
|         +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         \--- org.apache.httpcomponents:httpmime:4.5.13 (*)
+--- project :detector
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :detectable (*)
|    \--- project :common (*)
+--- ch.qos.logback:logback-classic:1.2.3
|    +--- ch.qos.logback:logback-core:1.2.3
|    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
+--- com.esotericsoftware.yamlbeans:yamlbeans:1.11
+--- com.synopsys.integration:blackduck-common:57.0.0
|    +--- com.synopsys.integration:blackduck-common-api:2021.6.0.1
|    |    \--- com.synopsys.integration:integration-rest:10.1.0 (*)
|    +--- com.synopsys.integration:integration-bdio:22.1.4 (*)
|    +--- com.synopsys.integration:phone-home-client:5.0.0
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    +--- com.blackducksoftware.bdio:bdio2:3.0.0-beta.47
|    |    +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |    |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    |    \--- com.google.guava:guava:23.3-jre -> 29.0-jre
|    |    |         +--- com.google.guava:failureaccess:1.0.1
|    |    |         +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    |    |         +--- com.google.code.findbugs:jsr305:3.0.2
|    |    |         +--- org.checkerframework:checker-qual:2.11.1
|    |    |         +--- com.google.errorprone:error_prone_annotations:2.3.4
|    |    |         \--- com.google.j2objc:j2objc-annotations:1.3
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.9.0 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.9.8 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.8 -> 2.11.4 (*)
|    |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    +--- com.google.guava:guava:23.3-jre -> 29.0-jre (*)
|    |    +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |    |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.11.4
|    |    |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.11.4 (*)
|    |    |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.13
|    |    |    |    +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    |    +--- commons-codec:commons-codec:1.11 -> 1.15
|    |    |    |    +--- org.apache.httpcomponents:httpmime:4.5.13 (*)
|    |    |    |    +--- org.apache.httpcomponents:httpclient-cache:4.5.13
|    |    |    |    |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    |    \--- org.apache.httpcomponents:fluent-hc:4.5.13
|    |    |    |         \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |    |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.14
|    |    |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.14
|    |    |    |         \--- org.apache.httpcomponents:httpcore:4.4.14
|    |    |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|    |    |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.30
|    |    |    |    \--- org.slf4j:slf4j-api:1.7.30
|    |    |    \--- commons-io:commons-io:2.6 -> 2.8.0
|    |    \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.3
|    \--- org.apache.commons:commons-collections4:4.4
+--- com.synopsys:method-analyzer-core:0.1.0
|    +--- com.google.code.gson:gson:2.8.6
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- com.google.guava:guava:29.0-jre (*)
|    \--- org.ow2.asm:asm:8.0.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework.boot:spring-boot -> 2.4.5 (*)
+--- org.yaml:snakeyaml:1.27
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.30
+--- org.freemarker:freemarker:2.3.26-incubating
\--- org.apache.pdfbox:pdfbox:2.0.21
     +--- org.apache.pdfbox:fontbox:2.0.21
     |    \--- commons-logging:commons-logging:1.2
     \--- commons-logging:commons-logging:1.2

runtime - Runtime dependencies for source set 'main' (deprecated, use 'runtimeOnly' instead). (n)
No dependencies

runtimeClasspath - Runtime classpath of source set 'main'.
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10
+--- org.jetbrains:annotations:19.0.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.11.4
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.11.4
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 -> 2.11.4 (c)
+--- project :common
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    \--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0
|         +--- org.apache.httpcomponents:httpclient:4.5.13
|         |    +--- org.apache.httpcomponents:httpcore:4.4.13 -> 4.4.14
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-codec:commons-codec:1.11 -> 1.15
|         +--- org.apache.httpcomponents:httpmime:4.5.13
|         |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         +--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- org.apache.commons:commons-text:1.9
|         |    \--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- commons-io:commons-io:2.8.0
|         +--- org.apache.commons:commons-compress:1.20
|         +--- commons-codec:commons-codec:1.15
|         +--- commons-beanutils:commons-beanutils:1.9.4
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-collections:commons-collections:3.2.2
|         +--- com.google.code.gson:gson:2.8.6
|         +--- org.jetbrains:annotations:20.1.0 -> 19.0.0
|         +--- com.jayway.jsonpath:json-path:2.6.0 -> 2.4.0
|         |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
|         |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|         +--- org.slf4j:slf4j-api:1.7.30
|         \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
|              +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.11.4 (*)
|              +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.11.4
|              \--- org.apache.commons:commons-collections4:4.2 -> 4.4
+--- project :configuration
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :common (*)
|    +--- org.springframework.boot:spring-boot -> 2.4.5
|    |    +--- org.springframework:spring-core:5.3.6
|    |    |    \--- org.springframework:spring-jcl:5.3.6
|    |    \--- org.springframework:spring-context:5.3.6
|    |         +--- org.springframework:spring-aop:5.3.6
|    |         |    +--- org.springframework:spring-beans:5.3.6
|    |         |    |    \--- org.springframework:spring-core:5.3.6 (*)
|    |         |    \--- org.springframework:spring-core:5.3.6 (*)
|    |         +--- org.springframework:spring-beans:5.3.6 (*)
|    |         +--- org.springframework:spring-core:5.3.6 (*)
|    |         \--- org.springframework:spring-expression:5.3.6
|    |              \--- org.springframework:spring-core:5.3.6 (*)
|    \--- org.springframework:spring-core -> 5.3.6 (*)
+--- project :detectable
|    +--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0 (*)
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :common (*)
|    +--- org.tomlj:tomlj:1.0.0
|    |    +--- org.antlr:antlr4-runtime:4.7.2
|    |    \--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.codehaus.groovy:groovy-astbuilder:3.0.7
|    |    \--- org.codehaus.groovy:groovy:3.0.7 -> 3.0.0
|    +--- com.moandjiezana.toml:toml4j:0.7.1
|    |    \--- com.google.code.gson:gson:2.3.1 -> 2.8.6
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.7.2
|    +--- org.freemarker:freemarker:2.3.26-incubating
|    +--- com.synopsys.integration:integration-bdio:21.2.0 -> 22.1.4
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    \--- com.synopsys.integration:integration-rest:6.0.1 -> 10.1.0
|         +--- com.synopsys.integration:integration-common:25.2.0 (*)
|         +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         \--- org.apache.httpcomponents:httpmime:4.5.13 (*)
+--- project :detector
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :detectable (*)
|    \--- project :common (*)
+--- ch.qos.logback:logback-classic:1.2.3
|    +--- ch.qos.logback:logback-core:1.2.3
|    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
+--- com.esotericsoftware.yamlbeans:yamlbeans:1.11
+--- com.synopsys.integration:blackduck-common:57.0.0
|    +--- com.synopsys.integration:blackduck-common-api:2021.6.0.1
|    |    \--- com.synopsys.integration:integration-rest:10.1.0 (*)
|    +--- com.synopsys.integration:integration-bdio:22.1.4 (*)
|    +--- com.synopsys.integration:phone-home-client:5.0.0
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    +--- com.blackducksoftware.bdio:bdio2:3.0.0-beta.47
|    |    +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |    |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    |    \--- com.google.guava:guava:23.3-jre -> 29.0-jre
|    |    |         +--- com.google.guava:failureaccess:1.0.1
|    |    |         +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    |    |         +--- com.google.code.findbugs:jsr305:3.0.2
|    |    |         +--- org.checkerframework:checker-qual:2.11.1
|    |    |         +--- com.google.errorprone:error_prone_annotations:2.3.4
|    |    |         \--- com.google.j2objc:j2objc-annotations:1.3
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.9.0 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.9.8 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.8 -> 2.11.4 (*)
|    |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    +--- com.google.guava:guava:23.3-jre -> 29.0-jre (*)
|    |    +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |    |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.11.4
|    |    |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.11.4 (*)
|    |    |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.13
|    |    |    |    +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    |    +--- commons-codec:commons-codec:1.11 -> 1.15
|    |    |    |    +--- org.apache.httpcomponents:httpmime:4.5.13 (*)
|    |    |    |    +--- org.apache.httpcomponents:httpclient-cache:4.5.13
|    |    |    |    |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    |    \--- org.apache.httpcomponents:fluent-hc:4.5.13
|    |    |    |         \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |    |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.14
|    |    |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.14
|    |    |    |         \--- org.apache.httpcomponents:httpcore:4.4.14
|    |    |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|    |    |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.30
|    |    |    |    \--- org.slf4j:slf4j-api:1.7.30
|    |    |    \--- commons-io:commons-io:2.6 -> 2.8.0
|    |    \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.3
|    \--- org.apache.commons:commons-collections4:4.4
+--- com.synopsys:method-analyzer-core:0.1.0
|    +--- com.google.code.gson:gson:2.8.6
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- com.google.guava:guava:29.0-jre (*)
|    \--- org.ow2.asm:asm:8.0.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework.boot:spring-boot -> 2.4.5 (*)
+--- org.yaml:snakeyaml:1.27
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.30
+--- org.freemarker:freemarker:2.3.26-incubating
\--- org.apache.pdfbox:pdfbox:2.0.21
     +--- org.apache.pdfbox:fontbox:2.0.21
     |    \--- commons-logging:commons-logging:1.2
     \--- commons-logging:commons-logging:1.2

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
+--- org.apache.commons:commons-lang3:3.10
+--- org.jetbrains:annotations:19.0.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.11.4
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.11.4
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 -> 2.11.4 (c)
+--- project :common
+--- project :configuration
+--- project :detectable
|    \--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0
|         +--- org.apache.httpcomponents:httpclient:4.5.13
|         |    +--- org.apache.httpcomponents:httpcore:4.4.13 -> 4.4.14
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-codec:commons-codec:1.11 -> 1.15
|         +--- org.apache.httpcomponents:httpmime:4.5.13
|         |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         +--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- org.apache.commons:commons-text:1.9
|         |    \--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- commons-io:commons-io:2.8.0
|         +--- org.apache.commons:commons-compress:1.20
|         +--- commons-codec:commons-codec:1.15
|         +--- commons-beanutils:commons-beanutils:1.9.4
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-collections:commons-collections:3.2.2
|         +--- com.google.code.gson:gson:2.8.6
|         +--- org.jetbrains:annotations:20.1.0 -> 19.0.0
|         +--- com.jayway.jsonpath:json-path:2.6.0 -> 2.4.0
|         |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
|         |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|         +--- org.slf4j:slf4j-api:1.7.30
|         \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
|              +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.11.4 (*)
|              +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.11.4
|              \--- org.apache.commons:commons-collections4:4.2
+--- project :detector
+--- ch.qos.logback:logback-classic:1.2.3
|    +--- ch.qos.logback:logback-core:1.2.3
|    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
+--- com.esotericsoftware.yamlbeans:yamlbeans:1.11
+--- com.synopsys.integration:blackduck-common:57.0.0
|    +--- com.synopsys.integration:blackduck-common-api:2021.6.0.1
|    |    \--- com.synopsys.integration:integration-rest:10.1.0
|    |         +--- com.synopsys.integration:integration-common:25.2.0 (*)
|    |         +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |         \--- org.apache.httpcomponents:httpmime:4.5.13 (*)
|    +--- com.synopsys.integration:integration-bdio:22.1.4
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    +--- com.synopsys.integration:phone-home-client:5.0.0
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    \--- com.blackducksoftware.bdio:bdio2:3.0.0-beta.47
|         +--- com.blackducksoftware.magpie:magpie:0.6.0
|         |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|         |    \--- com.google.guava:guava:23.3-jre -> 29.0-jre
|         |         +--- com.google.guava:failureaccess:1.0.1
|         |         +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|         |         +--- com.google.code.findbugs:jsr305:3.0.2
|         |         +--- org.checkerframework:checker-qual:2.11.1
|         |         +--- com.google.errorprone:error_prone_annotations:2.3.4
|         |         \--- com.google.j2objc:j2objc-annotations:1.3
|         +--- com.fasterxml.jackson.core:jackson-annotations:2.9.0 -> 2.11.4
|         +--- com.fasterxml.jackson.core:jackson-core:2.9.8 -> 2.11.4
|         +--- com.fasterxml.jackson.core:jackson-databind:2.9.8 -> 2.11.4 (*)
|         +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|         +--- com.google.guava:guava:23.3-jre -> 29.0-jre (*)
|         +--- com.github.jsonld-java:jsonld-java:0.12.3
|         |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.11.4
|         |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.11.4 (*)
|         |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.13
|         |    |    +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         |    |    +--- commons-codec:commons-codec:1.11 -> 1.15
|         |    |    +--- org.apache.httpcomponents:httpmime:4.5.13 (*)
|         |    |    +--- org.apache.httpcomponents:httpclient-cache:4.5.13
|         |    |    |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         |    |    \--- org.apache.httpcomponents:fluent-hc:4.5.13
|         |    |         \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|         |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.14
|         |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.14
|         |    |         \--- org.apache.httpcomponents:httpcore:4.4.14
|         |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|         |    \--- commons-io:commons-io:2.6 -> 2.8.0
|         \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.3
+--- com.synopsys:method-analyzer-core:0.1.0
|    +--- com.google.guava:guava:29.0-jre (*)
|    \--- org.ow2.asm:asm:8.0.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework.boot:spring-boot -> 2.4.5
|    +--- org.springframework:spring-core:5.3.6
|    |    \--- org.springframework:spring-jcl:5.3.6
|    \--- org.springframework:spring-context:5.3.6
|         +--- org.springframework:spring-aop:5.3.6
|         |    +--- org.springframework:spring-beans:5.3.6
|         |    |    \--- org.springframework:spring-core:5.3.6 (*)
|         |    \--- org.springframework:spring-core:5.3.6 (*)
|         +--- org.springframework:spring-beans:5.3.6 (*)
|         +--- org.springframework:spring-core:5.3.6 (*)
|         \--- org.springframework:spring-expression:5.3.6
|              \--- org.springframework:spring-core:5.3.6 (*)
+--- org.yaml:snakeyaml:1.27
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.30
+--- org.freemarker:freemarker:2.3.26-incubating
+--- org.apache.pdfbox:pdfbox:2.0.21
|    +--- org.apache.pdfbox:fontbox:2.0.21
|    |    \--- commons-logging:commons-logging:1.2
|    \--- commons-logging:commons-logging:1.2
+--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    +--- org.junit:junit-bom:5.7.1
|    |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1 (c)
|    |    +--- org.junit.jupiter:junit-jupiter-params:5.7.1 -> 5.4.2 (c)
|    |    \--- org.junit.platform:junit-platform-commons:1.7.1 (c)
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    +--- org.opentest4j:opentest4j:1.2.0
|    \--- org.junit.platform:junit-platform-commons:1.7.1
|         \--- org.apiguardian:apiguardian-api:1.1.0
+--- org.junit-pioneer:junit-pioneer:0.3.3
+--- org.junit.jupiter:junit-jupiter-params:5.4.2
|    +--- org.apiguardian:apiguardian-api:1.0.0 -> 1.1.0
|    \--- org.junit.jupiter:junit-jupiter-api:5.4.2 -> 5.7.1 (*)
+--- org.mockito:mockito-core:2.+ -> 3.6.28
|    +--- net.bytebuddy:byte-buddy:1.10.18 -> 1.10.22
|    +--- net.bytebuddy:byte-buddy-agent:1.10.18 -> 1.10.22
|    \--- org.objenesis:objenesis:3.1
+--- org.assertj:assertj-core:3.13.2
+--- org.skyscreamer:jsonassert:1.5.0
|    \--- com.vaadin.external.google:android-json:0.0.20131108.vaadin1
+--- org.mockito:mockito-inline:2.+ -> 2.28.2
|    \--- org.mockito:mockito-core:2.28.2 -> 3.6.28 (*)
+--- project :common-test
+--- com.github.docker-java:docker-java-core:3.2.7
|    +--- com.github.docker-java:docker-java-api:3.2.7
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.10.3 -> 2.11.4
|    |    \--- org.slf4j:slf4j-api:1.7.30
|    +--- com.github.docker-java:docker-java-transport:3.2.7
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- commons-io:commons-io:2.6 -> 2.8.0
|    +--- org.apache.commons:commons-compress:1.20
|    +--- commons-lang:commons-lang:2.6
|    +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.11.4 (*)
|    +--- com.google.guava:guava:19.0 -> 29.0-jre (*)
|    \--- org.bouncycastle:bcpkix-jdk15on:1.64
|         \--- org.bouncycastle:bcprov-jdk15on:1.64
\--- com.github.docker-java:docker-java-transport-httpclient5:3.2.7
     +--- com.github.docker-java:docker-java-transport:3.2.7
     +--- org.apache.httpcomponents.client5:httpclient5:5.0
     |    +--- org.apache.httpcomponents.core5:httpcore5:5.0
     |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
     |    \--- commons-codec:commons-codec:1.13 -> 1.15
     \--- net.java.dev.jna:jna:5.5.0

testCompileOnly - Compile only dependencies for source set 'test'. (n)
No dependencies

testImplementation - Implementation only dependencies for source set 'test'. (n)
+--- org.junit.jupiter:junit-jupiter-api:5.7.1 (n)
+--- org.junit-pioneer:junit-pioneer:0.3.3 (n)
+--- org.junit.jupiter:junit-jupiter-params:5.4.2 (n)
+--- org.mockito:mockito-core:2.+ (n)
+--- org.assertj:assertj-core:3.13.2 (n)
+--- org.skyscreamer:jsonassert:1.5.0 (n)
+--- org.mockito:mockito-inline:2.+ (n)
+--- unspecified (n)
+--- project common-test (n)
+--- com.github.docker-java:docker-java-core:3.2.7 (n)
\--- com.github.docker-java:docker-java-transport-httpclient5:3.2.7 (n)

testRuntime - Runtime dependencies for source set 'test' (deprecated, use 'testRuntimeOnly' instead). (n)
No dependencies

testRuntimeClasspath - Runtime classpath of source set 'test'.
+--- org.slf4j:slf4j-api:1.7.30
+--- org.apache.commons:commons-lang3:3.10
+--- org.jetbrains:annotations:19.0.0
+--- net.minidev:json-smart:2.4.2
|    \--- net.minidev:accessors-smart:2.4.2
|         \--- org.ow2.asm:asm:8.0.1
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3
|    +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.11.4
|    |    \--- com.fasterxml.jackson.core:jackson-core:2.11.4
|    +--- org.yaml:snakeyaml:1.27
|    +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4
|    \--- com.fasterxml.jackson:jackson-bom:2.12.3
|         +--- com.fasterxml.jackson.core:jackson-core:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.core:jackson-databind:2.12.3 -> 2.11.4 (c)
|         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (c)
|         \--- com.fasterxml.jackson.core:jackson-annotations:2.12.3 -> 2.11.4 (c)
+--- project :common
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    \--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0
|         +--- org.apache.httpcomponents:httpclient:4.5.13
|         |    +--- org.apache.httpcomponents:httpcore:4.4.13 -> 4.4.14
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-codec:commons-codec:1.11 -> 1.15
|         +--- org.apache.httpcomponents:httpmime:4.5.13
|         |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         +--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- org.apache.commons:commons-text:1.9
|         |    \--- org.apache.commons:commons-lang3:3.11 -> 3.10
|         +--- commons-io:commons-io:2.8.0
|         +--- org.apache.commons:commons-compress:1.20
|         +--- commons-codec:commons-codec:1.15
|         +--- commons-beanutils:commons-beanutils:1.9.4
|         |    +--- commons-logging:commons-logging:1.2
|         |    \--- commons-collections:commons-collections:3.2.2
|         +--- com.google.code.gson:gson:2.8.6
|         +--- org.jetbrains:annotations:20.1.0 -> 19.0.0
|         +--- com.jayway.jsonpath:json-path:2.6.0 -> 2.4.0
|         |    +--- net.minidev:json-smart:2.3 -> 2.4.2 (*)
|         |    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|         +--- org.slf4j:slf4j-api:1.7.30
|         \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.11
|              +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.11.4 (*)
|              +--- com.fasterxml.jackson.core:jackson-core:2.10.3 -> 2.11.4
|              \--- org.apache.commons:commons-collections4:4.2 -> 4.4
+--- project :configuration
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :common (*)
|    +--- org.springframework.boot:spring-boot -> 2.4.5
|    |    +--- org.springframework:spring-core:5.3.6
|    |    |    \--- org.springframework:spring-jcl:5.3.6
|    |    \--- org.springframework:spring-context:5.3.6
|    |         +--- org.springframework:spring-aop:5.3.6
|    |         |    +--- org.springframework:spring-beans:5.3.6
|    |         |    |    \--- org.springframework:spring-core:5.3.6 (*)
|    |         |    \--- org.springframework:spring-core:5.3.6 (*)
|    |         +--- org.springframework:spring-beans:5.3.6 (*)
|    |         +--- org.springframework:spring-core:5.3.6 (*)
|    |         \--- org.springframework:spring-expression:5.3.6
|    |              \--- org.springframework:spring-core:5.3.6 (*)
|    \--- org.springframework:spring-core -> 5.3.6 (*)
+--- project :detectable
|    +--- com.synopsys.integration:integration-common:24.0.0 -> 25.2.0 (*)
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :common (*)
|    +--- org.tomlj:tomlj:1.0.0
|    |    +--- org.antlr:antlr4-runtime:4.7.2
|    |    \--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.codehaus.groovy:groovy-astbuilder:3.0.7
|    |    \--- org.codehaus.groovy:groovy:3.0.7 -> 3.0.0
|    +--- com.moandjiezana.toml:toml4j:0.7.1
|    |    \--- com.google.code.gson:gson:2.3.1 -> 2.8.6
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.7.2
|    +--- org.freemarker:freemarker:2.3.26-incubating
|    +--- com.synopsys.integration:integration-bdio:21.2.0 -> 22.1.4
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    \--- com.synopsys.integration:integration-rest:6.0.1 -> 10.1.0
|         +--- com.synopsys.integration:integration-common:25.2.0 (*)
|         +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|         \--- org.apache.httpcomponents:httpmime:4.5.13 (*)
+--- project :detector
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    +--- project :detectable (*)
|    \--- project :common (*)
+--- ch.qos.logback:logback-classic:1.2.3
|    +--- ch.qos.logback:logback-core:1.2.3
|    \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
+--- com.esotericsoftware.yamlbeans:yamlbeans:1.11
+--- com.synopsys.integration:blackduck-common:57.0.0
|    +--- com.synopsys.integration:blackduck-common-api:2021.6.0.1
|    |    \--- com.synopsys.integration:integration-rest:10.1.0 (*)
|    +--- com.synopsys.integration:integration-bdio:22.1.4 (*)
|    +--- com.synopsys.integration:phone-home-client:5.0.0
|    |    \--- com.synopsys.integration:integration-common:25.2.0 (*)
|    +--- com.blackducksoftware.bdio:bdio2:3.0.0-beta.47
|    |    +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |    |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    |    \--- com.google.guava:guava:23.3-jre -> 29.0-jre
|    |    |         +--- com.google.guava:failureaccess:1.0.1
|    |    |         +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    |    |         +--- com.google.code.findbugs:jsr305:3.0.2
|    |    |         +--- org.checkerframework:checker-qual:2.11.1
|    |    |         +--- com.google.errorprone:error_prone_annotations:2.3.4
|    |    |         \--- com.google.j2objc:j2objc-annotations:1.3
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.9.0 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.9.8 -> 2.11.4
|    |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.8 -> 2.11.4 (*)
|    |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |    +--- com.google.guava:guava:23.3-jre -> 29.0-jre (*)
|    |    +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |    |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.11.4
|    |    |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.11.4 (*)
|    |    |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.13
|    |    |    |    +--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    |    +--- commons-codec:commons-codec:1.11 -> 1.15
|    |    |    |    +--- org.apache.httpcomponents:httpmime:4.5.13 (*)
|    |    |    |    +--- org.apache.httpcomponents:httpclient-cache:4.5.13
|    |    |    |    |    \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    |    \--- org.apache.httpcomponents:fluent-hc:4.5.13
|    |    |    |         \--- org.apache.httpcomponents:httpclient:4.5.13 (*)
|    |    |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |    |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.14
|    |    |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.14
|    |    |    |         \--- org.apache.httpcomponents:httpcore:4.4.14
|    |    |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|    |    |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.30
|    |    |    |    \--- org.slf4j:slf4j-api:1.7.30
|    |    |    \--- commons-io:commons-io:2.6 -> 2.8.0
|    |    \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.3
|    \--- org.apache.commons:commons-collections4:4.4
+--- com.synopsys:method-analyzer-core:0.1.0
|    +--- com.google.code.gson:gson:2.8.6
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- com.google.guava:guava:29.0-jre (*)
|    \--- org.ow2.asm:asm:8.0.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework.boot:spring-boot -> 2.4.5 (*)
+--- org.yaml:snakeyaml:1.27
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.30
+--- org.freemarker:freemarker:2.3.26-incubating
+--- org.apache.pdfbox:pdfbox:2.0.21
|    +--- org.apache.pdfbox:fontbox:2.0.21
|    |    \--- commons-logging:commons-logging:1.2
|    \--- commons-logging:commons-logging:1.2
+--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    +--- org.junit:junit-bom:5.7.1
|    |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1 (c)
|    |    +--- org.junit.jupiter:junit-jupiter-engine:5.7.1 (c)
|    |    +--- org.junit.jupiter:junit-jupiter-params:5.7.1 -> 5.4.2 (c)
|    |    +--- org.junit.platform:junit-platform-commons:1.7.1 (c)
|    |    \--- org.junit.platform:junit-platform-engine:1.7.1 (c)
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    +--- org.opentest4j:opentest4j:1.2.0
|    \--- org.junit.platform:junit-platform-commons:1.7.1
|         \--- org.apiguardian:apiguardian-api:1.1.0
+--- org.junit-pioneer:junit-pioneer:0.3.3
|    \--- org.junit.jupiter:junit-jupiter-api:5.1.1 -> 5.7.1 (*)
+--- org.junit.jupiter:junit-jupiter-params:5.4.2
|    +--- org.apiguardian:apiguardian-api:1.0.0 -> 1.1.0
|    \--- org.junit.jupiter:junit-jupiter-api:5.4.2 -> 5.7.1 (*)
+--- org.mockito:mockito-core:2.+ -> 3.6.28
|    +--- net.bytebuddy:byte-buddy:1.10.18 -> 1.10.22
|    +--- net.bytebuddy:byte-buddy-agent:1.10.18 -> 1.10.22
|    \--- org.objenesis:objenesis:3.1
+--- org.assertj:assertj-core:3.13.2
+--- org.skyscreamer:jsonassert:1.5.0
|    \--- com.vaadin.external.google:android-json:0.0.20131108.vaadin1
+--- org.mockito:mockito-inline:2.+ -> 2.28.2
|    \--- org.mockito:mockito-core:2.28.2 -> 3.6.28 (*)
+--- project :common-test
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- org.apache.commons:commons-lang3:3.10
|    +--- org.jetbrains:annotations:19.0.0
|    +--- net.minidev:json-smart:2.4.2 (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.3 (*)
|    \--- org.junit.jupiter:junit-jupiter-api:5.3.1 -> 5.7.1 (*)
+--- com.github.docker-java:docker-java-core:3.2.7
|    +--- com.github.docker-java:docker-java-api:3.2.7
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.10.3 -> 2.11.4
|    |    \--- org.slf4j:slf4j-api:1.7.30
|    +--- com.github.docker-java:docker-java-transport:3.2.7
|    +--- org.slf4j:slf4j-api:1.7.30
|    +--- commons-io:commons-io:2.6 -> 2.8.0
|    +--- org.apache.commons:commons-compress:1.20
|    +--- commons-lang:commons-lang:2.6
|    +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.11.4 (*)
|    +--- com.google.guava:guava:19.0 -> 29.0-jre (*)
|    \--- org.bouncycastle:bcpkix-jdk15on:1.64
|         \--- org.bouncycastle:bcprov-jdk15on:1.64
+--- com.github.docker-java:docker-java-transport-httpclient5:3.2.7
|    +--- com.github.docker-java:docker-java-transport:3.2.7
|    +--- org.apache.httpcomponents.client5:httpclient5:5.0
|    |    +--- org.apache.httpcomponents.core5:httpcore5:5.0
|    |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.30
|    |    \--- commons-codec:commons-codec:1.13 -> 1.15
|    \--- net.java.dev.jna:jna:5.5.0
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
projectName:detect
projectVersion:11.1.0-SIGQA8-SNAPSHOT
projectParent:null
DETECT META DATA END
