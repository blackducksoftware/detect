package com.blackduck.integration.detectable.detectables.maven.resolver;

/**
 * Internal constants for Maven artifact download configuration.
 * These values are intentionally NOT exposed as user-facing flags.
 * They are tuned to work well across a wide range of network environments.
 */
public final class MavenDownloadConstants {

    private MavenDownloadConstants() {
        // Utility class - no instantiation
    }

    /** HTTP connection timeout in milliseconds (30 seconds). */
    public static final int CONNECT_TIMEOUT_MS = 30_000;

    /** HTTP read timeout in milliseconds (60 seconds — large JARs need more time). */
    public static final int READ_TIMEOUT_MS = 60_000;

    /** Number of concurrent download threads. */
    public static final int DOWNLOAD_THREADS = 4;

    /** Maximum number of retry attempts for transient failures. */
    public static final int RETRY_COUNT = 3;

    /** Initial backoff delay in milliseconds before the first retry (1 second). */
    public static final long RETRY_BACKOFF_INITIAL_MS = 1_000L;

    /** Maximum backoff delay in milliseconds between retries (30 seconds). */
    public static final long RETRY_BACKOFF_MAX_MS = 30_000L;
}

