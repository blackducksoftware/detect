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

            // Step 3: Create a preliminary model to determine final coordinates
            PartialMavenProject preliminaryResult = new PartialMavenProject();
            preliminaryResult.setCoordinates(new JavaCoordinates(
                trimSpace(unresolvedParsedPomFile.getGroupId()),
                trimSpace(unresolvedParsedPomFile.getArtifactId()),
                trimSpace(unresolvedParsedPomFile.getVersion()),
                trimSpace(unresolvedParsedPomFile.getPackaging())
            ));

            ParentPomInfo parentPomInfo = new ParentPomInfo();
            PomXmlParent parent = unresolvedParsedPomFile.getParent();
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
            preliminaryResult.setParentPomInfo(parentPomInfo);
            JavaCoordinates parentCoords = parentPomInfo.getCoordinates();

            // Inherit GroupId or Version from parent if empty
            if (isEmpty(preliminaryResult.getCoordinates().getGroupId()) && isNotEmpty(parentCoords.getGroupId())) {
                preliminaryResult.getCoordinates().setGroupId(parentCoords.getGroupId());
            }
            if (isEmpty(preliminaryResult.getCoordinates().getVersion()) && isNotEmpty(parentCoords.getVersion())) {
                preliminaryResult.getCoordinates().setVersion(parentCoords.getVersion());
            }

            // Step 4: Create project star properties now that coordinates are finalized
            Map<String, String> projectStarProperties = new HashMap<>();
            JavaCoordinates finalCoords = preliminaryResult.getCoordinates();
            if (isNotEmpty(finalCoords.getGroupId())) {
                projectStarProperties.put("project.groupId", finalCoords.getGroupId());
                projectStarProperties.put("pom.groupId", finalCoords.getGroupId());
            }
            if (isNotEmpty(finalCoords.getArtifactId())) {
                projectStarProperties.put("project.artifactId", finalCoords.getArtifactId());
                projectStarProperties.put("pom.artifactId", finalCoords.getArtifactId());
            }
            if (isNotEmpty(finalCoords.getVersion())) {
                projectStarProperties.put("project.version", finalCoords.getVersion());
                projectStarProperties.put("pom.version", finalCoords.getVersion());
            }

            // Step 5: Create the final, fully-contextualized property resolver
            PropertyResolver propertiesResolver = propertiesResolverProvider.newResolver(explicitProps, projectStarProperties);

            // Step 6: Replace properties in the complete POM XML file
            String resolvedPomFileContent = replaceProperties(xmlContent, propertiesResolver);

            // Step 7: Parse POM file with properties resolved
            PomXml parsedPomFile;
            try {
                parsedPomFile = xmlMapper.readValue(resolvedPomFileContent, PomXml.class);
            } catch (Exception e) {
                logger.debug("Parsing file with resolved properties failed. Using original file without resolved properties.");
                parsedPomFile = unresolvedParsedPomFile;
            }

            // Step 8: Build the final resulting POM model
            PartialMavenProject result = new PartialMavenProject();
            result.setParentPomInfo(parentPomInfo);
            result.setProperties(propertiesResolver.getAllProperties());
            result.setCoordinates(finalCoords); // Use the already finalized coordinates

            parentPomInfo.setDependencies(new ArrayList<>());
            parentPomInfo.setDependencyManagement(new ArrayList<>());

            result.setRepositories(new ArrayList<>());
            result.setDependencies(new ArrayList<>());
            result.setDependencyManagement(new ArrayList<>());
            result.setDependenciesWithShaded(new ArrayList<>());
            result.setDependencyManagementForShaded(new ArrayList<>());
            result.setModules(new ArrayList<>());
            result.setPlugins(new HashMap<>());

            // Calculate expected parent POM path
            String expectedParentPomPath = "";
            if (isNotEmpty(parentCoords.getGroupId()) && isNotEmpty(parentCoords.getArtifactId()) && isNotEmpty(parentCoords.getVersion())) {
                String relativePath = (parent != null) ? parent.getRelativePath() : null;
                expectedParentPomPath = calcExpectedParentPath(pomFilePath, relativePath);
            }
            parentPomInfo.setExpectedPath(expectedParentPomPath);

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
