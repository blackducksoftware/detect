package com.blackduck.integration.detectable.detectables.maven.resolver;

import com.blackduck.integration.detectable.detectables.maven.resolver.model.*;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles conversion of POM XML dependency entries into Java dependency objects
 * and application of dependency management rules.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Resolve property placeholders in dependency coordinates</li>
 *   <li>Apply dependency management (fill in missing versions and scopes)</li>
 *   <li>Convert PomXmlDependency to JavaDependency including exclusion mapping</li>
 * </ul>
 *
 * <p>This class is stateless and thread-safe.
 */
class DependencyConverter {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final PropertiesResolverProvider propertiesResolverProvider;

    DependencyConverter(PropertiesResolverProvider propertiesResolverProvider) {
        this.propertiesResolverProvider = propertiesResolverProvider;
    }

    void resolveDependencyProperties(PartialMavenProject project) {
        // Create a complete map of properties for resolution.
        // Build a combined properties map that includes parent properties so
        // BOM coordinates that use parent/root properties (e.g. ${spring-cloud-dependencies.version})
        // can be resolved during the first pass.
        Map<String, String> allProps = new HashMap<>();
        try {
            Map<String, String> parentProps = propertiesResolverProvider.getParentProperties();
            if (parentProps != null) {
                allProps.putAll(parentProps);
            }
        } catch (Exception e) {
            logger.debug("Failed to include parent properties in resolution map: {}", e.getMessage());
        }
        if (project.getProperties() != null) {
            allProps.putAll(project.getProperties());
        }

        // Resolve properties for dependencies
        for (PomXmlDependency dep : project.getDependencies()) {
            dep.setGroupId(resolveProperties(dep.getGroupId(), allProps));
            dep.setArtifactId(resolveProperties(dep.getArtifactId(), allProps));
            dep.setVersion(resolveProperties(dep.getVersion(), allProps));
        }
        // Resolve properties for dependency management
        for (PomXmlDependency dep : project.getDependencyManagement()) {
            dep.setGroupId(resolveProperties(dep.getGroupId(), allProps));
            dep.setArtifactId(resolveProperties(dep.getArtifactId(), allProps));
            dep.setVersion(resolveProperties(dep.getVersion(), allProps));
        }
    }

    List<JavaDependency> applyDependencyManagement(PartialMavenProject project) {
        Map<String, PomXmlDependency> managedDependencies = project.getDependencyManagement().stream()
            .collect(Collectors.toMap(dep -> dep.getGroupId() + ":" + dep.getArtifactId(), dep -> dep));

        return project.getDependencies().stream()
            .map(dep -> {
                PomXmlDependency managed = managedDependencies.get(dep.getGroupId() + ":" + dep.getArtifactId());
                if (managed != null) {
                    if (isEmpty(dep.getVersion()) && !isEmpty(managed.getVersion())) {
                        dep.setVersion(managed.getVersion());
                    }
                    if (isEmpty(dep.getScope()) && !isEmpty(managed.getScope())) {
                        dep.setScope(managed.getScope());
                    }
                }
                return convertPomXmlDependencyToJavaDependency(dep);
            })
            .collect(Collectors.toList());
    }

    JavaDependency convertPomXmlDependencyToJavaDependency(PomXmlDependency pomDep) {
        List<JavaDependencyExclusion> exclusions = new ArrayList<>();
        if (pomDep.getExclusions() != null) {
            exclusions = pomDep.getExclusions().stream()
                .map(exclusion -> new JavaDependencyExclusion(exclusion.getGroupId(), exclusion.getArtifactId()))
                .collect(Collectors.toList());
        }

        JavaCoordinates coordinates = new JavaCoordinates(pomDep.getGroupId(), pomDep.getArtifactId(), pomDep.getVersion(), pomDep.getType());

        return new JavaDependency(
            coordinates,
            pomDep.getScope(),
            pomDep.getType(),
            pomDep.getClassifier(),
            exclusions
        );
    }

    String resolveProperties(String value, Map<String, String> properties) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        // Use StringSubstitutor for more robust property replacement.
        StringSubstitutor sub = new StringSubstitutor(properties);
        return sub.replace(value);
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
