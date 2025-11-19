package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PomParser {

    private static final Logger logger = LoggerFactory.getLogger(com.blackduck.integration.detectable.detectables.maven.resolver.PomParser.class);
    private static final Pattern PROPERTIES_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

    /**
     * Parses a POM file and returns a PartialMavenProject with resolved properties.
     *
     * @param pomFilePath The file system path to the pom.xml file
     * @param pomFileContent The raw XML content of the pom.xml file
     * @param propertiesResolverProvider The properties resolver for handling Maven properties
     * @return A PartialMavenProject containing all parsed information
     * @throws com.blackduck.integration.detectable.detectables.maven.resolver.PomParser.PomParsingException if parsing fails
     */
    public PartialMavenProject parsePomFile(String pomFilePath, byte[] pomFileContent,
                                            PropertiesResolverProvider propertiesResolverProvider) throws com.blackduck.integration.detectable.detectables.maven.resolver.PomParser.PomParsingException {

        try {
            String xmlContent = new String(pomFileContent, "UTF-8");

            // Step 1: Parse properties from the XML
            Map<String, String> explicitProps = parseProperties(xmlContent);
            Map<String, String> implicitProps = new HashMap<>(); // Would be populated by properties parser

            if (explicitProps.isEmpty()) {
                logger.debug("Couldn't parse pom.xml properties, using empty map");
                explicitProps = new HashMap<>();
            }

            // Step 2: Parse XML without property resolution first
            XmlMapper xmlMapper = new XmlMapper();

            //workaround for now
            xmlMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            PomXml unresolvedParsedPomFile = xmlMapper.readValue(xmlContent, PomXml.class);

            // Step 3: Create properties resolver
            PropertyResolver propertiesResolver = propertiesResolverProvider.newResolver(explicitProps, implicitProps);

            // Step 4: Replace properties in the complete POM XML file
            String resolvedPomFileContent = replaceProperties(xmlContent, propertiesResolver);

            // Step 5: Parse POM file with properties resolved
            PomXml parsedPomFile;
            try {
                parsedPomFile = xmlMapper.readValue(resolvedPomFileContent, PomXml.class);
            } catch (Exception e) {
                logger.debug("Parsing file with resolved properties failed. Using original file without resolved properties.");
                parsedPomFile = unresolvedParsedPomFile;
            }

            // Step 6: Retrieve all properties that were defined in this pom file (including parents)
            Map<String, String> resolvedProperties = new HashMap<>();

            // Add properties that start resolved (proj.*, jvm user props)
            resolvedProperties.putAll(propertiesResolver.getResolvedProperties());

            // Add properties that need resolution
            for (Map.Entry<String, String> entry : explicitProps.entrySet()) {
                String propName = entry.getKey();
                String propVal = entry.getValue();
                String resolvedVal = propertiesResolver.resolve(propName);
                if (resolvedVal != null) {
                    propVal = resolvedVal;
                }
                resolvedProperties.put(propName, propVal);
            }

            for (Map.Entry<String, String> entry : propertiesResolverProvider.getParentProperties().entrySet()) {
                String propName = entry.getKey();
                String propVal = entry.getValue();
                String resolvedVal = propertiesResolver.resolve(propName);
                if (resolvedVal != null) {
                    propVal = resolvedVal;
                }
                resolvedProperties.put(propName, propVal);
            }

            // Step 7: Build resulting POM model
            PartialMavenProject result = new PartialMavenProject();

            // Set parent POM info
            ParentPomInfo parentPomInfo = new ParentPomInfo();
            PomXmlParent parent = parsedPomFile.getParent();
            if (parent != null) {
                parentPomInfo.setCoordinates(new JavaCoordinates(
                    trimSpace(parent.getGroupId()),
                    trimSpace(parent.getArtifactId()),
                    trimSpace(parent.getVersion()),
                    ""
                ));
            } else {
                parentPomInfo.setCoordinates(new JavaCoordinates());
            }
            parentPomInfo.setDependencies(new ArrayList<>());
            parentPomInfo.setDependencyManagement(new ArrayList<>());

            result.setParentPomInfo(parentPomInfo);
            result.setProperties(resolvedProperties);

            // Set project coordinates
            result.setCoordinates(new JavaCoordinates(
                    trimSpace(parsedPomFile.getGroupId()),
                    trimSpace(parsedPomFile.getArtifactId()),
                    trimSpace(parsedPomFile.getVersion()),
                    trimSpace(parsedPomFile.getPackaging())
            ));

            result.setRepositories(new ArrayList<>());
            result.setDependencies(new ArrayList<>());
            result.setDependencyManagement(new ArrayList<>());
            result.setDependenciesWithShaded(new ArrayList<>());
            result.setDependencyManagementForShaded(new ArrayList<>());
            result.setModules(new ArrayList<>());
            result.setPlugins(new HashMap<>());

            // Calculate expected parent POM path
            String expectedParentPomPath = "";
            JavaCoordinates parentCoords = parentPomInfo.getCoordinates();
            if (isNotEmpty(parentCoords.getGroupId()) && isNotEmpty(parentCoords.getArtifactId()) && isNotEmpty(parentCoords.getVersion())) {
                String relativePath = (parent != null) ? parent.getRelativePath() : null;
                expectedParentPomPath = calcExpectedParentPath(pomFilePath, relativePath);
            }
            parentPomInfo.setExpectedPath(expectedParentPomPath);

            // Inherit GroupId or Version from parent if empty
            if (isEmpty(result.getCoordinates().getGroupId()) && isNotEmpty(parentCoords.getGroupId())) {
                logger.debug("Project groupId not found, overriding with parent pom groupId: {}", parentCoords.getGroupId());
                result.getCoordinates().setGroupId(parentCoords.getGroupId());
            }
            if (isEmpty(result.getCoordinates().getVersion()) && isNotEmpty(parentCoords.getVersion())) {
                logger.debug("Project version not found, overriding with parent pom version: {}", parentCoords.getVersion());
                result.getCoordinates().setVersion(parentCoords.getVersion());
            }

            // Process modules
            if (parsedPomFile.getModules() != null) {
                for (String module : parsedPomFile.getModules()) {
                    result.getModules().add(trimSpace(module));
                }
            }

            // Process plugins
            if (parsedPomFile.getBuild() != null && parsedPomFile.getBuild().getPlugins() != null) {
                for (PomXmlPlugin plugin : parsedPomFile.getBuild().getPlugins()) {
                    result.getPlugins().put(trimSpace(plugin.getArtifactId()), true);
                }
            }

            // Process repositories
            if (parsedPomFile.getRepositories() != null) {
                for (MavenRepositoryXml xmlRepo : parsedPomFile.getRepositories()) {
                    result.getRepositories().add(xmlToBuildlessRepository(xmlRepo));
                }
            }

            // Process dependencies (store without resolving properties for later processing)
            if (unresolvedParsedPomFile.getDependencies() != null) {
                for (PomXmlDependency dep : unresolvedParsedPomFile.getDependencies()) {
                    trimSpacesInPomXmlDep(dep);
                    result.getDependencies().add(dep);
                }
            }

            // Process dependency management
            if (unresolvedParsedPomFile.getDependencyManagement() != null && unresolvedParsedPomFile.getDependencyManagement().getDependencies() != null) {
                for (PomXmlDependency depMgmt : unresolvedParsedPomFile.getDependencyManagement().getDependencies()) {
                    trimSpacesInPomXmlDep(depMgmt);
                    result.getDependencyManagement().add(depMgmt);
                }
            }

            return result;

        } catch (Exception e) {
            throw new com.blackduck.integration.detectable.detectables.maven.resolver.PomParser.PomParsingException("Failed to parse POM file: " + pomFilePath, e);
        }
    }

    /**
     * Parses only the coordinates from a POM file without property or parent resolution.
     *
     * @param pomFileContent The raw XML content of the pom.xml file
     * @return PomXmlCoordinates containing the basic project coordinates
     * @throws com.blackduck.integration.detectable.detectables.maven.resolver.PomParser.PomParsingException if parsing fails
     */
    public PomXmlCoordinates parsePomCoordinates(byte[] pomFileContent) throws com.blackduck.integration.detectable.detectables.maven.resolver.PomParser.PomParsingException {
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
            throw new com.blackduck.integration.detectable.detectables.maven.resolver.PomParser.PomParsingException("Failed to parse POM coordinates", e);
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
