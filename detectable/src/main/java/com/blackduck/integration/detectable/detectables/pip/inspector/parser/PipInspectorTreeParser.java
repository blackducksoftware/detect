package com.blackduck.integration.detectable.detectables.pip.inspector.parser;

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
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectable.util.DependencyHistory;
import com.blackduck.integration.detectable.detectables.pip.inspector.model.NameVersionCodeLocation;

public class PipInspectorTreeParser {
    private final Logger logger = LoggerFactory.getLogger(PipInspectorTreeParser.class);

    public static final String SEPARATOR = "==";
    public static final String UNKNOWN_PROJECT_NAME = "n?";
    public static final String UNKNOWN_PROJECT_VERSION = "v?";
    public static final String UNKNOWN_REQUIREMENTS_PREFIX = "r?";
    public static final String UNPARSEABLE_REQUIREMENTS_PREFIX = "p?";
    public static final String UNKNOWN_PACKAGE_PREFIX = "--";
    public static final String INDENTATION = "    ";

    private final ExternalIdFactory externalIdFactory;

    public PipInspectorTreeParser(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public Optional<NameVersionCodeLocation> parse(List<String> pipInspectorOutputAsList, String sourcePath, boolean projectNameWasGiven) {
        NameVersionCodeLocation parseResult = null;

        DependencyGraph graph = new BasicDependencyGraph();
        DependencyHistory history = new DependencyHistory();
        Dependency project = null;
        int unResolvedPackageCount = 0;
        for (String line : pipInspectorOutputAsList) {
            String trimmedLine = StringUtils.trimToEmpty(line);
            if (StringUtils.isEmpty(trimmedLine)
                || !trimmedLine.contains(SEPARATOR)
                || trimmedLine.startsWith(UNKNOWN_REQUIREMENTS_PREFIX)
                || trimmedLine.startsWith(UNPARSEABLE_REQUIREMENTS_PREFIX)
                || trimmedLine.startsWith(UNKNOWN_PACKAGE_PREFIX)
                || trimmedLine.startsWith(UNKNOWN_PROJECT_NAME) && projectNameWasGiven
            ) {
                boolean wasUnresolved = parseErrorsFromLine(trimmedLine);
                if (wasUnresolved) {
                    unResolvedPackageCount++;
                }
                continue;
            }
            Dependency currentDependency = parseDependencyFromLine(trimmedLine, sourcePath);
            adjustForIndentLevel(history, line);
            project = addDependencyToGraph(graph, history, project, currentDependency);
            history.add(currentDependency);
        }

        adviseIfUnresolvedPackages(unResolvedPackageCount);

        if (project != null && unResolvedPackageCount == 0) {
            CodeLocation codeLocation = new CodeLocation(graph, project.getExternalId());
            parseResult = new NameVersionCodeLocation(project.getName(), project.getVersion(), codeLocation);
        }

        return Optional.ofNullable(parseResult);
    }

    private void adjustForIndentLevel(DependencyHistory history, String line) {
        int lineLevel = getLineLevel(line);
        try {
            history.clearDependenciesDeeperThan(lineLevel);
        } catch (IllegalStateException e) {
            logger.warn(String.format("Problem parsing line '%s': %s", line, e.getMessage()));
        }
    }

    private Dependency addDependencyToGraph(DependencyGraph graph, DependencyHistory history, Dependency project, Dependency currentDependency) {
        if (project == null) {
            project = currentDependency;
        } else if (project.equals(history.getLastDependency())) {
            graph.addChildToRoot(currentDependency);
        } else if (history.isEmpty()) {
            graph.addChildToRoot(currentDependency);
        } else {
            graph.addChildWithParents(currentDependency, history.getLastDependency());
        }
        return project;
    }

    private void adviseIfUnresolvedPackages(int unResolvedPackageCount) {
        if (unResolvedPackageCount > 0) {
            logger.error(
                "The Pip inspector was unable to resolve {} packages. Please check to be sure all packages have been installed with 'pip install'. Refer to 'Python support' in the Detect documentation for important information regarding Python projects.",
                unResolvedPackageCount
            );
        }
    }

    private boolean parseErrorsFromLine(String trimmedLine) {
        boolean unResolvedPackage = false;
        if (trimmedLine.startsWith(UNKNOWN_REQUIREMENTS_PREFIX)) {
            logger.error(String.format("Pip inspector could not find requirements file @ %s", trimmedLine.substring(UNKNOWN_REQUIREMENTS_PREFIX.length())));
        }

        if (trimmedLine.startsWith(UNPARSEABLE_REQUIREMENTS_PREFIX)) {
            logger.error(String.format("Pip inspector could not parse requirements file @ %s", trimmedLine.substring(UNPARSEABLE_REQUIREMENTS_PREFIX.length())));
        }

        if (trimmedLine.startsWith(UNKNOWN_PACKAGE_PREFIX)) {
            logger.error(String.format("Pip inspector could not resolve the package: %s", trimmedLine.substring(UNKNOWN_PACKAGE_PREFIX.length())));
            unResolvedPackage = true;
        }

        if (trimmedLine.startsWith(UNKNOWN_PROJECT_NAME)) {
            logger.error("Pip inspector could not resolve the project");
            unResolvedPackage = true;
        }
        return unResolvedPackage;
    }

    private Dependency parseDependencyFromLine(String line, String sourcePath) {
        String[] segments = line.split(SEPARATOR);

        String name = segments[0].trim();
        String version = segments[1].trim();
        ExternalId externalId = externalIdFactory.createNameVersionExternalId(Forge.PYPI, name, version);

        if (name.equals(UNKNOWN_PROJECT_NAME) || version.equals(
            UNKNOWN_PROJECT_VERSION)) {    //TODO: Pip needs some love. It shouldn't have to do this here but the change seems non-trivial. A code location with no external id should be created as such.
            externalId = externalIdFactory.createPathExternalId(Forge.PYPI, sourcePath);
        }

        name = name.equals(UNKNOWN_PROJECT_NAME) ? "" : name;
        version = version.equals(UNKNOWN_PROJECT_VERSION) ? "" : version;

        return new Dependency(name, version, externalId);
    }

    // TODO: This should be more strict. Currently successfully parses a graph with an indentation of three spaces instead of 4.
    private int getLineLevel(String line) {
        int level = 0;
        String tmpLine = line;
        while (tmpLine.startsWith(INDENTATION)) {
            tmpLine = tmpLine.replaceFirst(INDENTATION, "");
            level++;
        }

        return level;
    }
}
