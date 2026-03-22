package com.blackduck.integration.detectable.detectables.maven.resolver;

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

    private final String proxyHost;
    private final int proxyPort;
    @Nullable
    private final String proxyUsername;
    @Nullable
    private final String proxyPassword;
    private final List<String> proxyIgnoredHosts;

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
     * Constructs a proxy configurator with the given settings.
     *
     * @param proxyHost         Proxy hostname or IP — plain value, <strong>no</strong> {@code http://} or {@code https://} prefix.
     * @param proxyPort         Proxy port number (must be &gt; 0).
     * @param proxyUsername     Optional proxy authentication username (may be null).
     * @param proxyPassword     Optional proxy authentication password (may be null).
     * @param proxyIgnoredHosts Host patterns that should bypass the proxy (never null, may be empty).
     * @throws IllegalArgumentException if proxyHost is null/blank or proxyPort is &lt;= 0
     */
    public MavenProxyConfigurator(
        String proxyHost,
        int proxyPort,
        @Nullable String proxyUsername,
        @Nullable String proxyPassword,
        List<String> proxyIgnoredHosts
    ) {
        if (proxyHost == null || proxyHost.trim().isEmpty()) {
            throw new IllegalArgumentException("Proxy host cannot be null or empty");
        }
        if (proxyPort <= 0) {
            throw new IllegalArgumentException("Proxy port must be greater than 0");
        }

        this.proxyHost = proxyHost.trim();
        this.proxyPort = proxyPort;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        this.proxyIgnoredHosts = proxyIgnoredHosts != null ? proxyIgnoredHosts : java.util.Collections.emptyList();
    }

    /**
     * Configures proxy settings on the given Aether session builder.
     *
     * <p>This method performs three critical steps:
     * <ol>
     *   <li>Saves original Java system proxy properties for later restoration</li>
     *   <li>Configures Aether's ProxySelector for both HTTP and HTTPS protocols</li>
     *   <li>Sets Java system properties ({@code http.proxyHost}, {@code https.proxyHost}, etc.)
     *       because JdkTransporterFactory does NOT respect Aether's ProxySelector</li>
     * </ol>
     *
     * <p>Non-proxy hosts are built from the configured patterns combined with
     * hardcoded loopback addresses {@code localhost} and {@code 127.0.0.1}.
     *
     * <p><strong>IMPORTANT:</strong> After Maven resolution completes, you MUST call
     * {@link #restoreOriginalProxyProperties()} to restore the original system state,
     * preferably in a finally block to ensure cleanup happens even if errors occur.
     *
     * <p>If anything goes wrong during configuration, a warning is logged and
     * the method continues gracefully (no exception is thrown).
     *
     * @param builder the session builder to configure with proxy settings
     */
    public void configureProxy(SessionBuilder builder) {
        try {
            // STEP 1: Save original system properties BEFORE we modify them
            saveOriginalProxyProperties();

            DefaultProxySelector proxySelector = new DefaultProxySelector();

            // Build optional authentication
            Authentication auth = buildAuthentication();
            if (auth != null) {
                logger.info("Proxy authentication configured for user: {}", proxyUsername);
            }

            // Build the non-proxy-hosts string (pipe-delimited)
            String nonProxyHostsPattern = buildNonProxyHostsPattern();

            // Configure both HTTP and HTTPS proxies in Aether
            Proxy httpProxy = new Proxy("http", proxyHost, proxyPort, auth);
            Proxy httpsProxy = new Proxy("https", proxyHost, proxyPort, auth);

            proxySelector.add(httpProxy, nonProxyHostsPattern);
            proxySelector.add(httpsProxy, nonProxyHostsPattern);

            builder.setProxySelector(proxySelector);
            logger.info("Maven resolver proxy configured for HTTP and HTTPS: {}:{} (non-proxy hosts: {})",
                proxyHost, proxyPort, nonProxyHostsPattern);

            // STEP 2: Set Java system properties for JdkTransporterFactory
            setJavaSystemProxyProperties(nonProxyHostsPattern);

            logger.info("Java system proxy properties set for HTTP/HTTPS transporter");
        } catch (Exception e) {
            // Graceful degradation: log the error and continue without proxy
            logger.warn("Failed to configure proxy for Maven resolver ({}:{}). Continuing without proxy. Error: {}",
                proxyHost, proxyPort, e.getMessage());
            logger.debug("Proxy configuration exception details:", e);
        }
    }

    /**
     * Builds authentication credentials if both username and password are provided.
     *
     * @return Authentication object, or null if credentials are not fully specified
     */
    @Nullable
    private Authentication buildAuthentication() {
        if (proxyUsername != null && !proxyUsername.trim().isEmpty()
            && proxyPassword != null && !proxyPassword.trim().isEmpty()) {
            return new AuthenticationBuilder()
                .addUsername(proxyUsername)
                .addPassword(proxyPassword)
                .build();
        }
        return null;
    }

    /**
     * Builds a pipe-delimited string of host patterns that should bypass the proxy.
     *
     * <p>Always includes hardcoded loopback addresses ({@code localhost|127.0.0.1})
     * and appends user-specified patterns from {@code blackduck.proxy.ignored.hosts}.
     *
     * @return pipe-delimited non-proxy hosts pattern, or empty string if none
     */
    private String buildNonProxyHostsPattern() {
        StringBuilder nonProxyHosts = new StringBuilder("localhost|127.0.0.1");

        if (proxyIgnoredHosts != null && !proxyIgnoredHosts.isEmpty()) {
            for (String pattern : proxyIgnoredHosts) {
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
        System.setProperty("http.proxyHost", proxyHost);
        System.setProperty("http.proxyPort", String.valueOf(proxyPort));
        System.setProperty("https.proxyHost", proxyHost);
        System.setProperty("https.proxyPort", String.valueOf(proxyPort));

        // Set proxy authentication if available
        if (proxyUsername != null && !proxyUsername.trim().isEmpty()
            && proxyPassword != null && !proxyPassword.trim().isEmpty()) {
            System.setProperty("http.proxyUser", proxyUsername);
            System.setProperty("http.proxyPassword", proxyPassword);
            System.setProperty("https.proxyUser", proxyUsername);
            System.setProperty("https.proxyPassword", proxyPassword);
        }

        // Set non-proxy hosts (Java uses pipe-delimited, same as Aether)
        if (nonProxyHostsPattern != null && !nonProxyHostsPattern.isEmpty()) {
            System.setProperty("http.nonProxyHosts", nonProxyHostsPattern);
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
            originalHttpProxyHost = System.getProperty("http.proxyHost");
            originalHttpProxyPort = System.getProperty("http.proxyPort");
            originalHttpProxyUser = System.getProperty("http.proxyUser");
            originalHttpProxyPassword = System.getProperty("http.proxyPassword");
            originalHttpsProxyHost = System.getProperty("https.proxyHost");
            originalHttpsProxyPort = System.getProperty("https.proxyPort");
            originalHttpsProxyUser = System.getProperty("https.proxyUser");
            originalHttpsProxyPassword = System.getProperty("https.proxyPassword");
            originalHttpNonProxyHosts = System.getProperty("http.nonProxyHosts");

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
            restoreProperty("http.proxyHost", originalHttpProxyHost);
            restoreProperty("http.proxyPort", originalHttpProxyPort);
            restoreProperty("http.proxyUser", originalHttpProxyUser);
            restoreProperty("http.proxyPassword", originalHttpProxyPassword);
            restoreProperty("https.proxyHost", originalHttpsProxyHost);
            restoreProperty("https.proxyPort", originalHttpsProxyPort);
            restoreProperty("https.proxyUser", originalHttpsProxyUser);
            restoreProperty("https.proxyPassword", originalHttpsProxyPassword);
            restoreProperty("http.nonProxyHosts", originalHttpNonProxyHosts);

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

