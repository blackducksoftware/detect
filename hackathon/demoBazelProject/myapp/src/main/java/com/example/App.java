package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Demo Bazel application.
 *
 * Runtime dependencies (all production, no test-scoped deps in this project):
 *   - com.google.guava:guava:32.1.2-jre          (ImmutableList / ImmutableMap)
 *   - com.fasterxml.jackson.core:jackson-databind:2.17.0
 *   - org.slf4j:slf4j-api:2.0.12
 *   - ch.qos.logback:logback-classic:1.5.3
 *
 * All four appear in the Black Duck BOM after an AI-assisted scan with
 *   --detect.bazel.target=//myapp:myapp
 *   --detect.bazel.mode=WORKSPACE
 *   --detect.bazel.dependency.sources=MAVEN_INSTALL,HTTP_ARCHIVE
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws Exception {
        log.info("Demo Bazel application starting...");

        // Guava — ImmutableList of production dependencies used by this project
        List<String> deps = ImmutableList.of(
            "com.google.guava:guava:32.1.2-jre",
            "com.fasterxml.jackson.core:jackson-databind:2.17.0",
            "org.slf4j:slf4j-api:2.0.12",
            "ch.qos.logback:logback-classic:1.5.3"
        );

        // Jackson — serialise to JSON for demo output
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> report = ImmutableMap.of(
            "project", "demo_bazel_project",
            "target",  "//myapp:myapp",
            "mode",    "WORKSPACE",
            "dependencies", deps
        );
        log.info("BOM preview: {}", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report));
    }
}

