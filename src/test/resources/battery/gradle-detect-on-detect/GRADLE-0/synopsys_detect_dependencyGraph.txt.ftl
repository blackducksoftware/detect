
------------------------------------------------------------
Root project 'detect'
------------------------------------------------------------

annotationProcessor - Annotation processors and their dependencies for source set 'main'.
No dependencies

apiElements - API elements for main. (n)
No dependencies

archives - Configuration for archive artifacts. (n)
No dependencies

bootArchives - Configuration for Spring Boot archive artifacts. (n)
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
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.13.5
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.13.5 -> 2.15.0
|    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0 (*)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :common
|    \--- com.blackduck.integration:blackduck-common:67.0.20
|         +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|         |    \--- com.blackduck.integration:integration-rest:11.1.2
|         |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|         |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|         |         |    +--- org.apache.commons:commons-lang3:3.12.0
|         |         |    +--- org.apache.commons:commons-text:1.10.0
|         |         |    |    \--- org.apache.commons:commons-lang3:3.12.0
|         |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|         |         |    +--- org.apache.commons:commons-compress:1.26.1
|         |         |    |    +--- commons-codec:commons-codec:1.16.1 -> 1.15
|         |         |    |    +--- commons-io:commons-io:2.15.1
|         |         |    |    \--- org.apache.commons:commons-lang3:3.14.0 -> 3.12.0
|         |         |    +--- commons-codec:commons-codec:1.15
|         |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|         |         |    |    +--- commons-logging:commons-logging:1.3.5
|         |         |    |    \--- commons-collections:commons-collections:3.2.2
|         |         |    +--- org.apache.commons:commons-collections4:4.4
|         |         |    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|         |         |    +--- org.jetbrains:annotations:24.0.1
|         |         |    +--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0
|         |         |    |    +--- net.minidev:json-smart:2.4.7 -> 2.4.11
|         |         |    |    |    \--- net.minidev:accessors-smart:2.4.11
|         |         |    |    |         \--- org.ow2.asm:asm:9.3 -> 9.6
|         |         |    |    \--- org.slf4j:slf4j-api:1.7.33 -> 1.7.36
|         |         |    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|         |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|         |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.13.5 (*)
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
|              +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (*)
|              +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|              +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (*)
|              +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|              +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|              +--- com.github.jsonld-java:jsonld-java:0.12.3
|              |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|              |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.13.5 (*)
|              |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|              |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|              |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|              |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.16
|              |    |         \--- org.apache.httpcomponents:httpcore:4.4.16
|              |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|              |    \--- commons-io:commons-io:2.6 -> 2.15.1
|              \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.4
+--- project :configuration
+--- project :detectable
+--- project :detector
+--- ch.qos.logback:logback-classic:1.2.13
|    +--- ch.qos.logback:logback-core:1.2.13
|    \--- org.slf4j:slf4j-api:1.7.32 -> 1.7.36
+--- com.blackducksoftware.bdio:bdio-protobuf:3.2.12
|    +--- com.google.protobuf:protobuf-java:3.25.5
|    +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    \--- commons-lang:commons-lang:2.6
+--- com.blackduck.integration:blackduck-common:67.0.20 (*)
+--- com.blackduck.integration:blackduck-upload-common:4.1.2
+--- com.blackducksoftware:method-analyzer-core:1.0.1
|    +--- com.google.guava:guava:31.1-jre -> 32.1.2-jre (*)
|    \--- org.ow2.asm:asm:9.6
+--- com.blackduck.integration:component-locator:2.1.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework:spring-jcl:5.3.34
+--- org.springframework:spring-core:5.3.34
|    \--- org.springframework:spring-jcl:5.3.34
+--- org.springframework:spring-aop:5.3.34
|    +--- org.springframework:spring-beans:5.3.34
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    \--- org.springframework:spring-core:5.3.34 (*)
+--- org.springframework:spring-beans:5.3.34 (*)
+--- org.springframework:spring-expression:5.3.34
|    \--- org.springframework:spring-core:5.3.34 (*)
+--- org.springframework:spring-context:5.3.34
|    +--- org.springframework:spring-aop:5.3.34 (*)
|    +--- org.springframework:spring-beans:5.3.34 (*)
|    +--- org.springframework:spring-core:5.3.34 (*)
|    \--- org.springframework:spring-expression:5.3.34 (*)
+--- org.springframework.boot:spring-boot -> 2.7.12
|    +--- org.springframework:spring-core:5.3.27 -> 5.3.34 (*)
|    \--- org.springframework:spring-context:5.3.27 -> 5.3.34 (*)
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.36
+--- org.apache.pdfbox:pdfbox:2.0.27
|    +--- org.apache.pdfbox:fontbox:2.0.27 -> 2.0.29
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- org.apache.tika:tika-core:2.9.0
|    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    \--- commons-io:commons-io:2.13.0 -> 2.15.1
+--- org.apache.tika:tika-parsers-standard-package:2.9.0
|    +--- org.apache.tika:tika-parser-apple-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0
|    |    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    |    \--- com.googlecode.plist:dd-plist:1.27
|    +--- org.apache.tika:tika-parser-audiovideo-module:2.9.0
|    |    \--- com.drewnoakes:metadata-extractor:2.18.0
|    |         \--- com.adobe.xmp:xmpcore:6.1.11
|    +--- org.apache.tika:tika-parser-cad-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0
|    |    |    +--- org.apache.tika:tika-parser-html-module:2.9.0
|    |    |    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    |    |    \--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.tika:tika-parser-text-module:2.9.0
|    |    |    |    +--- com.github.albfernandez:juniversalchardet:2.4.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- org.apache.commons:commons-csv:1.10.0
|    |    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- xerces:xercesImpl:2.12.2
|    |    |    |         \--- xml-apis:xml-apis:1.4.01
|    |    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0
|    |    |    |    +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10
|    |    |    |    |    \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    \--- org.apache.james:apache-mime4j-dom:0.8.9
|    |    |    |         +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10 (*)
|    |    |    |         \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    +--- com.pff:java-libpst:0.9.3
|    |    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    |    +--- org.apache.poi:poi:5.2.3
|    |    |    |    +--- commons-codec:commons-codec:1.15
|    |    |    |    +--- org.apache.commons:commons-collections4:4.4
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.zaxxer:SparseBitSet:1.2
|    |    |    |    \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    +--- org.apache.poi:poi-scratchpad:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    \--- commons-codec:commons-codec:1.15
|    |    |    +--- org.apache.poi:poi-ooxml:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.poi:poi-ooxml-lite:5.2.3
|    |    |    |    |    \--- org.apache.xmlbeans:xmlbeans:5.1.1
|    |    |    |    |         \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.xmlbeans:xmlbeans:5.1.1 (*)
|    |    |    |    +--- org.apache.commons:commons-compress:1.21 -> 1.26.1 (*)
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.github.virtuald:curvesapi:1.07
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    \--- org.apache.commons:commons-collections4:4.4
|    |    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess:4.0.5
|    |    |    |    +--- org.apache.commons:commons-lang3:3.10 -> 3.12.0
|    |    |    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess-encrypt:4.0.2
|    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.72 -> 1.78
|    |    |    +--- org.bouncycastle:bcmail-jdk18on:1.76
|    |    |    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |    +--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78
|    |    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.78
|    |    |    |    \--- org.bouncycastle:bcpkix-jdk18on:1.76
|    |    |    |         +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |         \--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78 (*)
|    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.15.2 -> 2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-databind:2.15.2 -> 2.13.5 (*)
|    +--- org.apache.tika:tika-parser-code-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.codelibs:jhighlight:1.1.0
|    |    |    \--- commons-io:commons-io:2.7 -> 2.15.1
|    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    +--- org.ow2.asm:asm:9.5 -> 9.6
|    |    +--- com.epam:parso:2.0.14
|    |    |    \--- org.slf4j:slf4j-api:1.7.5 -> 1.7.36
|    |    \--- org.tallison:jmatio:1.5
|    |         \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    +--- org.apache.tika:tika-parser-crypto-module:2.9.0
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-digest-commons:2.9.0
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-font-module:2.9.0
|    |    \--- org.apache.pdfbox:fontbox:2.0.29 (*)
|    +--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-image-module:2.9.0
|    |    +--- com.drewnoakes:metadata-extractor:2.18.0 (*)
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0
|    |    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    |    \--- org.apache.pdfbox:xmpbox:2.0.29
|    |    |         \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    +--- com.github.jai-imageio:jai-imageio-core:1.4.0
|    |    \--- org.apache.pdfbox:jbig2-imageio:3.0.4
|    +--- org.apache.tika:tika-parser-mail-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    \--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0 (*)
|    +--- org.slf4j:jcl-over-slf4j:2.0.7 -> 1.7.36
|    |    \--- org.slf4j:slf4j-api:1.7.36
|    +--- org.apache.tika:tika-parser-miscoffice-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    +--- org.apache.commons:commons-collections4:4.4
|    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8
|    |    |    +--- jakarta.xml.bind:jakarta.xml.bind-api:2.3.3
|    |    |    +--- org.glassfish.jaxb:txw2:2.3.8
|    |    |    \--- com.sun.istack:istack-commons-runtime:3.0.12
|    |    \--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-news-module:2.9.0
|    |    +--- com.rometools:rome:2.1.0
|    |    |    +--- com.rometools:rome-utils:2.1.0
|    |    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    |    +--- org.jdom:jdom2:2.0.6.1
|    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    \--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    +--- org.apache.tika:tika-parser-ocr-module:2.9.0
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    \--- org.apache.commons:commons-exec:1.3
|    +--- org.apache.tika:tika-parser-pdf-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    |    +--- org.apache.pdfbox:pdfbox:2.0.29 -> 2.0.27 (*)
|    |    +--- org.apache.pdfbox:pdfbox-tools:2.0.29
|    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    \--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8 (*)
|    +--- org.apache.tika:tika-parser-pkg-module:2.9.0
|    |    +--- org.tukaani:xz:1.9
|    |    +--- org.brotli:dec:0.1.2
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    \--- com.github.junrar:junrar:7.5.5
|    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-webarchive-module:2.9.0
|    |    +--- org.netpreserve:jwarc:0.28.1
|    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.gagravarr:vorbis-java-tika:0.8
|    \--- org.gagravarr:vorbis-java-core:0.8
+--- org.apache.logging.log4j:log4j-to-slf4j:2.23.1
|    +--- org.apache.logging.log4j:log4j-api:2.23.1 -> 2.17.2
|    \--- org.slf4j:slf4j-api:2.0.9 -> 1.7.36
+--- ch.qos.logback:logback-core:1.2.13 (c)
+--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0 (c)
+--- org.bouncycastle:bcutil-jdk18on:1.78 (c)
\--- org.apache.james:apache-mime4j-core:0.8.10 (c)

compileOnly - Compile only dependencies for source set 'main'. (n)
No dependencies

default - Configuration for default artifacts. (n)
No dependencies

developmentOnly - Configuration for development-only dependencies such as Spring Boot's DevTools.
No dependencies

implementation - Implementation only dependencies for source set 'main'. (n)
+--- com.google.guava:guava:32.1.2-jre (n)
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (n)
+--- org.yaml:snakeyaml:2.0 (n)
+--- com.fasterxml.jackson.core:jackson-core:2.15.0 (n)
+--- org.freemarker:freemarker:2.3.31 (n)
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (n)
+--- project common (n)
+--- project configuration (n)
+--- project detectable (n)
+--- project detector (n)
+--- ch.qos.logback:logback-classic:1.2.13 (n)
+--- com.blackducksoftware.bdio:bdio-protobuf:3.2.12 (n)
+--- com.blackduck.integration:blackduck-common:67.0.20 (n)
+--- com.blackduck.integration:blackduck-upload-common:4.1.2 (n)
+--- com.blackducksoftware:method-analyzer-core:1.0.1 (n)
+--- com.blackduck.integration:component-locator:2.1.1 (n)
+--- org.apache.maven.shared:maven-invoker:3.0.0 (n)
+--- org.springframework:spring-jcl:5.3.34 (n)
+--- org.springframework:spring-core:5.3.34 (n)
+--- org.springframework:spring-aop:5.3.34 (n)
+--- org.springframework:spring-beans:5.3.34 (n)
+--- org.springframework:spring-expression:5.3.34 (n)
+--- org.springframework:spring-context:5.3.34 (n)
+--- org.springframework.boot:spring-boot (n)
+--- org.zeroturnaround:zt-zip:1.13 (n)
+--- org.apache.pdfbox:pdfbox:2.0.27 (n)
+--- org.apache.tika:tika-core:2.9.0 (n)
+--- org.apache.tika:tika-parsers-standard-package:2.9.0 (n)
\--- org.apache.logging.log4j:log4j-to-slf4j:2.23.1 (n)

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

productionRuntimeClasspath
+--- com.google.guava:guava:32.1.2-jre
|    +--- com.google.guava:failureaccess:1.0.1
|    +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    +--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.checkerframework:checker-qual:3.33.0
|    +--- com.google.errorprone:error_prone_annotations:2.18.0
|    \--- com.google.j2objc:j2objc-annotations:2.8
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.13.5
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.13.5 -> 2.15.0
|    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0 (*)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :common
|    +--- com.blackduck.integration:blackduck-common:67.0.20
|    |    +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|    |    |    \--- com.blackduck.integration:integration-rest:11.1.2
|    |    |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|    |    |         |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1 -> 5.8.2
|    |    |         |    |    +--- org.junit:junit-bom:5.8.2
|    |    |         |    |    |    +--- org.junit.jupiter:junit-jupiter-api:5.8.2 (c)
|    |    |         |    |    |    \--- org.junit.platform:junit-platform-commons:1.8.2 (c)
|    |    |         |    |    +--- org.opentest4j:opentest4j:1.2.0
|    |    |         |    |    \--- org.junit.platform:junit-platform-commons:1.8.2
|    |    |         |    |         \--- org.junit:junit-bom:5.8.2 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    |         |    +--- org.apache.commons:commons-lang3:3.12.0
|    |    |         |    +--- org.apache.commons:commons-text:1.10.0
|    |    |         |    |    \--- org.apache.commons:commons-lang3:3.12.0
|    |    |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |         |    +--- org.apache.commons:commons-compress:1.26.1
|    |    |         |    |    +--- commons-codec:commons-codec:1.16.1 -> 1.15
|    |    |         |    |    +--- commons-io:commons-io:2.15.1
|    |    |         |    |    \--- org.apache.commons:commons-lang3:3.14.0 -> 3.12.0
|    |    |         |    +--- commons-codec:commons-codec:1.15
|    |    |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|    |    |         |    |    +--- commons-logging:commons-logging:1.3.5
|    |    |         |    |    \--- commons-collections:commons-collections:3.2.2
|    |    |         |    +--- org.apache.commons:commons-collections4:4.4
|    |    |         |    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|    |    |         |    +--- org.jetbrains:annotations:24.0.1
|    |    |         |    +--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0
|    |    |         |    |    +--- net.minidev:json-smart:2.4.7 -> 2.4.11
|    |    |         |    |    |    \--- net.minidev:accessors-smart:2.4.11
|    |    |         |    |    |         \--- org.ow2.asm:asm:9.3 -> 9.6
|    |    |         |    |    \--- org.slf4j:slf4j-api:1.7.33 -> 1.7.36
|    |    |         |    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    |    |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|    |    |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.13.5 (*)
|    |    |         |         +--- com.fasterxml.jackson.core:jackson-core:2.14.0 -> 2.15.0
|    |    |         |         \--- org.apache.commons:commons-collections4:4.4
|    |    |         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |         \--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    +--- com.blackduck.integration:phone-home-client:7.0.1
|    |    |    \--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3 (*)
|    |    +--- com.blackduck.integration:integration-bdio:27.0.4
|    |    |    \--- com.blackduck.integration:integration-common:27.0.3 (*)
|    |    \--- com.blackducksoftware.bdio:bdio2:3.2.12
|    |         +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |         |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |         |    \--- com.google.guava:guava:23.3-jre -> 32.1.2-jre (*)
|    |         +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (*)
|    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (*)
|    |         +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |         +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    |         +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |         |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|    |         |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.13.5 (*)
|    |         |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|    |         |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |         |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|    |         |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.16
|    |         |    |         \--- org.apache.httpcomponents:httpcore:4.4.16
|    |         |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    |         |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.36
|    |         |    |    \--- org.slf4j:slf4j-api:1.7.36
|    |         |    \--- commons-io:commons-io:2.6 -> 2.15.1
|    |         \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.4
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    \--- com.blackduck.integration:integration-common -> 27.0.3 (*)
+--- project :configuration
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common (*)
|    +--- org.springframework:spring-jcl:5.3.34
|    +--- org.springframework:spring-core:5.3.34
|    |    \--- org.springframework:spring-jcl:5.3.34
|    +--- org.springframework:spring-aop:5.3.34
|    |    +--- org.springframework:spring-beans:5.3.34
|    |    |    \--- org.springframework:spring-core:5.3.34 (*)
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    +--- org.springframework:spring-beans:5.3.34 (*)
|    +--- org.springframework:spring-expression:5.3.34
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    +--- org.springframework:spring-context:5.3.34
|    |    +--- org.springframework:spring-aop:5.3.34 (*)
|    |    +--- org.springframework:spring-beans:5.3.34 (*)
|    |    +--- org.springframework:spring-core:5.3.34 (*)
|    |    \--- org.springframework:spring-expression:5.3.34 (*)
|    \--- org.springframework.boot:spring-boot -> 2.7.12
|         +--- org.springframework:spring-core:5.3.27 -> 5.3.34 (*)
|         \--- org.springframework:spring-context:5.3.27 -> 5.3.34 (*)
+--- project :detectable
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common (*)
|    +--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
|    +--- com.blackduck.integration:integration-rest -> 11.1.2 (*)
|    +--- org.tomlj:tomlj:1.1.1
|    |    +--- org.antlr:antlr4-runtime:4.11.1
|    |    \--- org.checkerframework:checker-qual:3.21.2 -> 3.33.0
|    +--- com.moandjiezana.toml:toml4j:0.7.2
|    |    \--- com.google.code.gson:gson:2.8.1 -> 2.9.1
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.11.1
|    \--- guru.nidi:graphviz-java:0.18.1
|         +--- org.webjars.npm:viz.js-graphviz-java:2.1.3
|         +--- guru.nidi.com.kitfox:svgSalamander:1.1.3
|         +--- net.arnx:nashorn-promise:0.1.1
|         +--- org.apache.commons:commons-exec:1.3
|         +--- com.google.code.findbugs:jsr305:3.0.2
|         +--- org.slf4j:jcl-over-slf4j:1.7.30 -> 1.7.36 (*)
|         +--- org.slf4j:jul-to-slf4j:1.7.30 -> 1.7.36
|         |    \--- org.slf4j:slf4j-api:1.7.36
|         \--- org.slf4j:slf4j-api:1.7.30 -> 1.7.36
+--- project :detector
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :detectable (*)
|    +--- project :common (*)
|    \--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
+--- ch.qos.logback:logback-classic:1.2.13
|    +--- ch.qos.logback:logback-core:1.2.13
|    \--- org.slf4j:slf4j-api:1.7.32 -> 1.7.36
+--- com.blackducksoftware.bdio:bdio-protobuf:3.2.12
|    +--- com.google.protobuf:protobuf-java:3.25.5
|    +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    \--- commons-lang:commons-lang:2.6
+--- com.blackduck.integration:blackduck-common:67.0.20 (*)
+--- com.blackduck.integration:blackduck-upload-common:4.1.2
|    +--- commons-codec:commons-codec:1.15
|    \--- com.blackduck.integration:integration-rest:11.0.0 -> 11.1.2 (*)
+--- com.blackducksoftware:method-analyzer-core:1.0.1
|    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|    +--- org.slf4j:slf4j-api:1.7.36
|    +--- com.google.guava:guava:31.1-jre -> 32.1.2-jre (*)
|    \--- org.ow2.asm:asm:9.6
+--- com.blackduck.integration:component-locator:2.1.1
|    +--- com.google.code.gson:gson:2.7 -> 2.9.1
|    +--- ch.qos.logback:logback-classic:1.2.13 (*)
|    \--- commons-io:commons-io:2.6 -> 2.15.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework:spring-jcl:5.3.34
+--- org.springframework:spring-core:5.3.34 (*)
+--- org.springframework:spring-aop:5.3.34 (*)
+--- org.springframework:spring-beans:5.3.34 (*)
+--- org.springframework:spring-expression:5.3.34 (*)
+--- org.springframework:spring-context:5.3.34 (*)
+--- org.springframework.boot:spring-boot -> 2.7.12 (*)
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.36
+--- org.apache.pdfbox:pdfbox:2.0.27
|    +--- org.apache.pdfbox:fontbox:2.0.27 -> 2.0.29
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- org.apache.tika:tika-core:2.9.0
|    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    \--- commons-io:commons-io:2.13.0 -> 2.15.1
+--- org.apache.tika:tika-parsers-standard-package:2.9.0
|    +--- org.apache.tika:tika-parser-apple-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0
|    |    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    |    \--- com.googlecode.plist:dd-plist:1.27
|    +--- org.apache.tika:tika-parser-audiovideo-module:2.9.0
|    |    \--- com.drewnoakes:metadata-extractor:2.18.0
|    |         \--- com.adobe.xmp:xmpcore:6.1.11
|    +--- org.apache.tika:tika-parser-cad-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0
|    |    |    +--- org.apache.tika:tika-parser-html-module:2.9.0
|    |    |    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    |    |    \--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.tika:tika-parser-text-module:2.9.0
|    |    |    |    +--- com.github.albfernandez:juniversalchardet:2.4.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- org.apache.commons:commons-csv:1.10.0
|    |    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- xerces:xercesImpl:2.12.2
|    |    |    |         \--- xml-apis:xml-apis:1.4.01
|    |    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0
|    |    |    |    +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10
|    |    |    |    |    \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    \--- org.apache.james:apache-mime4j-dom:0.8.9
|    |    |    |         +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10 (*)
|    |    |    |         \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    +--- com.pff:java-libpst:0.9.3
|    |    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    |    +--- org.apache.poi:poi:5.2.3
|    |    |    |    +--- commons-codec:commons-codec:1.15
|    |    |    |    +--- org.apache.commons:commons-collections4:4.4
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.zaxxer:SparseBitSet:1.2
|    |    |    |    \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    +--- org.apache.poi:poi-scratchpad:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    \--- commons-codec:commons-codec:1.15
|    |    |    +--- org.apache.poi:poi-ooxml:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.poi:poi-ooxml-lite:5.2.3
|    |    |    |    |    \--- org.apache.xmlbeans:xmlbeans:5.1.1
|    |    |    |    |         \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.xmlbeans:xmlbeans:5.1.1 (*)
|    |    |    |    +--- org.apache.commons:commons-compress:1.21 -> 1.26.1 (*)
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.github.virtuald:curvesapi:1.07
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    \--- org.apache.commons:commons-collections4:4.4
|    |    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess:4.0.5
|    |    |    |    +--- org.apache.commons:commons-lang3:3.10 -> 3.12.0
|    |    |    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess-encrypt:4.0.2
|    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.72 -> 1.78
|    |    |    +--- org.bouncycastle:bcmail-jdk18on:1.76
|    |    |    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |    +--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78
|    |    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.78
|    |    |    |    \--- org.bouncycastle:bcpkix-jdk18on:1.76
|    |    |    |         +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |         \--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78 (*)
|    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.15.2 -> 2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-databind:2.15.2 -> 2.13.5 (*)
|    +--- org.apache.tika:tika-parser-code-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.codelibs:jhighlight:1.1.0
|    |    |    \--- commons-io:commons-io:2.7 -> 2.15.1
|    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    +--- org.ow2.asm:asm:9.5 -> 9.6
|    |    +--- com.epam:parso:2.0.14
|    |    |    \--- org.slf4j:slf4j-api:1.7.5 -> 1.7.36
|    |    \--- org.tallison:jmatio:1.5
|    |         \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    +--- org.apache.tika:tika-parser-crypto-module:2.9.0
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-digest-commons:2.9.0
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-font-module:2.9.0
|    |    \--- org.apache.pdfbox:fontbox:2.0.29 (*)
|    +--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-image-module:2.9.0
|    |    +--- com.drewnoakes:metadata-extractor:2.18.0 (*)
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0
|    |    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    |    \--- org.apache.pdfbox:xmpbox:2.0.29
|    |    |         \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    +--- com.github.jai-imageio:jai-imageio-core:1.4.0
|    |    \--- org.apache.pdfbox:jbig2-imageio:3.0.4
|    +--- org.apache.tika:tika-parser-mail-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    \--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0 (*)
|    +--- org.slf4j:jcl-over-slf4j:2.0.7 -> 1.7.36 (*)
|    +--- org.apache.tika:tika-parser-miscoffice-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    +--- org.apache.commons:commons-collections4:4.4
|    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8
|    |    |    +--- jakarta.xml.bind:jakarta.xml.bind-api:2.3.3
|    |    |    +--- org.glassfish.jaxb:txw2:2.3.8
|    |    |    +--- com.sun.istack:istack-commons-runtime:3.0.12
|    |    |    \--- com.sun.activation:jakarta.activation:1.2.2
|    |    \--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-news-module:2.9.0
|    |    +--- com.rometools:rome:2.1.0
|    |    |    +--- com.rometools:rome-utils:2.1.0
|    |    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    |    +--- org.jdom:jdom2:2.0.6.1
|    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    \--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    +--- org.apache.tika:tika-parser-ocr-module:2.9.0
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    \--- org.apache.commons:commons-exec:1.3
|    +--- org.apache.tika:tika-parser-pdf-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    |    +--- org.apache.pdfbox:pdfbox:2.0.29 -> 2.0.27 (*)
|    |    +--- org.apache.pdfbox:pdfbox-tools:2.0.29
|    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    \--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8 (*)
|    +--- org.apache.tika:tika-parser-pkg-module:2.9.0
|    |    +--- org.tukaani:xz:1.9
|    |    +--- org.brotli:dec:0.1.2
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    \--- com.github.junrar:junrar:7.5.5
|    |         \--- org.slf4j:slf4j-api:1.7.36
|    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-webarchive-module:2.9.0
|    |    +--- org.netpreserve:jwarc:0.28.1
|    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.gagravarr:vorbis-java-tika:0.8
|    \--- org.gagravarr:vorbis-java-core:0.8
+--- org.apache.logging.log4j:log4j-to-slf4j:2.23.1
|    +--- org.apache.logging.log4j:log4j-api:2.23.1 -> 2.17.2
|    \--- org.slf4j:slf4j-api:2.0.9 -> 1.7.36
+--- ch.qos.logback:logback-core:1.2.13 (c)
+--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0 (c)
+--- org.bouncycastle:bcutil-jdk18on:1.78 (c)
\--- org.apache.james:apache-mime4j-core:0.8.10 (c)

runtimeClasspath - Runtime classpath of source set 'main'.
+--- com.google.guava:guava:32.1.2-jre
|    +--- com.google.guava:failureaccess:1.0.1
|    +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    +--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.checkerframework:checker-qual:3.33.0
|    +--- com.google.errorprone:error_prone_annotations:2.18.0
|    \--- com.google.j2objc:j2objc-annotations:2.8
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.13.5
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.13.5 -> 2.15.0
|    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0 (*)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :common
|    +--- com.blackduck.integration:blackduck-common:67.0.20
|    |    +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|    |    |    \--- com.blackduck.integration:integration-rest:11.1.2
|    |    |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|    |    |         |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1 -> 5.8.2
|    |    |         |    |    +--- org.junit:junit-bom:5.8.2
|    |    |         |    |    |    +--- org.junit.jupiter:junit-jupiter-api:5.8.2 (c)
|    |    |         |    |    |    \--- org.junit.platform:junit-platform-commons:1.8.2 (c)
|    |    |         |    |    +--- org.opentest4j:opentest4j:1.2.0
|    |    |         |    |    \--- org.junit.platform:junit-platform-commons:1.8.2
|    |    |         |    |         \--- org.junit:junit-bom:5.8.2 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    |         |    +--- org.apache.commons:commons-lang3:3.12.0
|    |    |         |    +--- org.apache.commons:commons-text:1.10.0
|    |    |         |    |    \--- org.apache.commons:commons-lang3:3.12.0
|    |    |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |         |    +--- org.apache.commons:commons-compress:1.26.1
|    |    |         |    |    +--- commons-codec:commons-codec:1.16.1 -> 1.15
|    |    |         |    |    +--- commons-io:commons-io:2.15.1
|    |    |         |    |    \--- org.apache.commons:commons-lang3:3.14.0 -> 3.12.0
|    |    |         |    +--- commons-codec:commons-codec:1.15
|    |    |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|    |    |         |    |    +--- commons-logging:commons-logging:1.3.5
|    |    |         |    |    \--- commons-collections:commons-collections:3.2.2
|    |    |         |    +--- org.apache.commons:commons-collections4:4.4
|    |    |         |    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|    |    |         |    +--- org.jetbrains:annotations:24.0.1
|    |    |         |    +--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0
|    |    |         |    |    +--- net.minidev:json-smart:2.4.7 -> 2.4.11
|    |    |         |    |    |    \--- net.minidev:accessors-smart:2.4.11
|    |    |         |    |    |         \--- org.ow2.asm:asm:9.3 -> 9.6
|    |    |         |    |    \--- org.slf4j:slf4j-api:1.7.33 -> 1.7.36
|    |    |         |    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    |    |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|    |    |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.13.5 (*)
|    |    |         |         +--- com.fasterxml.jackson.core:jackson-core:2.14.0 -> 2.15.0
|    |    |         |         \--- org.apache.commons:commons-collections4:4.4
|    |    |         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |         \--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    +--- com.blackduck.integration:phone-home-client:7.0.1
|    |    |    \--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3 (*)
|    |    +--- com.blackduck.integration:integration-bdio:27.0.4
|    |    |    \--- com.blackduck.integration:integration-common:27.0.3 (*)
|    |    \--- com.blackducksoftware.bdio:bdio2:3.2.12
|    |         +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |         |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |         |    \--- com.google.guava:guava:23.3-jre -> 32.1.2-jre (*)
|    |         +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (*)
|    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (*)
|    |         +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |         +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    |         +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |         |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|    |         |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.13.5 (*)
|    |         |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|    |         |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |         |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|    |         |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.16
|    |         |    |         \--- org.apache.httpcomponents:httpcore:4.4.16
|    |         |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    |         |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.36
|    |         |    |    \--- org.slf4j:slf4j-api:1.7.36
|    |         |    \--- commons-io:commons-io:2.6 -> 2.15.1
|    |         \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.4
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    \--- com.blackduck.integration:integration-common -> 27.0.3 (*)
+--- project :configuration
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common (*)
|    +--- org.springframework:spring-jcl:5.3.34
|    +--- org.springframework:spring-core:5.3.34
|    |    \--- org.springframework:spring-jcl:5.3.34
|    +--- org.springframework:spring-aop:5.3.34
|    |    +--- org.springframework:spring-beans:5.3.34
|    |    |    \--- org.springframework:spring-core:5.3.34 (*)
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    +--- org.springframework:spring-beans:5.3.34 (*)
|    +--- org.springframework:spring-expression:5.3.34
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    +--- org.springframework:spring-context:5.3.34
|    |    +--- org.springframework:spring-aop:5.3.34 (*)
|    |    +--- org.springframework:spring-beans:5.3.34 (*)
|    |    +--- org.springframework:spring-core:5.3.34 (*)
|    |    \--- org.springframework:spring-expression:5.3.34 (*)
|    \--- org.springframework.boot:spring-boot -> 2.7.12
|         +--- org.springframework:spring-core:5.3.27 -> 5.3.34 (*)
|         \--- org.springframework:spring-context:5.3.27 -> 5.3.34 (*)
+--- project :detectable
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common (*)
|    +--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
|    +--- com.blackduck.integration:integration-rest -> 11.1.2 (*)
|    +--- org.tomlj:tomlj:1.1.1
|    |    +--- org.antlr:antlr4-runtime:4.11.1
|    |    \--- org.checkerframework:checker-qual:3.21.2 -> 3.33.0
|    +--- com.moandjiezana.toml:toml4j:0.7.2
|    |    \--- com.google.code.gson:gson:2.8.1 -> 2.9.1
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.11.1
|    \--- guru.nidi:graphviz-java:0.18.1
|         +--- org.webjars.npm:viz.js-graphviz-java:2.1.3
|         +--- guru.nidi.com.kitfox:svgSalamander:1.1.3
|         +--- net.arnx:nashorn-promise:0.1.1
|         +--- org.apache.commons:commons-exec:1.3
|         +--- com.google.code.findbugs:jsr305:3.0.2
|         +--- org.slf4j:jcl-over-slf4j:1.7.30 -> 1.7.36 (*)
|         +--- org.slf4j:jul-to-slf4j:1.7.30 -> 1.7.36
|         |    \--- org.slf4j:slf4j-api:1.7.36
|         \--- org.slf4j:slf4j-api:1.7.30 -> 1.7.36
+--- project :detector
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :detectable (*)
|    +--- project :common (*)
|    \--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
+--- ch.qos.logback:logback-classic:1.2.13
|    +--- ch.qos.logback:logback-core:1.2.13
|    \--- org.slf4j:slf4j-api:1.7.32 -> 1.7.36
+--- com.blackducksoftware.bdio:bdio-protobuf:3.2.12
|    +--- com.google.protobuf:protobuf-java:3.25.5
|    +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    \--- commons-lang:commons-lang:2.6
+--- com.blackduck.integration:blackduck-common:67.0.20 (*)
+--- com.blackduck.integration:blackduck-upload-common:4.1.2
|    +--- commons-codec:commons-codec:1.15
|    \--- com.blackduck.integration:integration-rest:11.0.0 -> 11.1.2 (*)
+--- com.blackducksoftware:method-analyzer-core:1.0.1
|    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|    +--- org.slf4j:slf4j-api:1.7.36
|    +--- com.google.guava:guava:31.1-jre -> 32.1.2-jre (*)
|    \--- org.ow2.asm:asm:9.6
+--- com.blackduck.integration:component-locator:2.1.1
|    +--- com.google.code.gson:gson:2.7 -> 2.9.1
|    +--- ch.qos.logback:logback-classic:1.2.13 (*)
|    \--- commons-io:commons-io:2.6 -> 2.15.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework:spring-jcl:5.3.34
+--- org.springframework:spring-core:5.3.34 (*)
+--- org.springframework:spring-aop:5.3.34 (*)
+--- org.springframework:spring-beans:5.3.34 (*)
+--- org.springframework:spring-expression:5.3.34 (*)
+--- org.springframework:spring-context:5.3.34 (*)
+--- org.springframework.boot:spring-boot -> 2.7.12 (*)
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.36
+--- org.apache.pdfbox:pdfbox:2.0.27
|    +--- org.apache.pdfbox:fontbox:2.0.27 -> 2.0.29
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- org.apache.tika:tika-core:2.9.0
|    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    \--- commons-io:commons-io:2.13.0 -> 2.15.1
+--- org.apache.tika:tika-parsers-standard-package:2.9.0
|    +--- org.apache.tika:tika-parser-apple-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0
|    |    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    |    \--- com.googlecode.plist:dd-plist:1.27
|    +--- org.apache.tika:tika-parser-audiovideo-module:2.9.0
|    |    \--- com.drewnoakes:metadata-extractor:2.18.0
|    |         \--- com.adobe.xmp:xmpcore:6.1.11
|    +--- org.apache.tika:tika-parser-cad-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0
|    |    |    +--- org.apache.tika:tika-parser-html-module:2.9.0
|    |    |    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    |    |    \--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.tika:tika-parser-text-module:2.9.0
|    |    |    |    +--- com.github.albfernandez:juniversalchardet:2.4.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- org.apache.commons:commons-csv:1.10.0
|    |    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- xerces:xercesImpl:2.12.2
|    |    |    |         \--- xml-apis:xml-apis:1.4.01
|    |    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0
|    |    |    |    +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10
|    |    |    |    |    \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    \--- org.apache.james:apache-mime4j-dom:0.8.9
|    |    |    |         +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10 (*)
|    |    |    |         \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    +--- com.pff:java-libpst:0.9.3
|    |    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    |    +--- org.apache.poi:poi:5.2.3
|    |    |    |    +--- commons-codec:commons-codec:1.15
|    |    |    |    +--- org.apache.commons:commons-collections4:4.4
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.zaxxer:SparseBitSet:1.2
|    |    |    |    \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    +--- org.apache.poi:poi-scratchpad:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    \--- commons-codec:commons-codec:1.15
|    |    |    +--- org.apache.poi:poi-ooxml:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.poi:poi-ooxml-lite:5.2.3
|    |    |    |    |    \--- org.apache.xmlbeans:xmlbeans:5.1.1
|    |    |    |    |         \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.xmlbeans:xmlbeans:5.1.1 (*)
|    |    |    |    +--- org.apache.commons:commons-compress:1.21 -> 1.26.1 (*)
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.github.virtuald:curvesapi:1.07
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    \--- org.apache.commons:commons-collections4:4.4
|    |    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess:4.0.5
|    |    |    |    +--- org.apache.commons:commons-lang3:3.10 -> 3.12.0
|    |    |    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess-encrypt:4.0.2
|    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.72 -> 1.78
|    |    |    +--- org.bouncycastle:bcmail-jdk18on:1.76
|    |    |    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |    +--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78
|    |    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.78
|    |    |    |    \--- org.bouncycastle:bcpkix-jdk18on:1.76
|    |    |    |         +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |         \--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78 (*)
|    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.15.2 -> 2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-databind:2.15.2 -> 2.13.5 (*)
|    +--- org.apache.tika:tika-parser-code-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.codelibs:jhighlight:1.1.0
|    |    |    \--- commons-io:commons-io:2.7 -> 2.15.1
|    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    +--- org.ow2.asm:asm:9.5 -> 9.6
|    |    +--- com.epam:parso:2.0.14
|    |    |    \--- org.slf4j:slf4j-api:1.7.5 -> 1.7.36
|    |    \--- org.tallison:jmatio:1.5
|    |         \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    +--- org.apache.tika:tika-parser-crypto-module:2.9.0
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-digest-commons:2.9.0
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-font-module:2.9.0
|    |    \--- org.apache.pdfbox:fontbox:2.0.29 (*)
|    +--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-image-module:2.9.0
|    |    +--- com.drewnoakes:metadata-extractor:2.18.0 (*)
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0
|    |    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    |    \--- org.apache.pdfbox:xmpbox:2.0.29
|    |    |         \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    +--- com.github.jai-imageio:jai-imageio-core:1.4.0
|    |    \--- org.apache.pdfbox:jbig2-imageio:3.0.4
|    +--- org.apache.tika:tika-parser-mail-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    \--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0 (*)
|    +--- org.slf4j:jcl-over-slf4j:2.0.7 -> 1.7.36 (*)
|    +--- org.apache.tika:tika-parser-miscoffice-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    +--- org.apache.commons:commons-collections4:4.4
|    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8
|    |    |    +--- jakarta.xml.bind:jakarta.xml.bind-api:2.3.3
|    |    |    +--- org.glassfish.jaxb:txw2:2.3.8
|    |    |    +--- com.sun.istack:istack-commons-runtime:3.0.12
|    |    |    \--- com.sun.activation:jakarta.activation:1.2.2
|    |    \--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-news-module:2.9.0
|    |    +--- com.rometools:rome:2.1.0
|    |    |    +--- com.rometools:rome-utils:2.1.0
|    |    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    |    +--- org.jdom:jdom2:2.0.6.1
|    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    \--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    +--- org.apache.tika:tika-parser-ocr-module:2.9.0
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    \--- org.apache.commons:commons-exec:1.3
|    +--- org.apache.tika:tika-parser-pdf-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    |    +--- org.apache.pdfbox:pdfbox:2.0.29 -> 2.0.27 (*)
|    |    +--- org.apache.pdfbox:pdfbox-tools:2.0.29
|    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    \--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8 (*)
|    +--- org.apache.tika:tika-parser-pkg-module:2.9.0
|    |    +--- org.tukaani:xz:1.9
|    |    +--- org.brotli:dec:0.1.2
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    \--- com.github.junrar:junrar:7.5.5
|    |         \--- org.slf4j:slf4j-api:1.7.36
|    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-webarchive-module:2.9.0
|    |    +--- org.netpreserve:jwarc:0.28.1
|    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.gagravarr:vorbis-java-tika:0.8
|    \--- org.gagravarr:vorbis-java-core:0.8
+--- org.apache.logging.log4j:log4j-to-slf4j:2.23.1
|    +--- org.apache.logging.log4j:log4j-api:2.23.1 -> 2.17.2
|    \--- org.slf4j:slf4j-api:2.0.9 -> 1.7.36
+--- ch.qos.logback:logback-core:1.2.13 (c)
+--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0 (c)
+--- org.bouncycastle:bcutil-jdk18on:1.78 (c)
\--- org.apache.james:apache-mime4j-core:0.8.10 (c)

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
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.13.5
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.13.5 -> 2.15.0
|    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0 (*)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :common
|    \--- com.blackduck.integration:blackduck-common:67.0.20
|         +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|         |    \--- com.blackduck.integration:integration-rest:11.1.2
|         |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|         |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|         |         |    +--- org.apache.commons:commons-lang3:3.12.0
|         |         |    +--- org.apache.commons:commons-text:1.10.0
|         |         |    |    \--- org.apache.commons:commons-lang3:3.12.0
|         |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|         |         |    +--- org.apache.commons:commons-compress:1.26.1
|         |         |    |    +--- commons-codec:commons-codec:1.16.1 -> 1.15
|         |         |    |    +--- commons-io:commons-io:2.15.1
|         |         |    |    \--- org.apache.commons:commons-lang3:3.14.0 -> 3.12.0
|         |         |    +--- commons-codec:commons-codec:1.15
|         |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|         |         |    |    +--- commons-logging:commons-logging:1.3.5
|         |         |    |    \--- commons-collections:commons-collections:3.2.2
|         |         |    +--- org.apache.commons:commons-collections4:4.4
|         |         |    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|         |         |    +--- org.jetbrains:annotations:24.0.1
|         |         |    +--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0
|         |         |    |    +--- net.minidev:json-smart:2.4.7 -> 2.4.11
|         |         |    |    |    \--- net.minidev:accessors-smart:2.4.11
|         |         |    |    |         \--- org.ow2.asm:asm:9.3 -> 9.6
|         |         |    |    \--- org.slf4j:slf4j-api:1.7.33 -> 1.7.36
|         |         |    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|         |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|         |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.13.5 (*)
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
|              +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (*)
|              +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|              +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (*)
|              +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|              +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|              +--- com.github.jsonld-java:jsonld-java:0.12.3
|              |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|              |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.13.5 (*)
|              |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|              |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|              |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|              |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.16
|              |    |         \--- org.apache.httpcomponents:httpcore:4.4.16
|              |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|              |    \--- commons-io:commons-io:2.6 -> 2.15.1
|              \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.4
+--- project :configuration
+--- project :detectable
+--- project :detector
+--- ch.qos.logback:logback-classic:1.2.13
|    +--- ch.qos.logback:logback-core:1.2.13
|    \--- org.slf4j:slf4j-api:1.7.32 -> 1.7.36
+--- com.blackducksoftware.bdio:bdio-protobuf:3.2.12
|    +--- com.google.protobuf:protobuf-java:3.25.5
|    +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    \--- commons-lang:commons-lang:2.6
+--- com.blackduck.integration:blackduck-common:67.0.20 (*)
+--- com.blackduck.integration:blackduck-upload-common:4.1.2
+--- com.blackducksoftware:method-analyzer-core:1.0.1
|    +--- com.google.guava:guava:31.1-jre -> 32.1.2-jre (*)
|    \--- org.ow2.asm:asm:9.6
+--- com.blackduck.integration:component-locator:2.1.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework:spring-jcl:5.3.34
+--- org.springframework:spring-core:5.3.34
|    \--- org.springframework:spring-jcl:5.3.34
+--- org.springframework:spring-aop:5.3.34
|    +--- org.springframework:spring-beans:5.3.34
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    \--- org.springframework:spring-core:5.3.34 (*)
+--- org.springframework:spring-beans:5.3.34 (*)
+--- org.springframework:spring-expression:5.3.34
|    \--- org.springframework:spring-core:5.3.34 (*)
+--- org.springframework:spring-context:5.3.34
|    +--- org.springframework:spring-aop:5.3.34 (*)
|    +--- org.springframework:spring-beans:5.3.34 (*)
|    +--- org.springframework:spring-core:5.3.34 (*)
|    \--- org.springframework:spring-expression:5.3.34 (*)
+--- org.springframework.boot:spring-boot -> 2.7.12
|    +--- org.springframework:spring-core:5.3.27 -> 5.3.34 (*)
|    \--- org.springframework:spring-context:5.3.27 -> 5.3.34 (*)
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.36
+--- org.apache.pdfbox:pdfbox:2.0.27
|    +--- org.apache.pdfbox:fontbox:2.0.27 -> 2.0.29
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- org.apache.tika:tika-core:2.9.0
|    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    \--- commons-io:commons-io:2.13.0 -> 2.15.1
+--- org.apache.tika:tika-parsers-standard-package:2.9.0
|    +--- org.apache.tika:tika-parser-apple-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0
|    |    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    |    \--- com.googlecode.plist:dd-plist:1.27
|    +--- org.apache.tika:tika-parser-audiovideo-module:2.9.0
|    |    \--- com.drewnoakes:metadata-extractor:2.18.0
|    |         \--- com.adobe.xmp:xmpcore:6.1.11
|    +--- org.apache.tika:tika-parser-cad-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0
|    |    |    +--- org.apache.tika:tika-parser-html-module:2.9.0
|    |    |    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    |    |    \--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.tika:tika-parser-text-module:2.9.0
|    |    |    |    +--- com.github.albfernandez:juniversalchardet:2.4.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- org.apache.commons:commons-csv:1.10.0
|    |    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- xerces:xercesImpl:2.12.2
|    |    |    |         \--- xml-apis:xml-apis:1.4.01
|    |    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0
|    |    |    |    +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10
|    |    |    |    |    \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    \--- org.apache.james:apache-mime4j-dom:0.8.9
|    |    |    |         +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10 (*)
|    |    |    |         \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    +--- com.pff:java-libpst:0.9.3
|    |    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    |    +--- org.apache.poi:poi:5.2.3
|    |    |    |    +--- commons-codec:commons-codec:1.15
|    |    |    |    +--- org.apache.commons:commons-collections4:4.4
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.zaxxer:SparseBitSet:1.2
|    |    |    |    \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    +--- org.apache.poi:poi-scratchpad:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    \--- commons-codec:commons-codec:1.15
|    |    |    +--- org.apache.poi:poi-ooxml:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.poi:poi-ooxml-lite:5.2.3
|    |    |    |    |    \--- org.apache.xmlbeans:xmlbeans:5.1.1
|    |    |    |    |         \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.xmlbeans:xmlbeans:5.1.1 (*)
|    |    |    |    +--- org.apache.commons:commons-compress:1.21 -> 1.26.1 (*)
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.github.virtuald:curvesapi:1.07
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    \--- org.apache.commons:commons-collections4:4.4
|    |    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess:4.0.5
|    |    |    |    +--- org.apache.commons:commons-lang3:3.10 -> 3.12.0
|    |    |    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess-encrypt:4.0.2
|    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.72 -> 1.78
|    |    |    +--- org.bouncycastle:bcmail-jdk18on:1.76
|    |    |    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |    +--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78
|    |    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.78
|    |    |    |    \--- org.bouncycastle:bcpkix-jdk18on:1.76
|    |    |    |         +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |         \--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78 (*)
|    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.15.2 -> 2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-databind:2.15.2 -> 2.13.5 (*)
|    +--- org.apache.tika:tika-parser-code-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.codelibs:jhighlight:1.1.0
|    |    |    \--- commons-io:commons-io:2.7 -> 2.15.1
|    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    +--- org.ow2.asm:asm:9.5 -> 9.6
|    |    +--- com.epam:parso:2.0.14
|    |    |    \--- org.slf4j:slf4j-api:1.7.5 -> 1.7.36
|    |    \--- org.tallison:jmatio:1.5
|    |         \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    +--- org.apache.tika:tika-parser-crypto-module:2.9.0
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-digest-commons:2.9.0
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-font-module:2.9.0
|    |    \--- org.apache.pdfbox:fontbox:2.0.29 (*)
|    +--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-image-module:2.9.0
|    |    +--- com.drewnoakes:metadata-extractor:2.18.0 (*)
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0
|    |    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    |    \--- org.apache.pdfbox:xmpbox:2.0.29
|    |    |         \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    +--- com.github.jai-imageio:jai-imageio-core:1.4.0
|    |    \--- org.apache.pdfbox:jbig2-imageio:3.0.4
|    +--- org.apache.tika:tika-parser-mail-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    \--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0 (*)
|    +--- org.slf4j:jcl-over-slf4j:2.0.7 -> 1.7.36
|    |    \--- org.slf4j:slf4j-api:1.7.36
|    +--- org.apache.tika:tika-parser-miscoffice-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    +--- org.apache.commons:commons-collections4:4.4
|    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8
|    |    |    +--- jakarta.xml.bind:jakarta.xml.bind-api:2.3.3
|    |    |    +--- org.glassfish.jaxb:txw2:2.3.8
|    |    |    \--- com.sun.istack:istack-commons-runtime:3.0.12
|    |    \--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-news-module:2.9.0
|    |    +--- com.rometools:rome:2.1.0
|    |    |    +--- com.rometools:rome-utils:2.1.0
|    |    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    |    +--- org.jdom:jdom2:2.0.6.1
|    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    \--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    +--- org.apache.tika:tika-parser-ocr-module:2.9.0
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    \--- org.apache.commons:commons-exec:1.3
|    +--- org.apache.tika:tika-parser-pdf-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    |    +--- org.apache.pdfbox:pdfbox:2.0.29 -> 2.0.27 (*)
|    |    +--- org.apache.pdfbox:pdfbox-tools:2.0.29
|    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    \--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8 (*)
|    +--- org.apache.tika:tika-parser-pkg-module:2.9.0
|    |    +--- org.tukaani:xz:1.9
|    |    +--- org.brotli:dec:0.1.2
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    \--- com.github.junrar:junrar:7.5.5
|    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-webarchive-module:2.9.0
|    |    +--- org.netpreserve:jwarc:0.28.1
|    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.gagravarr:vorbis-java-tika:0.8
|    \--- org.gagravarr:vorbis-java-core:0.8
+--- org.apache.logging.log4j:log4j-to-slf4j:2.23.1
|    +--- org.apache.logging.log4j:log4j-api:2.23.1 -> 2.17.2
|    \--- org.slf4j:slf4j-api:2.0.9 -> 1.7.36
+--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    +--- org.apiguardian:apiguardian-api:1.1.0 -> 1.1.2
|    +--- org.opentest4j:opentest4j:1.2.0
|    \--- org.junit.platform:junit-platform-commons:1.7.1 -> 1.8.2
|         +--- org.junit:junit-bom:5.8.2
|         |    +--- org.junit.jupiter:junit-jupiter-api:5.8.2 -> 5.7.1 (c)
|         |    +--- org.junit.jupiter:junit-jupiter-params:5.8.2 -> 5.4.2 (c)
|         |    \--- org.junit.platform:junit-platform-commons:1.8.2 (c)
|         \--- org.apiguardian:apiguardian-api:1.1.2
+--- org.junit-pioneer:junit-pioneer:0.3.3
+--- org.junit.jupiter:junit-jupiter-params:5.4.2
|    +--- org.apiguardian:apiguardian-api:1.0.0 -> 1.1.2
|    \--- org.junit.jupiter:junit-jupiter-api:5.4.2 -> 5.7.1 (*)
+--- org.mockito:mockito-core:2.+ -> 4.5.1
|    +--- net.bytebuddy:byte-buddy:1.12.9 -> 1.12.23
|    \--- net.bytebuddy:byte-buddy-agent:1.12.9 -> 1.12.23
+--- org.assertj:assertj-core:3.13.2
+--- org.skyscreamer:jsonassert:1.5.0
|    \--- com.vaadin.external.google:android-json:0.0.20131108.vaadin1
+--- org.mockito:mockito-inline:2.+ -> 2.28.2
|    \--- org.mockito:mockito-core:2.28.2 -> 4.5.1 (*)
+--- project :common-test
+--- com.github.docker-java:docker-java-core:3.2.13
|    +--- com.github.docker-java:docker-java-api:3.2.13
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.10.3 -> 2.13.5 (*)
|    |    \--- org.slf4j:slf4j-api:1.7.30 -> 1.7.36
|    +--- com.github.docker-java:docker-java-transport:3.2.13 -> 3.3.0
|    +--- org.slf4j:slf4j-api:1.7.30 -> 1.7.36
|    +--- commons-io:commons-io:2.6 -> 2.15.1
|    +--- org.apache.commons:commons-compress:1.21 -> 1.26.1 (*)
|    +--- org.apache.commons:commons-lang3:3.12.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.13.5 (*)
|    +--- com.google.guava:guava:19.0 -> 32.1.2-jre (*)
|    \--- org.bouncycastle:bcpkix-jdk15on:1.64
|         \--- org.bouncycastle:bcprov-jdk15on:1.64
+--- com.github.docker-java:docker-java-transport-httpclient5:3.3.0
|    +--- com.github.docker-java:docker-java-transport:3.3.0
|    +--- org.apache.httpcomponents.client5:httpclient5:5.0.3 -> 5.1.4
|    |    +--- org.apache.httpcomponents.core5:httpcore5:5.1.5
|    |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    |    \--- commons-codec:commons-codec:1.15
|    \--- net.java.dev.jna:jna:5.12.1
+--- ch.qos.logback:logback-core:1.2.13 (c)
+--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0 (c)
+--- org.bouncycastle:bcutil-jdk18on:1.78 (c)
\--- org.apache.james:apache-mime4j-core:0.8.10 (c)

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
+--- com.github.docker-java:docker-java-core:3.2.13 (n)
\--- com.github.docker-java:docker-java-transport-httpclient5:3.3.0 (n)

testRuntimeClasspath - Runtime classpath of source set 'test'.
+--- com.google.guava:guava:32.1.2-jre
|    +--- com.google.guava:failureaccess:1.0.1
|    +--- com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
|    +--- com.google.code.findbugs:jsr305:3.0.2
|    +--- org.checkerframework:checker-qual:3.33.0
|    +--- com.google.errorprone:error_prone_annotations:2.18.0
|    \--- com.google.j2objc:j2objc-annotations:2.8
+--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.13.5
|    |    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0
|    |    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0 (c)
|    |    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (c)
|    |    |         +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (c)
|    |    |         \--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (c)
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.13.5 -> 2.15.0
|    |    \--- com.fasterxml.jackson:jackson-bom:2.13.5 -> 2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    \--- com.fasterxml.jackson:jackson-bom:2.15.0 (*)
+--- org.yaml:snakeyaml:2.0
+--- com.fasterxml.jackson.core:jackson-core:2.15.0
+--- org.freemarker:freemarker:2.3.31
+--- org.apache.httpcomponents:httpclient-osgi:4.5.14
|    +--- org.apache.httpcomponents:httpclient:4.5.14
|    |    +--- org.apache.httpcomponents:httpcore:4.4.16
|    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    \--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- commons-codec:commons-codec:1.11 -> 1.15
|    +--- org.apache.httpcomponents:httpmime:4.5.14
|    |    \--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    +--- org.apache.httpcomponents:httpclient-cache:4.5.14
|    |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- org.apache.httpcomponents:fluent-hc:4.5.14
|         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|         \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- project :common
|    +--- com.blackduck.integration:blackduck-common:67.0.20
|    |    +--- com.blackduck.integration:blackduck-common-api:2023.4.2.13
|    |    |    \--- com.blackduck.integration:integration-rest:11.1.2
|    |    |         +--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3
|    |    |         |    +--- org.junit.jupiter:junit-jupiter-api:5.7.1
|    |    |         |    |    +--- org.apiguardian:apiguardian-api:1.1.0
|    |    |         |    |    +--- org.opentest4j:opentest4j:1.2.0
|    |    |         |    |    \--- org.junit.platform:junit-platform-commons:1.7.1 -> 1.8.2
|    |    |         |    |         \--- org.junit:junit-bom:5.8.2
|    |    |         |    |              +--- org.junit.jupiter:junit-jupiter-api:5.8.2 -> 5.7.1 (c)
|    |    |         |    |              +--- org.junit.jupiter:junit-jupiter-engine:5.8.2 -> 5.7.1 (c)
|    |    |         |    |              +--- org.junit.jupiter:junit-jupiter-params:5.8.2 -> 5.4.2 (c)
|    |    |         |    |              +--- org.junit.platform:junit-platform-commons:1.8.2 (c)
|    |    |         |    |              \--- org.junit.platform:junit-platform-engine:1.8.2 (c)
|    |    |         |    +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |         |    +--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    |         |    +--- org.apache.commons:commons-lang3:3.12.0
|    |    |         |    +--- org.apache.commons:commons-text:1.10.0
|    |    |         |    |    \--- org.apache.commons:commons-lang3:3.12.0
|    |    |         |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |         |    +--- org.apache.commons:commons-compress:1.26.1
|    |    |         |    |    +--- commons-codec:commons-codec:1.16.1 -> 1.15
|    |    |         |    |    +--- commons-io:commons-io:2.15.1
|    |    |         |    |    \--- org.apache.commons:commons-lang3:3.14.0 -> 3.12.0
|    |    |         |    +--- commons-codec:commons-codec:1.15
|    |    |         |    +--- commons-beanutils:commons-beanutils:1.11.0
|    |    |         |    |    +--- commons-logging:commons-logging:1.3.5
|    |    |         |    |    \--- commons-collections:commons-collections:3.2.2
|    |    |         |    +--- org.apache.commons:commons-collections4:4.4
|    |    |         |    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|    |    |         |    +--- org.jetbrains:annotations:24.0.1
|    |    |         |    +--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0
|    |    |         |    |    +--- net.minidev:json-smart:2.4.7 -> 2.4.11
|    |    |         |    |    |    \--- net.minidev:accessors-smart:2.4.11
|    |    |         |    |    |         \--- org.ow2.asm:asm:9.3 -> 9.6
|    |    |         |    |    \--- org.slf4j:slf4j-api:1.7.33 -> 1.7.36
|    |    |         |    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    |    |         |    \--- com.flipkart.zjsonpatch:zjsonpatch:0.4.16
|    |    |         |         +--- com.fasterxml.jackson.core:jackson-databind:2.14.0 -> 2.13.5 (*)
|    |    |         |         +--- com.fasterxml.jackson.core:jackson-core:2.14.0 -> 2.15.0
|    |    |         |         \--- org.apache.commons:commons-collections4:4.4
|    |    |         +--- org.apache.httpcomponents:httpclient:4.5.14 (*)
|    |    |         \--- org.apache.httpcomponents:httpmime:4.5.14 (*)
|    |    +--- com.blackduck.integration:phone-home-client:7.0.1
|    |    |    \--- com.blackduck.integration:integration-common:27.0.2 -> 27.0.3 (*)
|    |    +--- com.blackduck.integration:integration-bdio:27.0.4
|    |    |    \--- com.blackduck.integration:integration-common:27.0.3 (*)
|    |    \--- com.blackducksoftware.bdio:bdio2:3.2.12
|    |         +--- com.blackducksoftware.magpie:magpie:0.6.0
|    |         |    +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |         |    \--- com.google.guava:guava:23.3-jre -> 32.1.2-jre (*)
|    |         +--- com.fasterxml.jackson.core:jackson-annotations:2.15.0 -> 2.13.5 (*)
|    |         +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    |         +--- com.fasterxml.jackson.core:jackson-databind:2.15.0 -> 2.13.5 (*)
|    |         +--- com.google.code.findbugs:jsr305:2.0.3 -> 3.0.2
|    |         +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    |         +--- com.github.jsonld-java:jsonld-java:0.12.3
|    |         |    +--- com.fasterxml.jackson.core:jackson-core:2.9.7 -> 2.15.0
|    |         |    +--- com.fasterxml.jackson.core:jackson-databind:2.9.7 -> 2.13.5 (*)
|    |         |    +--- org.apache.httpcomponents:httpclient-osgi:4.5.6 -> 4.5.14 (*)
|    |         |    +--- org.apache.httpcomponents:httpcore-osgi:4.4.10
|    |         |    |    +--- org.apache.httpcomponents:httpcore:4.4.10 -> 4.4.16
|    |         |    |    \--- org.apache.httpcomponents:httpcore-nio:4.4.10 -> 4.4.16
|    |         |    |         \--- org.apache.httpcomponents:httpcore:4.4.16
|    |         |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    |         |    +--- org.slf4j:jcl-over-slf4j:1.7.25 -> 1.7.36
|    |         |    |    \--- org.slf4j:slf4j-api:1.7.36
|    |         |    \--- commons-io:commons-io:2.6 -> 2.15.1
|    |         \--- org.reactivestreams:reactive-streams:1.0.2 -> 1.0.4
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    \--- com.blackduck.integration:integration-common -> 27.0.3 (*)
+--- project :configuration
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common (*)
|    +--- org.springframework:spring-jcl:5.3.34
|    +--- org.springframework:spring-core:5.3.34
|    |    \--- org.springframework:spring-jcl:5.3.34
|    +--- org.springframework:spring-aop:5.3.34
|    |    +--- org.springframework:spring-beans:5.3.34
|    |    |    \--- org.springframework:spring-core:5.3.34 (*)
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    +--- org.springframework:spring-beans:5.3.34 (*)
|    +--- org.springframework:spring-expression:5.3.34
|    |    \--- org.springframework:spring-core:5.3.34 (*)
|    +--- org.springframework:spring-context:5.3.34
|    |    +--- org.springframework:spring-aop:5.3.34 (*)
|    |    +--- org.springframework:spring-beans:5.3.34 (*)
|    |    +--- org.springframework:spring-core:5.3.34 (*)
|    |    \--- org.springframework:spring-expression:5.3.34 (*)
|    \--- org.springframework.boot:spring-boot -> 2.7.12
|         +--- org.springframework:spring-core:5.3.27 -> 5.3.34 (*)
|         \--- org.springframework:spring-context:5.3.27 -> 5.3.34 (*)
+--- project :detectable
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :common (*)
|    +--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
|    +--- com.blackduck.integration:integration-rest -> 11.1.2 (*)
|    +--- org.tomlj:tomlj:1.1.1
|    |    +--- org.antlr:antlr4-runtime:4.11.1
|    |    \--- org.checkerframework:checker-qual:3.21.2 -> 3.33.0
|    +--- com.moandjiezana.toml:toml4j:0.7.2
|    |    \--- com.google.code.gson:gson:2.8.1 -> 2.9.1
|    +--- com.paypal.digraph:digraph-parser:1.0
|    |    \--- org.antlr:antlr4-runtime:4.2 -> 4.11.1
|    \--- guru.nidi:graphviz-java:0.18.1
|         +--- org.webjars.npm:viz.js-graphviz-java:2.1.3
|         +--- guru.nidi.com.kitfox:svgSalamander:1.1.3
|         +--- net.arnx:nashorn-promise:0.1.1
|         +--- org.apache.commons:commons-exec:1.3
|         +--- com.google.code.findbugs:jsr305:3.0.2
|         +--- org.slf4j:jcl-over-slf4j:1.7.30 -> 1.7.36 (*)
|         +--- org.slf4j:jul-to-slf4j:1.7.30 -> 1.7.36
|         |    \--- org.slf4j:slf4j-api:1.7.36
|         \--- org.slf4j:slf4j-api:1.7.30 -> 1.7.36
+--- project :detector
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    +--- project :detectable (*)
|    +--- project :common (*)
|    \--- com.blackduck.integration:integration-bdio -> 27.0.4 (*)
+--- ch.qos.logback:logback-classic:1.2.13
|    +--- ch.qos.logback:logback-core:1.2.13
|    \--- org.slf4j:slf4j-api:1.7.32 -> 1.7.36
+--- com.blackducksoftware.bdio:bdio-protobuf:3.2.12
|    +--- com.google.protobuf:protobuf-java:3.25.5
|    +--- com.google.guava:guava:30.1.1-jre -> 32.1.2-jre (*)
|    \--- commons-lang:commons-lang:2.6
+--- com.blackduck.integration:blackduck-common:67.0.20 (*)
+--- com.blackduck.integration:blackduck-upload-common:4.1.2
|    +--- commons-codec:commons-codec:1.15
|    \--- com.blackduck.integration:integration-rest:11.0.0 -> 11.1.2 (*)
+--- com.blackducksoftware:method-analyzer-core:1.0.1
|    +--- com.google.code.gson:gson:2.10.1 -> 2.9.1
|    +--- org.slf4j:slf4j-api:1.7.36
|    +--- com.google.guava:guava:31.1-jre -> 32.1.2-jre (*)
|    \--- org.ow2.asm:asm:9.6
+--- com.blackduck.integration:component-locator:2.1.1
|    +--- com.google.code.gson:gson:2.7 -> 2.9.1
|    +--- ch.qos.logback:logback-classic:1.2.13 (*)
|    \--- commons-io:commons-io:2.6 -> 2.15.1
+--- org.apache.maven.shared:maven-invoker:3.0.0
|    +--- org.codehaus.plexus:plexus-utils:3.0.24
|    \--- org.codehaus.plexus:plexus-component-annotations:1.7
+--- org.springframework:spring-jcl:5.3.34
+--- org.springframework:spring-core:5.3.34 (*)
+--- org.springframework:spring-aop:5.3.34 (*)
+--- org.springframework:spring-beans:5.3.34 (*)
+--- org.springframework:spring-expression:5.3.34 (*)
+--- org.springframework:spring-context:5.3.34 (*)
+--- org.springframework.boot:spring-boot -> 2.7.12 (*)
+--- org.zeroturnaround:zt-zip:1.13
|    \--- org.slf4j:slf4j-api:1.6.6 -> 1.7.36
+--- org.apache.pdfbox:pdfbox:2.0.27
|    +--- org.apache.pdfbox:fontbox:2.0.27 -> 2.0.29
|    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    \--- commons-logging:commons-logging:1.2 -> 1.3.5
+--- org.apache.tika:tika-core:2.9.0
|    +--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    \--- commons-io:commons-io:2.13.0 -> 2.15.1
+--- org.apache.tika:tika-parsers-standard-package:2.9.0
|    +--- org.apache.tika:tika-parser-apple-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0
|    |    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    |    \--- com.googlecode.plist:dd-plist:1.27
|    +--- org.apache.tika:tika-parser-audiovideo-module:2.9.0
|    |    \--- com.drewnoakes:metadata-extractor:2.18.0
|    |         \--- com.adobe.xmp:xmpcore:6.1.11
|    +--- org.apache.tika:tika-parser-cad-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0
|    |    |    +--- org.apache.tika:tika-parser-html-module:2.9.0
|    |    |    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    |    |    \--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.tika:tika-parser-text-module:2.9.0
|    |    |    |    +--- com.github.albfernandez:juniversalchardet:2.4.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- org.apache.commons:commons-csv:1.10.0
|    |    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0
|    |    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    |    \--- xerces:xercesImpl:2.12.2
|    |    |    |         \--- xml-apis:xml-apis:1.4.01
|    |    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0
|    |    |    |    +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10
|    |    |    |    |    \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    \--- org.apache.james:apache-mime4j-dom:0.8.9
|    |    |    |         +--- org.apache.james:apache-mime4j-core:0.8.9 -> 0.8.10 (*)
|    |    |    |         \--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    +--- com.pff:java-libpst:0.9.3
|    |    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    |    +--- org.apache.poi:poi:5.2.3
|    |    |    |    +--- commons-codec:commons-codec:1.15
|    |    |    |    +--- org.apache.commons:commons-collections4:4.4
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.zaxxer:SparseBitSet:1.2
|    |    |    |    \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    +--- org.apache.poi:poi-scratchpad:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.commons:commons-math3:3.6.1
|    |    |    |    \--- commons-codec:commons-codec:1.15
|    |    |    +--- org.apache.poi:poi-ooxml:5.2.3
|    |    |    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    |    |    +--- org.apache.poi:poi-ooxml-lite:5.2.3
|    |    |    |    |    \--- org.apache.xmlbeans:xmlbeans:5.1.1
|    |    |    |    |         \--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    +--- org.apache.xmlbeans:xmlbeans:5.1.1 (*)
|    |    |    |    +--- org.apache.commons:commons-compress:1.21 -> 1.26.1 (*)
|    |    |    |    +--- commons-io:commons-io:2.11.0 -> 2.15.1
|    |    |    |    +--- com.github.virtuald:curvesapi:1.07
|    |    |    |    +--- org.apache.logging.log4j:log4j-api:2.18.0 -> 2.17.2
|    |    |    |    \--- org.apache.commons:commons-collections4:4.4
|    |    |    +--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess:4.0.5
|    |    |    |    +--- org.apache.commons:commons-lang3:3.10 -> 3.12.0
|    |    |    |    \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    |    +--- com.healthmarketscience.jackcess:jackcess-encrypt:4.0.2
|    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.72 -> 1.78
|    |    |    +--- org.bouncycastle:bcmail-jdk18on:1.76
|    |    |    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |    +--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78
|    |    |    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.78
|    |    |    |    \--- org.bouncycastle:bcpkix-jdk18on:1.76
|    |    |    |         +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    |    |         \--- org.bouncycastle:bcutil-jdk18on:1.76 -> 1.78 (*)
|    |    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    +--- com.fasterxml.jackson.core:jackson-core:2.15.2 -> 2.15.0
|    |    \--- com.fasterxml.jackson.core:jackson-databind:2.15.2 -> 2.13.5 (*)
|    +--- org.apache.tika:tika-parser-code-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.codelibs:jhighlight:1.1.0
|    |    |    \--- commons-io:commons-io:2.7 -> 2.15.1
|    |    +--- org.ccil.cowan.tagsoup:tagsoup:1.2.1
|    |    +--- org.ow2.asm:asm:9.5 -> 9.6
|    |    +--- com.epam:parso:2.0.14
|    |    |    \--- org.slf4j:slf4j-api:1.7.5 -> 1.7.36
|    |    \--- org.tallison:jmatio:1.5
|    |         \--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    +--- org.apache.tika:tika-parser-crypto-module:2.9.0
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-digest-commons:2.9.0
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    \--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    +--- org.apache.tika:tika-parser-font-module:2.9.0
|    |    \--- org.apache.pdfbox:fontbox:2.0.29 (*)
|    +--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-image-module:2.9.0
|    |    +--- com.drewnoakes:metadata-extractor:2.18.0 (*)
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0
|    |    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    |    \--- org.apache.pdfbox:xmpbox:2.0.29
|    |    |         \--- commons-logging:commons-logging:1.2 -> 1.3.5
|    |    +--- com.github.jai-imageio:jai-imageio-core:1.4.0
|    |    \--- org.apache.pdfbox:jbig2-imageio:3.0.4
|    +--- org.apache.tika:tika-parser-mail-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-mail-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    \--- org.apache.tika:tika-parser-html-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-microsoft-module:2.9.0 (*)
|    +--- org.slf4j:jcl-over-slf4j:2.0.7 -> 1.7.36 (*)
|    +--- org.apache.tika:tika-parser-miscoffice-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    |    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    +--- org.apache.commons:commons-collections4:4.4
|    |    +--- org.apache.poi:poi:5.2.3 (*)
|    |    +--- commons-codec:commons-codec:1.16.0 -> 1.15
|    |    +--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8
|    |    |    +--- jakarta.xml.bind:jakarta.xml.bind-api:2.3.3
|    |    |    +--- org.glassfish.jaxb:txw2:2.3.8
|    |    |    +--- com.sun.istack:istack-commons-runtime:3.0.12
|    |    |    \--- com.sun.activation:jakarta.activation:1.2.2
|    |    \--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-news-module:2.9.0
|    |    +--- com.rometools:rome:2.1.0
|    |    |    +--- com.rometools:rome-utils:2.1.0
|    |    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    |    +--- org.jdom:jdom2:2.0.6.1
|    |    |    \--- org.slf4j:slf4j-api:2.0.6 -> 1.7.36
|    |    \--- org.slf4j:slf4j-api:2.0.7 -> 1.7.36
|    +--- org.apache.tika:tika-parser-ocr-module:2.9.0
|    |    +--- org.apache.commons:commons-lang3:3.13.0 -> 3.12.0
|    |    \--- org.apache.commons:commons-exec:1.3
|    +--- org.apache.tika:tika-parser-pdf-module:2.9.0
|    |    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    |    +--- org.apache.pdfbox:pdfbox:2.0.29 -> 2.0.27 (*)
|    |    +--- org.apache.pdfbox:pdfbox-tools:2.0.29
|    |    +--- org.apache.pdfbox:jempbox:1.8.17
|    |    +--- org.bouncycastle:bcmail-jdk18on:1.76 (*)
|    |    +--- org.bouncycastle:bcprov-jdk18on:1.76 -> 1.78
|    |    \--- org.glassfish.jaxb:jaxb-runtime:2.3.6 -> 2.3.8 (*)
|    +--- org.apache.tika:tika-parser-pkg-module:2.9.0
|    |    +--- org.tukaani:xz:1.9
|    |    +--- org.brotli:dec:0.1.2
|    |    +--- org.apache.tika:tika-parser-zip-commons:2.9.0 (*)
|    |    \--- com.github.junrar:junrar:7.5.5
|    |         \--- org.slf4j:slf4j-api:1.7.36
|    +--- org.apache.tika:tika-parser-text-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-webarchive-module:2.9.0
|    |    +--- org.netpreserve:jwarc:0.28.1
|    |    \--- org.apache.commons:commons-compress:1.23.0 -> 1.26.1 (*)
|    +--- org.apache.tika:tika-parser-xml-module:2.9.0 (*)
|    +--- org.apache.tika:tika-parser-xmp-commons:2.9.0 (*)
|    +--- org.gagravarr:vorbis-java-tika:0.8
|    \--- org.gagravarr:vorbis-java-core:0.8
+--- org.apache.logging.log4j:log4j-to-slf4j:2.23.1
|    +--- org.apache.logging.log4j:log4j-api:2.23.1 -> 2.17.2
|    \--- org.slf4j:slf4j-api:2.0.9 -> 1.7.36
+--- org.junit.jupiter:junit-jupiter-api:5.7.1 (*)
+--- org.junit-pioneer:junit-pioneer:0.3.3
|    \--- org.junit.jupiter:junit-jupiter-api:5.1.1 -> 5.7.1 (*)
+--- org.junit.jupiter:junit-jupiter-params:5.4.2
|    +--- org.apiguardian:apiguardian-api:1.0.0 -> 1.1.0
|    \--- org.junit.jupiter:junit-jupiter-api:5.4.2 -> 5.7.1 (*)
+--- org.mockito:mockito-core:2.+ -> 4.5.1
|    +--- net.bytebuddy:byte-buddy:1.12.9 -> 1.12.23
|    +--- net.bytebuddy:byte-buddy-agent:1.12.9 -> 1.12.23
|    \--- org.objenesis:objenesis:3.2
+--- org.assertj:assertj-core:3.13.2
+--- org.skyscreamer:jsonassert:1.5.0
|    \--- com.vaadin.external.google:android-json:0.0.20131108.vaadin1
+--- org.mockito:mockito-inline:2.+ -> 2.28.2
|    \--- org.mockito:mockito-core:2.28.2 -> 4.5.1 (*)
+--- project :common-test
|    +--- com.google.guava:guava:32.1.2-jre (*)
|    +--- com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0 (*)
|    +--- org.yaml:snakeyaml:2.0
|    +--- com.fasterxml.jackson.core:jackson-core:2.15.0
|    +--- org.freemarker:freemarker:2.3.31
|    +--- org.apache.httpcomponents:httpclient-osgi:4.5.14 (*)
|    \--- org.junit.jupiter:junit-jupiter-api:5.3.1 -> 5.7.1 (*)
+--- com.github.docker-java:docker-java-core:3.2.13
|    +--- com.github.docker-java:docker-java-api:3.2.13
|    |    +--- com.fasterxml.jackson.core:jackson-annotations:2.10.3 -> 2.13.5 (*)
|    |    \--- org.slf4j:slf4j-api:1.7.30 -> 1.7.36
|    +--- com.github.docker-java:docker-java-transport:3.2.13 -> 3.3.0
|    +--- org.slf4j:slf4j-api:1.7.30 -> 1.7.36
|    +--- commons-io:commons-io:2.6 -> 2.15.1
|    +--- org.apache.commons:commons-compress:1.21 -> 1.26.1 (*)
|    +--- org.apache.commons:commons-lang3:3.12.0
|    +--- com.fasterxml.jackson.core:jackson-databind:2.10.3 -> 2.13.5 (*)
|    +--- com.google.guava:guava:19.0 -> 32.1.2-jre (*)
|    \--- org.bouncycastle:bcpkix-jdk15on:1.64
|         \--- org.bouncycastle:bcprov-jdk15on:1.64
+--- com.github.docker-java:docker-java-transport-httpclient5:3.3.0
|    +--- com.github.docker-java:docker-java-transport:3.3.0
|    +--- org.apache.httpcomponents.client5:httpclient5:5.0.3 -> 5.1.4
|    |    +--- org.apache.httpcomponents.core5:httpcore5:5.1.5
|    |    +--- org.slf4j:slf4j-api:1.7.25 -> 1.7.36
|    |    \--- commons-codec:commons-codec:1.15
|    \--- net.java.dev.jna:jna:5.12.1
+--- org.junit.jupiter:junit-jupiter-engine:5.7.1
|    +--- org.apiguardian:apiguardian-api:1.1.0
|    +--- org.junit.platform:junit-platform-engine:1.7.1 -> 1.8.2
|    |    +--- org.junit:junit-bom:5.8.2 (*)
|    |    +--- org.opentest4j:opentest4j:1.2.0
|    |    \--- org.junit.platform:junit-platform-commons:1.8.2 (*)
|    \--- org.junit.jupiter:junit-jupiter-api:5.7.1 (*)
+--- ch.qos.logback:logback-core:1.2.13 (c)
+--- com.jayway.jsonpath:json-path:2.9.0 -> 2.7.0 (c)
+--- org.bouncycastle:bcutil-jdk18on:1.78 (c)
\--- org.apache.james:apache-mime4j-core:0.8.10 (c)

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
