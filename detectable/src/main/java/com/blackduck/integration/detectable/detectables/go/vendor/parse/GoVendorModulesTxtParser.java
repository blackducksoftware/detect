package com.blackduck.integration.detectable.detectables.go.vendor.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.blackduck.integration.bdio.graph.BasicDependencyGraph;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoVendorModulesTxtParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExternalIdFactory externalIdFactory;

    // Example line: "# github.com/pkg/errors v0.8.1"
    private static final Pattern MODULE_LINE_PATTERN = Pattern.compile("^#\\s+(\\S+)\\s+(\\S+)$");

    public GoVendorModulesTxtParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public DependencyGraph parseModulesTxt(String modulesTxtContents) {
        DependencyGraph graph = new BasicDependencyGraph();
        String[] lines = modulesTxtContents.split("\\r?\\n");
        for (String line : lines) {
            Matcher matcher = MODULE_LINE_PATTERN.matcher(line.trim());
            if (matcher.matches()) {
                String moduleName = matcher.group(1);
                String moduleVersion = matcher.group(2);
                ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.GOLANG, moduleName, moduleVersion);
                Dependency dependency = new Dependency(moduleName, moduleVersion, externalId);
                logger.trace(String.format("Parsed dependency: %s %s", moduleName, moduleVersion));
                graph.addDirectDependency(dependency);
            }
        }
        return graph;
    }
}