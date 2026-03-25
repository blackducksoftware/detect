package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Domain object representing a resolved proxy configuration for Maven dependency resolution.
 *
 * <p>This class holds the configuration needed to route Maven artifact downloads through
 * a corporate forward proxy.
 *
 * <p><strong>Security Note:</strong> This class holds sensitive credentials (username/password).
 * The {@link #toString()} method intentionally masks these values to prevent accidental
 * logging of credentials.
 *
 * <p>Instances are immutable after construction.
 */
public class MavenProxyConfig {

    private final String host;
    private final int port;
    @Nullable
    private final String username;
    @Nullable
    private final String password;
    private final List<String> nonProxyHosts;

    /**
     * Constructs a proxy configuration without authentication.
     *
     * @param host           proxy hostname or IP (plain value, no http:// prefix)
     * @param port           proxy port
     * @param nonProxyHosts  list of host patterns that should bypass the proxy (may be empty, never null)
     */
    public MavenProxyConfig(String host, int port, List<String> nonProxyHosts) {
        this(host, port, null, null, nonProxyHosts);
    }

    /**
     * Constructs a proxy configuration with optional authentication.
     *
     * @param host           proxy hostname or IP (plain value, no http:// prefix)
     * @param port           proxy port
     * @param username       proxy authentication username (may be null)
     * @param password       proxy authentication password (may be null)
     * @param nonProxyHosts  list of host patterns that should bypass the proxy (may be empty, never null)
     */
    public MavenProxyConfig(
        String host,
        int port,
        @Nullable String username,
        @Nullable String password,
        List<String> nonProxyHosts
    ) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.nonProxyHosts = nonProxyHosts != null ? nonProxyHosts : java.util.Collections.emptyList();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getPassword() {
        return password;
    }

    public List<String> getNonProxyHosts() {
        return nonProxyHosts;
    }

    /**
     * Checks if this proxy has authentication credentials configured.
     *
     * @return true if both username and password are non-null and non-empty
     */
    public boolean hasAuthentication() {
        return username != null && !username.trim().isEmpty()
            && password != null && !password.trim().isEmpty();
    }

    /**
     * Returns a string representation of this proxy configuration.
     *
     * <p><strong>Security Note:</strong> This method intentionally does NOT include
     * the actual username or password fields to prevent credential leakage into log files.
     * Only the {@code hasAuth} flag is included to indicate whether credentials are configured.
     *
     * <p>To access actual credentials, use {@link #getUsername()} and {@link #getPassword()}.
     *
     * @return safe string representation with credentials masked
     */
    @Override
    public String toString() {
        return "MavenProxyConfig{" +
            "host='" + host + '\'' +
            ", port=" + port +
            ", hasAuth=" + hasAuthentication() +
            ", nonProxyHosts=" + nonProxyHosts.size() + " patterns" +
            '}';
    }
}

