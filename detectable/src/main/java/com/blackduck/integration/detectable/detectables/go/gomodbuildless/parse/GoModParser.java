package com.blackduck.integration.detectable.detectables.go.gomodbuildless.parse;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;

public class GoModParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExternalIdFactory externalIdFactory;

    public GoModParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public DependencyGraph parseGoModFile(List<String> goModContents) {
        DependencyGraph graph = new BasicDependencyGraph();
        String moduleName = null;
        ExternalId parentModuleExternalId = null;
        Dependency parentModuleDependency = null;
        for (String line: goModContents) {
            line = line.trim();
            if (shouldIncludeLine(line)) {
                if (line.startsWith("module")) {
                    moduleName = line.replace("module", "").trim();
                    parentModuleExternalId = externalIdFactory.createNameVersionExternalId(Forge.GOLANG, moduleName, null);
                    parentModuleDependency = new Dependency(moduleName, null, parentModuleExternalId);
                    graph.addChildToRoot(parentModuleDependency);
                } else {
                    if (line.endsWith("// indirect")) {
                        String transitiveDependency = line.replace("// indirect", "").trim();
                        Optional<Dependency> dependency = parseLineToDependency(transitiveDependency);
                        graph.addChildWithParent(dependency.orElse(null), parentModuleDependency);
                    } else {
                        Optional<Dependency> dependency = parseLineToDependency(line);
                        dependency.ifPresent(graph::addChildToRoot);
                    }
                }
            }   
        }

        return graph;
    }

    private Optional<Dependency> parseLineToDependency(String line) {
        String[] parts = StringUtils.split(line, " ");
        if (parts.length < 2) {
            logger.error("Failed to parse go.mod line. Excluding from BOM. Line: {}", line);
            return Optional.empty();
        }
        String updatedVersion = null;
        if(parts[1].endsWith("+incompatible")) {
            updatedVersion = parts[1].replace("+incompatible", "");
        } else if (parts[1].endsWith("%2Bincompatible")) {
            updatedVersion = parts[1].replace("%2Bincompatible", "");
        } else {
            updatedVersion = parts[1];
        }
        ExternalId dependencyExternalId = externalIdFactory.createNameVersionExternalId(Forge.GOLANG, parts[0], updatedVersion);
        Dependency dependency = new Dependency(parts[0], updatedVersion, dependencyExternalId);
        return Optional.of(dependency);
    }

    private boolean shouldIncludeLine(String line) {
        if (line.startsWith("go")) {
            return false;
        } else if (line.startsWith("require")) {
            return false;
        } else if (line.startsWith("replace")) {
            return false;
        } else if (line.startsWith("exclude")) {
            return false;
        } else if (line.startsWith(")")) {
            return false;
        }
        return StringUtils.isNotBlank(line) && !line.startsWith("//");
    }

}
