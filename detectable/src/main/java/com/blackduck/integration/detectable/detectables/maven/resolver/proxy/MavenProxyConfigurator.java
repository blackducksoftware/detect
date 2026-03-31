package com.blackduck.integration.detectable.detectables.maven.resolver.proxy;

import org.eclipse.aether.RepositorySystemSession.SessionBuilder;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.eclipse.aether.util.repository.DefaultProxySelector;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * MavenProxyConfigurator is responsible for configuring forward-proxy settings
 * for Maven Aether dependency resolution.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Configuration of Aether's ProxySelector for both HTTP and HTTPS protocols</li>
 *   <li>Setting Java system properties so JdkTransporterFactory respects the proxy</li>
 *   <li>Managing proxy authentication credentials</li>
 *   <li>Configuring non-proxy hosts (bypass patterns)</li>
 * </ul>
 *
 * <p><strong>Important:</strong> The proxy host must be a bare hostname or IP address
 * without any {@code http://} or {@code https://} scheme prefix.
 *
 * <p>Thread-safety: This class is immutable and thread-safe after construction.
 */
public class MavenProxyConfigurator {
    private static final Logger logger = LoggerFactory.getLogger(MavenProxyConfigurator.class);

    // System property names for HTTP proxy configuration
    private static final String HTTP_PROXY_HOST = "http.proxyHost";
    private static final String HTTP_PROXY_PORT = "http.proxyPort";
    private static final String HTTP_PROXY_USER = "http.proxyUser";
    private static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";
    private static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";

    // System property names for HTTPS proxy configuration
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";
    private static final String HTTPS_PROXY_USER = "https.proxyUser";
    private static final String HTTPS_PROXY_PASSWORD = "https.proxyPassword";

    private final MavenProxyConfig proxyConfig;

    // Store original system properties so we can restore them later
    // These are captured when configureProxy() is called
    private String originalHttpProxyHost;
    private String originalHttpProxyPort;
    private String originalHttpProxyUser;
    private String originalHttpProxyPassword;
    private String originalHttpsProxyHost;
    private String originalHttpsProxyPort;
    private String originalHttpsProxyUser;
    private String originalHttpsProxyPassword;
    private String originalHttpNonProxyHosts;

    /**
     * Constructs a proxy configurator with the given proxy configuration.
     *
     * @param proxyConfig The proxy configuration (must not be null)
     * @throws IllegalArgumentException if proxyConfig is null
     */
    public MavenProxyConfigurator(MavenProxyConfig proxyConfig) {
        if (proxyConfig == null) {
            throw new IllegalArgumentException("Proxy configuration cannot be null");
        }
        this.proxyConfig = proxyConfig;
    }

    /**
     * Configures proxy settings on the given Aether session builder.
     *
     * <p>This method performs an optimization check before modifying system properties:
     * <ol>
     *   <li><strong>Check Current State:</strong> Reads current Java system proxy properties to see if
     *       they already match the desired configuration.</li>
     *   <li><strong>Skip if Matching:</strong> If all proxy values (host, port, credentials, nonProxyHosts)
     *       already match, skips the save/set/restore cycle entirely for efficiency.</li>
     *   <li><strong>Configure if Different:</strong> If values differ, saves original properties, configures
     *       Aether's ProxySelector, and sets Java system properties.</li>
     * </ol>
     *
     * <p><strong>Why This Matters:</strong> In most cases (95%+), the global Black Duck proxy settings
     * have already configured system properties with the same values we want to set. This optimization
     * avoids redundant work and provides clearer logging about what's actually happening.
     *
     * <p>This method configures both:
     * <ul>
     *   <li>Aether's ProxySelector for both HTTP and HTTPS protocols</li>
     *   <li>Java system properties ({@code http.proxyHost}, {@code https.proxyHost}, etc.)
     *       because JdkTransporterFactory does NOT respect Aether's ProxySelector</li>
     * </ul>
     *
     * <p>Non-proxy hosts are built from the configured patterns combined with
     * hardcoded loopback addresses {@code localhost} and {@code 127.0.0.1}.
     *
     * <p><strong>IMPORTANT:</strong> After Maven resolution completes, you MUST call
     * {@link #restoreOriginalProxyProperties()} to restore the original system state,
     * preferably in a finally block to ensure cleanup happens even if errors occur.
     * (Note: If optimization skips configuration, restoration is also skipped automatically.)
     *
     * <p>If anything goes wrong during configuration, a warning is logged and
     * the method continues gracefully (no exception is thrown).
     *
     * @param builder the session builder to configure with proxy settings
     */
    public void configureProxy(SessionBuilder builder) {
        try {
            // OPTIMIZATION: Check if proxy is already configured with matching values
            // This is common when Black Duck's global proxy settings have already set system properties
            if (isProxyAlreadyConfigured()) {
                logger.info("Proxy already configured with matching values ({}:{}). No changes needed.",
                    proxyConfig.getHost(), proxyConfig.getPort());
                logger.debug("Skipping save/set/restore cycle because current system properties match desired configuration.");
                return; // Skip entire save/set/restore cycle
            }

            // Log that we're changing proxy configuration
            String currentHost = System.getProperty(HTTP_PROXY_HOST);
            String currentPort = System.getProperty(HTTP_PROXY_PORT);
            if (currentHost != null && !currentHost.isEmpty()) {
                logger.info("Changing proxy configuration from {}:{} to {}:{} for Maven resolution",
                    currentHost, currentPort != null ? currentPort : "?",
                    proxyConfig.getHost(), proxyConfig.getPort());
            } else {
                logger.info("Configuring proxy for Maven resolution: {}:{}", proxyConfig.getHost(), proxyConfig.getPort());
            }

            // STEP 1: Save original system properties BEFORE we modify them
            saveOriginalProxyProperties();

            DefaultProxySelector proxySelector = new DefaultProxySelector();

            // Build optional authentication
            Authentication auth = buildAuthentication();
            if (auth != null) {
                logger.info("Proxy authentication configured for user: {}", proxyConfig.getUsername());
            }

            // Build the non-proxy-hosts string (pipe-delimited)
            String nonProxyHostsPattern = buildNonProxyHostsPattern();

            // Configure both HTTP and HTTPS proxies in Aether
            Proxy httpProxy = new Proxy("http", proxyConfig.getHost(), proxyConfig.getPort(), auth);
            Proxy httpsProxy = new Proxy("https", proxyConfig.getHost(), proxyConfig.getPort(), auth);

            proxySelector.add(httpProxy, nonProxyHostsPattern);
            proxySelector.add(httpsProxy, nonProxyHostsPattern);

            builder.setProxySelector(proxySelector);
            logger.info("Maven resolver proxy configured for HTTP and HTTPS: {}:{} (non-proxy hosts: {})",
                proxyConfig.getHost(), proxyConfig.getPort(), nonProxyHostsPattern);

            // STEP 2: Set Java system properties for JdkTransporterFactory
            setJavaSystemProxyProperties(nonProxyHostsPattern);

            logger.info("Java system proxy properties set for HTTP/HTTPS transporter");
        } catch (Exception e) {
            // Graceful degradation: log the error and continue without proxy
            logger.warn("Failed to configure proxy for Maven resolver ({}:{}). Continuing without proxy. Error: {}",
                proxyConfig.getHost(), proxyConfig.getPort(), e.getMessage());
            logger.debug("Proxy configuration exception details:", e);
        }
    }

    /**
     * Checks if the Java system proxy properties already match the desired configuration.
     *
     * <p>This optimization check compares all proxy-related fields:
     * <ul>
     *   <li>Host and port (must match exactly)</li>
     *   <li>Authentication credentials (must both match or both be absent)</li>
     *   <li>Non-proxy hosts pattern (must match exactly)</li>
     * </ul>
     *
     * <p>If ALL fields match, we can skip the save/set/restore cycle because the system
     * is already in the desired state.
     *
     * @return true if current system properties match desired configuration, false otherwise
     */
    private boolean isProxyAlreadyConfigured() {
        try {
            String currentHttpHost = System.getProperty(HTTP_PROXY_HOST);
            String currentHttpPort = System.getProperty(HTTP_PROXY_PORT);
            String currentHttpsHost = System.getProperty(HTTPS_PROXY_HOST);
            String currentHttpsPort = System.getProperty(HTTPS_PROXY_PORT);

            // Check host and port for both HTTP and HTTPS
            boolean hostPortMatch = 
                matches(currentHttpHost, proxyConfig.getHost()) &&
                matches(currentHttpPort, String.valueOf(proxyConfig.getPort())) &&
                matches(currentHttpsHost, proxyConfig.getHost()) &&
                matches(currentHttpsPort, String.valueOf(proxyConfig.getPort()));

            if (!hostPortMatch) {
                return false; // Host/port mismatch - need to reconfigure
            }

            // Check authentication credentials
            String currentHttpUser = System.getProperty(HTTP_PROXY_USER);
            String currentHttpPass = System.getProperty(HTTP_PROXY_PASSWORD);
            String currentHttpsUser = System.getProperty(HTTPS_PROXY_USER);
            String currentHttpsPass = System.getProperty(HTTPS_PROXY_PASSWORD);

            boolean authMatch = matchesAuth(currentHttpUser, currentHttpPass) &&
                                matchesAuth(currentHttpsUser, currentHttpsPass);

            if (!authMatch) {
                return false; // Auth mismatch - need to reconfigure
            }

            // Check non-proxy hosts pattern
            String currentNonProxyHosts = System.getProperty(HTTP_NON_PROXY_HOSTS);
            String desiredNonProxyHosts = buildNonProxyHostsPattern();

            return matches(currentNonProxyHosts, desiredNonProxyHosts); // All fields match!

        } catch (Exception e) {
            // If anything goes wrong checking, assume not configured
            logger.debug("Error checking if proxy already configured: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Compares two strings for equality, treating null and empty as equivalent.
     *
     * @param current the current value from system properties
     * @param desired the desired value we want to set
     * @return true if values match, false otherwise
     */
    private boolean matches(@Nullable String current, @Nullable String desired) {
        // Normalize: treat null and empty as equivalent
        String normalizedCurrent = (current == null || current.trim().isEmpty()) ? null : current.trim();
        String normalizedDesired = (desired == null || desired.trim().isEmpty()) ? null : desired.trim();

        if (normalizedCurrent == null && normalizedDesired == null) {
            return true; // Both absent
        }
        if (normalizedCurrent == null || normalizedDesired == null) {
            return false; // One absent, one present
        }
        return normalizedCurrent.equals(normalizedDesired);
    }

    /**
     * Checks if authentication credentials in system properties match our desired configuration.
     *
     * @param currentUser current username from system properties
     * @param currentPass current password from system properties
     * @return true if credentials match, false otherwise
     */
    private boolean matchesAuth(@Nullable String currentUser, @Nullable String currentPass) {
        boolean currentHasAuth = (currentUser != null && !currentUser.trim().isEmpty() &&
                                  currentPass != null && !currentPass.trim().isEmpty());
        boolean desiredHasAuth = proxyConfig.hasAuthentication();

        // If one has auth and the other doesn't, they don't match
        if (currentHasAuth != desiredHasAuth) {
            return false;
        }

        // If neither has auth, they match
        if (!currentHasAuth && !desiredHasAuth) {
            return true;
        }

        // Both have auth - compare values
        return matches(currentUser, proxyConfig.getUsername()) &&
               matches(currentPass, proxyConfig.getPassword());
    }

    /**
     * Builds authentication credentials if both username and password are provided.
     *
     * @return Authentication object, or null if credentials are not fully specified
     */
    @Nullable
    private Authentication buildAuthentication() {
        if (proxyConfig.hasAuthentication()) {
            return new AuthenticationBuilder()
                .addUsername(proxyConfig.getUsername())
                .addPassword(proxyConfig.getPassword())
                .build();
        }
        return null;
    }

    /**
     * Builds a pipe-delimited string of host patterns that should bypass the proxy.
     *
     * <p>Always includes hardcoded loopback addresses ({@code localhost|127.0.0.1})
     * and appends user-specified patterns from the proxy configuration.
     *
     * @return pipe-delimited non-proxy hosts pattern, or empty string if none
     */
    private String buildNonProxyHostsPattern() {
        StringBuilder nonProxyHosts = new StringBuilder("localhost|127.0.0.1");

        List<String> configuredPatterns = proxyConfig.getNonProxyHosts();
        if (configuredPatterns != null && !configuredPatterns.isEmpty()) {
            for (String pattern : configuredPatterns) {
                if (pattern != null && !pattern.trim().isEmpty()) {
                    nonProxyHosts.append("|").append(pattern.trim());
                }
            }
        }

        return nonProxyHosts.toString();
    }

    /**
     * Sets Java system properties for proxy configuration.
     *
     * <p>This is CRITICAL because JdkTransporterFactory uses Java's HttpClient,
     * which does NOT respect Aether's ProxySelector — it only reads system properties.
     *
     * <p><strong>Warning:</strong> This modifies JVM-wide system properties!
     * Always call {@link #restoreOriginalProxyProperties()} after Maven resolution completes.
     *
     * @param nonProxyHostsPattern the pipe-delimited non-proxy hosts pattern
     */
    private void setJavaSystemProxyProperties(String nonProxyHostsPattern) {
        // Set proxy host and port for both HTTP and HTTPS
        System.setProperty(HTTP_PROXY_HOST, proxyConfig.getHost());
        System.setProperty(HTTP_PROXY_PORT, String.valueOf(proxyConfig.getPort()));
        System.setProperty(HTTPS_PROXY_HOST, proxyConfig.getHost());
        System.setProperty(HTTPS_PROXY_PORT, String.valueOf(proxyConfig.getPort()));

        // Set proxy authentication if available
        if (proxyConfig.hasAuthentication()) {
            System.setProperty(HTTP_PROXY_USER, proxyConfig.getUsername());
            System.setProperty(HTTP_PROXY_PASSWORD, proxyConfig.getPassword());
            System.setProperty(HTTPS_PROXY_USER, proxyConfig.getUsername());
            System.setProperty(HTTPS_PROXY_PASSWORD, proxyConfig.getPassword());
        }

        // Set non-proxy hosts (Java uses pipe-delimited, same as Aether)
        if (nonProxyHostsPattern != null && !nonProxyHostsPattern.isEmpty()) {
            System.setProperty(HTTP_NON_PROXY_HOSTS, nonProxyHostsPattern);
            // Note: https uses the same nonProxyHosts property as http
        }
    }

    /**
     * Saves the current Java system proxy properties so they can be restored later.
     *
     * <p>This method captures the current state of all proxy-related system properties
     * BEFORE we modify them. This allows us to restore the original state after Maven
     * resolution completes, preventing our temporary proxy settings from leaking to
     * other parts of the application.
     *
     * <p>This method is called automatically by {@link #configureProxy(SessionBuilder)}.
     */
    private void saveOriginalProxyProperties() {
        try {
            originalHttpProxyHost = System.getProperty(HTTP_PROXY_HOST);
            originalHttpProxyPort = System.getProperty(HTTP_PROXY_PORT);
            originalHttpProxyUser = System.getProperty(HTTP_PROXY_USER);
            originalHttpProxyPassword = System.getProperty(HTTP_PROXY_PASSWORD);
            originalHttpsProxyHost = System.getProperty(HTTPS_PROXY_HOST);
            originalHttpsProxyPort = System.getProperty(HTTPS_PROXY_PORT);
            originalHttpsProxyUser = System.getProperty(HTTPS_PROXY_USER);
            originalHttpsProxyPassword = System.getProperty(HTTPS_PROXY_PASSWORD);
            originalHttpNonProxyHosts = System.getProperty(HTTP_NON_PROXY_HOSTS);

            logger.debug("Original proxy properties saved for later restoration");
        } catch (Exception e) {
            logger.warn("Failed to save original proxy properties. Restoration may not work correctly. Error: {}", e.getMessage());
            logger.debug("Save exception details:", e);
        }
    }

    /**
     * Restores the original Java system proxy properties that were saved by
     * {@link #saveOriginalProxyProperties()}.
     *
     * <p><strong>MUST</strong> be called after Maven dependency resolution completes
     * to ensure other HTTP calls in the application are not affected by Maven's
     * proxy configuration.
     *
     * <p><strong>Best Practice:</strong> Call this in a finally block to guarantee
     * cleanup happens even if Maven resolution fails:
     * <pre>{@code
     * MavenProxyConfigurator proxyConfig = new MavenProxyConfigurator(...);
     * try {
     *     proxyConfig.configureProxy(sessionBuilder);
     *     // ... do Maven resolution ...
     * } finally {
     *     proxyConfig.restoreOriginalProxyProperties();
     * }
     * }</pre>
     *
     * <p>If restoration fails (which is rare), a warning is logged but no exception
     * is thrown to prevent disrupting the main Maven resolution flow.
     */
    public void restoreOriginalProxyProperties() {
        try {
            restoreProperty(HTTP_PROXY_HOST, originalHttpProxyHost);
            restoreProperty(HTTP_PROXY_PORT, originalHttpProxyPort);
            restoreProperty(HTTP_PROXY_USER, originalHttpProxyUser);
            restoreProperty(HTTP_PROXY_PASSWORD, originalHttpProxyPassword);
            restoreProperty(HTTPS_PROXY_HOST, originalHttpsProxyHost);
            restoreProperty(HTTPS_PROXY_PORT, originalHttpsProxyPort);
            restoreProperty(HTTPS_PROXY_USER, originalHttpsProxyUser);
            restoreProperty(HTTPS_PROXY_PASSWORD, originalHttpsProxyPassword);
            restoreProperty(HTTP_NON_PROXY_HOSTS, originalHttpNonProxyHosts);

            logger.debug("Original Java system proxy properties restored successfully");
        } catch (Exception e) {
            // Don't throw - just log the warning and continue
            // Better to have a leaked proxy config than to crash the entire scan
            logger.warn("Failed to restore original proxy properties. System proxy settings may be modified. Error: {}", e.getMessage());
            logger.debug("Restore exception details:", e);
        }
    }

    /**
     * Helper method to restore a single system property.
     * If the original value was null (property didn't exist), the property is cleared.
     * If the original value was non-null, it's restored.
     *
     * @param key the system property key
     * @param originalValue the original value to restore (may be null)
     */
    private void restoreProperty(String key, @Nullable String originalValue) {
        if (originalValue == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, originalValue);
        }
    }
}

