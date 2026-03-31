package com.blackduck.integration.detectable.detectables.maven.resolver.pom;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
import com.blackduck.integration.detectable.detectables.maven.resolver.model.pomxml.*;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.property.PropertiesResolverProvider;
import com.blackduck.integration.detectable.detectables.maven.resolver.pom.property.PropertyResolver;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomParser {

    private static final Logger logger = LoggerFactory.getLogger(PomParser.class);
    private static final Pattern PROPERTIES_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

    /**
     * Parses a POM file and returns a PartialMavenProject with resolved properties.
     *
     * @param pomFilePath The file system path to the pom.xml file
     * @param pomFileContent The raw XML content of the pom.xml file
     * @param propertiesResolverProvider The properties resolver for handling Maven properties
     * @return A PartialMavenProject containing all parsed information
     * @throws PomParsingException if parsing fails
     */
    public PartialMavenProject parsePomFile(String pomFilePath, byte[] pomFileContent,
                                            PropertiesResolverProvider propertiesResolverProvider) throws PomParsingException {

        try {
            ParseContext ctx = new ParseContext(pomFilePath, pomFileContent);

            parseInitialModel(ctx);
            buildPreliminaryCoordinates(ctx);
            inheritCoordinatesFromParent(ctx);
            createProjectStarProperties(ctx);
            resolveAndReparseWithProperties(ctx, propertiesResolverProvider);

            return buildFinalResult(ctx);

        } catch (Exception e) {
            throw new PomParsingException("Failed to parse POM file: " + pomFilePath, e);
        }
    }

    private void parseInitialModel(ParseContext ctx) throws Exception {
        ctx.explicitProps = parseProperties(ctx.xmlContent);
        if (ctx.explicitProps.isEmpty()) {
            logger.debug("Couldn't parse pom.xml properties, using empty map");
            ctx.explicitProps = new HashMap<>();
        }

        ctx.xmlMapper = new XmlMapper();
        ctx.xmlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ctx.unresolvedPom = ctx.xmlMapper.readValue(ctx.xmlContent, PomXml.class);
    }

    private void buildPreliminaryCoordinates(ParseContext ctx) {
        ctx.preliminaryResult = new PartialMavenProject();
        ctx.preliminaryResult.setCoordinates(new JavaCoordinates(
            trimSpace(ctx.unresolvedPom.getGroupId()),
            trimSpace(ctx.unresolvedPom.getArtifactId()),
            trimSpace(ctx.unresolvedPom.getVersion()),
            trimSpace(ctx.unresolvedPom.getPackaging())
        ));

        ctx.parentPomInfo = new ParentPomInfo();
        PomXmlParent parent = ctx.unresolvedPom.getParent();
        if (parent != null) {
            ctx.parentPomInfo.setCoordinates(new JavaCoordinates(
                trimSpace(parent.getGroupId()),
                trimSpace(parent.getArtifactId()),
                trimSpace(parent.getVersion()),
                ""
            ));
        } else {
            ctx.parentPomInfo.setCoordinates(new JavaCoordinates());
        }
        ctx.preliminaryResult.setParentPomInfo(ctx.parentPomInfo);
    }

    private void inheritCoordinatesFromParent(ParseContext ctx) {
        JavaCoordinates coords = ctx.preliminaryResult.getCoordinates();
        JavaCoordinates parentCoords = ctx.parentPomInfo.getCoordinates();

        if (isEmpty(coords.getGroupId()) && isNotEmpty(parentCoords.getGroupId())) {
            coords.setGroupId(parentCoords.getGroupId());
        }
        if (isEmpty(coords.getVersion()) && isNotEmpty(parentCoords.getVersion())) {
            coords.setVersion(parentCoords.getVersion());
        }
    }

    private void createProjectStarProperties(ParseContext ctx) {
        ctx.projectStarProperties = new HashMap<>();
        JavaCoordinates finalCoords = ctx.preliminaryResult.getCoordinates();

        addCoordinateProperty(ctx.projectStarProperties, "groupId", finalCoords.getGroupId());
        addCoordinateProperty(ctx.projectStarProperties, "artifactId", finalCoords.getArtifactId());
        addCoordinateProperty(ctx.projectStarProperties, "version", finalCoords.getVersion());
    }

    private void addCoordinateProperty(Map<String, String> props, String name, String value) {
        if (isNotEmpty(value)) {
            props.put("project." + name, value);
            props.put("pom." + name, value);
        }
    }

    private void resolveAndReparseWithProperties(ParseContext ctx, PropertiesResolverProvider propertiesResolverProvider) {
        ctx.propertiesResolver = propertiesResolverProvider.newResolver(ctx.explicitProps, ctx.projectStarProperties);
        String resolvedPomFileContent = replaceProperties(ctx.xmlContent, ctx.propertiesResolver);
        ctx.parsedPom = tryParseResolvedPom(ctx.xmlMapper, resolvedPomFileContent, ctx.unresolvedPom);
    }

    private PomXml tryParseResolvedPom(XmlMapper xmlMapper, String resolvedContent, PomXml fallback) {
        try {
            return xmlMapper.readValue(resolvedContent, PomXml.class);
        } catch (Exception e) {
            logger.debug("Parsing file with resolved properties failed. Using original file without resolved properties.");
            return fallback;
        }
    }

    private PartialMavenProject buildFinalResult(ParseContext ctx) {
        PartialMavenProject result = new PartialMavenProject();
        result.setParentPomInfo(ctx.parentPomInfo);
        result.setProperties(ctx.propertiesResolver.getAllProperties());
        result.setCoordinates(ctx.preliminaryResult.getCoordinates());

        initializeEmptyCollections(result, ctx.parentPomInfo);
        setExpectedParentPath(ctx);
        processModules(result, ctx.parsedPom);
        processPlugins(result, ctx.parsedPom);
        processRepositories(result, ctx.parsedPom);
        processDependencies(result, ctx.unresolvedPom);
        processDependencyManagement(result, ctx.unresolvedPom);

        return result;
    }

    private void initializeEmptyCollections(PartialMavenProject result, ParentPomInfo parentPomInfo) {
        parentPomInfo.setDependencies(new ArrayList<>());
        parentPomInfo.setDependencyManagement(new ArrayList<>());

        result.setRepositories(new ArrayList<>());
        result.setDependencies(new ArrayList<>());
        result.setDependencyManagement(new ArrayList<>());
        result.setDependenciesWithShaded(new ArrayList<>());
        result.setDependencyManagementForShaded(new ArrayList<>());
        result.setModules(new ArrayList<>());
        result.setPlugins(new HashMap<>());
    }

    private void setExpectedParentPath(ParseContext ctx) {
        JavaCoordinates parentCoords = ctx.parentPomInfo.getCoordinates();
        String expectedParentPomPath = "";
        if (isNotEmpty(parentCoords.getGroupId()) && isNotEmpty(parentCoords.getArtifactId()) && isNotEmpty(parentCoords.getVersion())) {
            PomXmlParent parent = ctx.unresolvedPom.getParent();
            String relativePath = (parent != null) ? parent.getRelativePath() : null;
            expectedParentPomPath = calcExpectedParentPath(ctx.pomFilePath, relativePath);
        }
        ctx.parentPomInfo.setExpectedPath(expectedParentPomPath);
    }

    private void processModules(PartialMavenProject result, PomXml parsedPom) {
        if (parsedPom.getModules() == null) {
            return;
        }
        for (String module : parsedPom.getModules()) {
            result.getModules().add(trimSpace(module));
        }
    }

    private void processPlugins(PartialMavenProject result, PomXml parsedPom) {
        if (parsedPom.getBuild() == null || parsedPom.getBuild().getPlugins() == null) {
            return;
        }
        for (PomXmlPlugin plugin : parsedPom.getBuild().getPlugins()) {
            result.getPlugins().put(trimSpace(plugin.getArtifactId()), true);
        }
    }

    private void processRepositories(PartialMavenProject result, PomXml parsedPom) {
        if (parsedPom.getRepositories() == null) {
            return;
        }
        for (MavenRepositoryXml xmlRepo : parsedPom.getRepositories()) {
            result.getRepositories().add(xmlToBuildlessRepository(xmlRepo));
        }
    }

    private void processDependencies(PartialMavenProject result, PomXml unresolvedPom) {
        if (unresolvedPom.getDependencies() == null) {
            return;
        }
        for (PomXmlDependency dep : unresolvedPom.getDependencies()) {
            trimSpacesInPomXmlDep(dep);
            result.getDependencies().add(dep);
        }
    }

    private void processDependencyManagement(PartialMavenProject result, PomXml unresolvedPom) {
        if (unresolvedPom.getDependencyManagement() == null || unresolvedPom.getDependencyManagement().getDependencies() == null) {
            return;
        }
        for (PomXmlDependency depMgmt : unresolvedPom.getDependencyManagement().getDependencies()) {
            trimSpacesInPomXmlDep(depMgmt);
            result.getDependencyManagement().add(depMgmt);
        }
    }

    /**
     * Parses only the coordinates from a POM file without property or parent resolution.
     *
     * @param pomFileContent The raw XML content of the pom.xml file
     * @return PomXmlCoordinates containing the basic project coordinates
     * @throws PomParsingException if parsing fails
     */
    public PomXmlCoordinates parsePomCoordinates(byte[] pomFileContent) throws PomParsingException {
        try {
            String xmlContent = new String(pomFileContent, "UTF-8");
            XmlMapper xmlMapper = new XmlMapper();
            PomXmlCoordinates parsedCoordinates = xmlMapper.readValue(xmlContent, PomXmlCoordinates.class);

            // Trim all coordinate fields
            parsedCoordinates.setParentGroupId(trimSpace(parsedCoordinates.getParentGroupId()));
            parsedCoordinates.setParentArtifactId(trimSpace(parsedCoordinates.getParentArtifactId()));
            parsedCoordinates.setParentVersion(trimSpace(parsedCoordinates.getParentVersion()));

            parsedCoordinates.setGroupId(trimSpace(parsedCoordinates.getGroupId()));
            parsedCoordinates.setArtifactId(trimSpace(parsedCoordinates.getArtifactId()));
            parsedCoordinates.setVersion(trimSpace(parsedCoordinates.getVersion()));

            // Inherit from parent if empty
            if (isEmpty(parsedCoordinates.getGroupId()) && isNotEmpty(parsedCoordinates.getParentGroupId())) {
                logger.debug("Project groupId not found, overriding with parent pom groupId: {}", parsedCoordinates.getParentGroupId());
                parsedCoordinates.setGroupId(parsedCoordinates.getParentGroupId());
            }
            if (isEmpty(parsedCoordinates.getVersion()) && isNotEmpty(parsedCoordinates.getParentVersion())) {
                logger.debug("Project version not found, overriding with parent pom version: {}", parsedCoordinates.getParentVersion());
                parsedCoordinates.setVersion(parsedCoordinates.getParentVersion());
            }

            return parsedCoordinates;

        } catch (Exception e) {
            throw new PomParsingException("Failed to parse POM coordinates", e);
        }
    }

    // Private helper methods

    private Map<String, String> parseProperties(String xmlContent) {
        // This would be implemented to extract properties from <properties> section
        // For now, returning empty map as placeholder
        Map<String, String> properties = new HashMap<>();

        // Basic regex-based property extraction (simplified)
        Pattern propsPattern = Pattern.compile("<properties>(.*?)</properties>", Pattern.DOTALL);
        Matcher propsMatcher = propsPattern.matcher(xmlContent);

        if (propsMatcher.find()) {
            String propsSection = propsMatcher.group(1);
            Pattern propPattern = Pattern.compile("<([^/>]+)>([^<]+)</[^>]+>");
            Matcher propMatcher = propPattern.matcher(propsSection);

            while (propMatcher.find()) {
                String key = propMatcher.group(1);
                String value = propMatcher.group(2);
                properties.put(key, value);
            }
        }

        return properties;
    }

    private String replaceProperties(String content, PropertyResolver resolver) {
        StringSubstitutor substitutor = new StringSubstitutor(resolver.getAllProperties());
        return substitutor.replace(content);
    }

    private String calcExpectedParentPath(String childPomFilePath, String xmlParentPath) {
        // If the tag is present and empty we keep the empty value
        if (xmlParentPath != null && xmlParentPath.trim().isEmpty()) {
            return "";
        }

        // If the tag isn't present we use the default (../pom.xml)
        Path pomFileBaseDir = Paths.get(childPomFilePath).getParent();
        if (xmlParentPath == null) {
            return pomFileBaseDir.resolve("..").resolve("pom.xml").toString();
        }

        // Transform the relative path
        String expectedPath = xmlParentPath.trim();
        return pomFileBaseDir.resolve(expectedPath).toString();
    }

    private void trimSpacesInPomXmlDep(PomXmlDependency dep) {
        if (dep == null) return;

        dep.setFilePath(trimSpace(dep.getFilePath()));
        dep.setClassifier(trimSpace(dep.getClassifier()));
        dep.setVersion(trimSpace(dep.getVersion()));
        dep.setOptional(trimSpace(dep.getOptional()));
        dep.setType(trimSpace(dep.getType()));
        dep.setGroupId(trimSpace(dep.getGroupId()));
        dep.setArtifactId(trimSpace(dep.getArtifactId()));
        dep.setScope(trimSpace(dep.getScope()));

        if (dep.getExclusions() != null) {
            for (PomXmlDependencyExclusion exclusion : dep.getExclusions()) {
                exclusion.setGroupId(trimSpace(exclusion.getGroupId()));
                exclusion.setArtifactId(trimSpace(exclusion.getArtifactId()));
            }
        }
    }

    private JavaRepository xmlToBuildlessRepository(MavenRepositoryXml xmlRepository) {
        JavaRepository repo = new JavaRepository();
        repo.setId(trimSpace(xmlRepository.getId()));
        repo.setName(trimSpace(xmlRepository.getName()));
        repo.setUrl(trimSpace(xmlRepository.getUrl()));
        // Respect repository policy flags if present
        if (xmlRepository.getSnapshots() != null && xmlRepository.getSnapshots().getEnabled() != null) {
            String enabled = trimSpace(xmlRepository.getSnapshots().getEnabled());
            repo.setSnapshotsEnabled("true".equalsIgnoreCase(enabled));
        }
        if (xmlRepository.getReleases() != null && xmlRepository.getReleases().getEnabled() != null) {
            String enabled = trimSpace(xmlRepository.getReleases().getEnabled());
            repo.setReleasesEnabled("true".equalsIgnoreCase(enabled));
        }
        return repo;
    }

    private String trimSpace(String str) {
        return str != null ? str.trim() : "";
    }

    private boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    private boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Context object holding all parsing state for a single POM file parsing operation.
     * Reduces parameter passing and variable count in methods.
     */
    private static class ParseContext {
        final String pomFilePath;
        final String xmlContent;

        XmlMapper xmlMapper;
        Map<String, String> explicitProps;
        Map<String, String> projectStarProperties;
        PomXml unresolvedPom;
        PomXml parsedPom;
        PartialMavenProject preliminaryResult;
        ParentPomInfo parentPomInfo;
        PropertyResolver propertiesResolver;

        ParseContext(String pomFilePath, byte[] pomFileContent) throws Exception {
            this.pomFilePath = pomFilePath;
            this.xmlContent = new String(pomFileContent, "UTF-8");
        }
    }

    // Exception class
    public static class PomParsingException extends Exception {
        public PomParsingException(String message) {
            super(message);
        }

        public PomParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
