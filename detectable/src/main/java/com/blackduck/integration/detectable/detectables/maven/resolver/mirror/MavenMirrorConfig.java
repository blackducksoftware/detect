package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import org.jetbrains.annotations.Nullable;

/**
 * Immutable domain object representing a Maven mirror configuration.
 *
 * <p>A mirror intercepts requests to specific repositories and redirects them
 * to an alternative URL. This is commonly used in corporate environments where
 * a repository manager (Nexus, Artifactory) acts as a proxy/cache for external
 * repositories.
 *
 * <p>The {@code mirrorOf} field follows Maven's mirror matching syntax:
 * <ul>
 *   <li>{@code *} - matches all repositories</li>
 *   <li>{@code central} - matches only the central repository</li>
 *   <li>{@code *,!repo1} - matches all except repo1</li>
 *   <li>{@code external:*} - matches all external repositories</li>
 * </ul>
 *
 * <p>Thread-safety: This class is immutable and thread-safe.
 */
public class MavenMirrorConfig {

    private final String id;
    private final String url;
    private final String mirrorOf;

    @Nullable
    private final String username;

    @Nullable
    private final String password;

    /**
     * Constructs a MavenMirrorConfig with all fields.
     *
     * @param id       Unique identifier for this mirror (used for auth matching)
     * @param url      The URL of the mirror repository
     * @param mirrorOf Pattern specifying which repositories this mirror intercepts
     * @param username Optional authentication username (may be null)
     * @param password Optional authentication password (may be null)
     */
    public MavenMirrorConfig(
        String id,
        String url,
        String mirrorOf,
        @Nullable String username,
        @Nullable String password
    ) {
        this.id = id;
        this.url = url;
        this.mirrorOf = mirrorOf;
        this.username = username;
        this.password = password;
    }

    /**
     * Constructs a MavenMirrorConfig without authentication.
     *
     * @param id       Unique identifier for this mirror
     * @param url      The URL of the mirror repository
     * @param mirrorOf Pattern specifying which repositories this mirror intercepts
     */
    public MavenMirrorConfig(String id, String url, String mirrorOf) {
        this(id, url, mirrorOf, null, null);
    }

    /**
     * Returns the unique identifier for this mirror.
     *
     * @return mirror ID, never null
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the URL of the mirror repository.
     *
     * @return mirror URL, never null
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the pattern specifying which repositories this mirror intercepts.
     *
     * @return mirrorOf pattern, never null
     */
    public String getMirrorOf() {
        return mirrorOf;
    }

    /**
     * Returns the authentication username.
     *
     * @return username, or null if no authentication is configured
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * Returns the authentication password.
     *
     * @return password, or null if no authentication is configured
     */
    @Nullable
    public String getPassword() {
        return password;
    }

    /**
     * Checks if this mirror has authentication credentials configured.
     *
     * @return true if both username and password are non-null and non-empty
     */
    public boolean hasAuthentication() {
        return username != null && !username.trim().isEmpty()
            && password != null && !password.trim().isEmpty();
    }

    /**
     * Returns a string representation of this mirror configuration.
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
        return "MavenMirrorConfig{" +
            "id='" + id + '\'' +
            ", url='" + url + '\'' +
            ", mirrorOf='" + mirrorOf + '\'' +
            ", hasAuth=" + hasAuthentication() +
            '}';
    }
}

