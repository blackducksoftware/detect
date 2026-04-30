package com.blackduck.integration.detectable.detectables.maven.resolver.pom.property;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Enables the creation of PropertyResolver objects.
 *
 * <p><strong>Thread Safety:</strong> {@link #parentProperties} is stored in a
 * {@link ThreadLocal} so that concurrent threads (e.g., parallel module processing) each
 * maintain their own independent parent-property context without racing on a shared field.
 * All other fields are read-only after construction and therefore thread-safe.
 */
public class PropertiesResolverProvider {
    private Map<String, String> userJvmProps;
    /**
     * Per-thread parent properties.
     *
     * <p>Before this change, {@code parentProperties} was a plain instance field. When two
     * threads called {@link #setParentProperties} concurrently, one thread's write could be
     * observed by the other, producing incorrect property resolution. With {@link ThreadLocal},
     * each thread's parent context is fully isolated.
     */
    private final ThreadLocal<Map<String, String>> parentProperties =
        ThreadLocal.withInitial(HashMap::new);
    private Map<String, String> jvmProps;
    private Function<String, String> envPropertiesProvider;

    /**
     * Creates a new PropertiesResolverProvider.
     *
     * @param userJvmProps User defined properties via the jvm "-D" option
     * @param envPropertiesProvider Function that provides properties set by the environment
     */
    public PropertiesResolverProvider(Map<String, String> userJvmProps,
                                      Function<String, String> envPropertiesProvider) {
        this.userJvmProps = userJvmProps != null ? new HashMap<>(userJvmProps) : new HashMap<>();
        this.jvmProps = getJavaCalcProperties();
        this.envPropertiesProvider = envPropertiesProvider;
        // parentProperties ThreadLocal is self-initializing — no explicit initialization needed.
    }

    /**
     * Sets the parent properties for property resolution on the <em>current thread</em>.
     *
     * <p>Each thread maintains its own independent parent-property context so that concurrent
     * module-processing threads do not interfere with each other's resolution.
     */
    public void setParentProperties(Map<String, String> parentProps) {
        parentProperties.set(parentProps != null ? new HashMap<>(parentProps) : new HashMap<>());
    }

    /**
     * Creates a new PropertyResolver with the given properties.
     *
     * @param pomProps Properties sourced from the pom.xml file
     * @param projectStarProperties Well-known properties that don't need resolution
     * @return A new PropertyResolver instance
     */
    public PropertyResolver newResolver(Map<String, String> pomProps, Map<String, String> projectStarProperties) {
        // Clone to avoid mutations
        Map<String, String> clonedPomProps = pomProps != null ? new HashMap<>(pomProps) : new HashMap<>();
        Map<String, String> currentParentProps = parentProperties.get(); // thread-local read

        Map<String, String> resolvedProps = new HashMap<>();

        // Add properties in priority order (lowest to highest)

        // 1. Java system properties (lowest priority for collisions with pom props)
        for (Map.Entry<String, String> entry : jvmProps.entrySet()) {
            if (!clonedPomProps.containsKey(entry.getKey())) {
                resolvedProps.put(entry.getKey(), entry.getValue());
            }
        }

        // 2. Parent properties (skip on pom props collision)
        for (Map.Entry<String, String> entry : currentParentProps.entrySet()) {
            if (!clonedPomProps.containsKey(entry.getKey())) {
                resolvedProps.put(entry.getKey(), entry.getValue());
            }
        }

        // 3. User JVM properties (higher priority)
        resolvedProps.putAll(userJvmProps);

        // 4. Project star properties (highest priority)
        if (projectStarProperties != null) {
            resolvedProps.putAll(projectStarProperties);
        }

        return new PropertyResolver(resolvedProps, clonedPomProps, currentParentProps, envPropertiesProvider);
    }

    /**
     * Gets calculated Java system properties.
     * This is a simplified version - in a full implementation, this would include
     * properties like ${java.version}, ${os.name}, etc.
     */
    private Map<String, String> getJavaCalcProperties() {
        Map<String, String> props = new HashMap<>();

        // Add some basic Java system properties
        props.put("java.version", System.getProperty("java.version", ""));
        props.put("java.home", System.getProperty("java.home", ""));
        props.put("os.name", System.getProperty("os.name", ""));
        props.put("os.arch", System.getProperty("os.arch", ""));
        props.put("user.name", System.getProperty("user.name", ""));
        props.put("user.home", System.getProperty("user.home", ""));
        props.put("file.separator", System.getProperty("file.separator", "/"));

        // Maven-specific calculated properties would be added here
        // like ${maven.build.timestamp}, ${project.basedir}, etc.

        return props;
    }

    // Getters
    public Map<String, String> getParentProperties() { return parentProperties.get(); }
    public Map<String, String> getUserJvmProps() { return userJvmProps; }
}
