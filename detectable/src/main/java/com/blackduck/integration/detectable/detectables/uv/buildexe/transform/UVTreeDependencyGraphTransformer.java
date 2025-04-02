package com.blackduck.integration.detectable.detectables.uv.buildexe.transform;

import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectables.cargo.transform.CargoDependencyGraphTransformer;
import com.blackduck.integration.detectable.detectables.uv.UVDetectorOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.regex.Pattern;

public class UVTreeDependencyGraphTransformer {

    private final ExternalIdFactory externalIdFactory;
    private static final Logger logger = LoggerFactory.getLogger(CargoDependencyGraphTransformer.class);
    private final List<String> prefixStrings = Arrays.asList("├── ","│   ","└── ");
    private int depth;

    public UVTreeDependencyGraphTransformer(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public DependencyGraph transform(List<String> uvTreeOutput, UVDetectorOptions detectorOptions) {
        DependencyGraph dependencyGraph = new BasicDependencyGraph();
        Deque<String> dependencyStack = new ArrayDeque<>();

        for(String line: uvTreeOutput) {
            parseLine(line.trim(), dependencyGraph, detectorOptions, dependencyStack);
        }

        return dependencyGraph;
    }

    private void parseLine(String line, DependencyGraph dependencyGraph, UVDetectorOptions detectorOptions, Deque<String> dependencyStack) {
        if(line.isEmpty()) {
            return;
        }

        int previousDepth = depth;
        String cleanedLine = findDepth(line);

        Dependency dependency = getDependency(cleanedLine, detectorOptions);

        if(depth > 0) {

        }
    }

    private String findDepth(String line) {
        depth = 0;
        String cleanedLine = line;
        for(String prefix: prefixStrings) {
            while(line.startsWith(prefix)) {
                depth++;
                cleanedLine = line.replaceFirst(Pattern.quote(prefix), "");
            }
        }

        return cleanedLine;
    }

    private Dependency getDependency(String line, UVDetectorOptions detectorOptions) {
        String[] parts = line.split(" ");
        if(parts.length < 2) {
            logger.warn("Unable to parse dependency from line: {}", line);
            return null;
        }

        if(parts.length == 2) {
            String dependencyName = parts[0];
            String dependencyVersion = parts[1].replace("v", "");

            ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, dependencyName, dependencyVersion);
            return new Dependency(dependencyName, dependencyVersion, externalId);
        } else if (parts.length == 3) {
            String dependencyName = parts[0];
            String dependencyVersion = parts[1].replace("v", "");

            String groupName = parts[2].split(":")[0].replace(")", "").trim();
            for(String group: detectorOptions.getExcludedDependencyGroups()) {
               if(group.toLowerCase().equals(groupName)) {
                   return null;
               }
            }

            ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, dependencyName, dependencyVersion);
            return new Dependency(dependencyName, dependencyVersion, externalId);
        } else {
            logger.warn("Unable to parse dependency from line: {}", line);
            return null;
        }
    }
}
