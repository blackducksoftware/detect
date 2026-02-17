package com.blackduck.integration.detectable.detectables.meson.parse;

import java.io.BufferedReader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.google.gson.Gson;

public class MesonDependencyFileParser {
    private static final Forge GENERIC_FORGE = new Forge("/", "Generic");
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExternalIdFactory externalIdFactory;

    public MesonDependencyFileParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public DependencyGraph parseProjectDependencies(Gson gson, BufferedReader reader) {
        DependencyGraph graph = new BasicDependencyGraph();

        try {
            MesonDependency[] dependencies = gson.fromJson(reader, MesonDependency[].class);
            
            if (dependencies == null) {
                logger.warn("Failed to parse Meson dependencies - gson returned null");
                return graph;
            }
            
            logger.debug("Found {} Meson dependencies", dependencies.length);

            for (MesonDependency dep : dependencies) {
                if (!"internal".equals(dep.getType())
                        && StringUtils.isNotBlank(dep.getName())
                        && StringUtils.isNotBlank(dep.getVersion())) {

                    // https://github.com/blackducksoftware/bdio/blob/master/bdio2/src/main/java/com/blackducksoftware/bdio2/Bdio.java#L570
                    // https://github.com/blackducksoftware/integration-bdio/blob/master/src/main/java/com/blackduck/integration/bdio/model/Forge.java
                    // Create ExternalId with generic forge to produce format: pkg:generic/boost@1.83.0
                    ExternalId dependencyExternalId = externalIdFactory.createNameVersionExternalId(GENERIC_FORGE, dep.getName(), dep.getVersion());
                    dependencyExternalId.createBdioId();
                    Dependency dependency = new Dependency(dep.getName(), dep.getVersion(), dependencyExternalId, "");
                    logger.trace("Adding dependency: {}", dependency.getExternalId().toString());
                    graph.addDirectDependency(dependency);
                } else {
                    logger.debug("Skipping dependency - name: '{}', type: '{}', version: '{}'", dep.getName(), dep.getType(), dep.getVersion());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to parse Meson dependency JSON", e);
        }

        return graph;
    }

    // Could have extended com.blackduck.integration.util.NameVersion with a type field
    private static class MesonDependency {
        private String name;
        private String type;
        private String version;

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public String getVersion() {
            return version;
        }
    }
}
