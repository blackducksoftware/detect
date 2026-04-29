package com.blackduck.integration.detectable.detectables.maven.resolver.artifactdownload;

import com.blackduck.integration.detectable.detectables.maven.resolver.exception.ArtifactDownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Configurable retry policy for artifact downloads.
 * Single Responsibility: Determine retry behavior and delays.
 */
public class RetryPolicy {

    private static final Logger logger = LoggerFactory.getLogger(RetryPolicy.class);

    private static final int DEFAULT_RETRY_COUNT = 3;
    private static final long DEFAULT_INITIAL_BACKOFF_MS = 1000;
    private static final long DEFAULT_MAX_BACKOFF_MS = 30000;
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final double JITTER_FACTOR = 0.2;

    private final int maxRetries;
    private final long initialBackoffMs;
    private final long maxBackoffMs;
    private final Random random;

    /**
     * Creates a retry policy with default settings.
     */
    public static RetryPolicy defaultPolicy() {
        return new RetryPolicy(null, null, null);
    }

    /**
     * Creates a retry policy with custom settings.
     *
     * @param retryCount Maximum number of retries (null for default)
     * @param initialBackoffMs Initial backoff in milliseconds (null for default)
     * @param maxBackoffMs Maximum backoff in milliseconds (null for default)
     */
    public RetryPolicy(Integer retryCount, Long initialBackoffMs, Long maxBackoffMs) {
        this.maxRetries = validateRetryCount(retryCount);
        this.initialBackoffMs = validateInitialBackoff(initialBackoffMs);
        this.maxBackoffMs = validateMaxBackoff(maxBackoffMs, this.initialBackoffMs);
        this.random = new Random();

        logger.debug("Retry policy configured: maxRetries={}, initialBackoff={}ms, maxBackoff={}ms",
            this.maxRetries, this.initialBackoffMs, this.maxBackoffMs);
    }

    /**
     * Determines if an exception is retryable.
     *
     * @param exception The exception to check
     * @return true if the operation should be retried
     */
    public boolean isRetryable(ArtifactDownloadException exception) {
        if (exception == null) {
            return false;
        }

        switch (exception.getCategory()) {
            case NETWORK_ERROR:
                // Always retry network errors
                return true;

            case REPOSITORY_ERROR:
                // Check specific error messages
                String message = exception.getMessage();
                if (message != null) {
                    String lowerMessage = message.toLowerCase();

                    // Retry on server errors (5xx)
                    if (lowerMessage.contains("500") || lowerMessage.contains("502") ||
                        lowerMessage.contains("503") || lowerMessage.contains("504")) {
                        return true;
                    }

                    // Retry on timeout (408)
                    if (lowerMessage.contains("408") || lowerMessage.contains("timeout")) {
                        return true;
                    }

                    // Retry on rate limiting (429)
                    if (lowerMessage.contains("429") || lowerMessage.contains("rate limit")) {
                        return true;
                    }

                    // Don't retry on client errors (4xx except 408, 429)
                    if (lowerMessage.contains("401") || lowerMessage.contains("403") ||
                        lowerMessage.contains("404") || lowerMessage.contains("405") ||
                        lowerMessage.contains("406") || lowerMessage.contains("409")) {
                        return false;
                    }
                }
                return false;

            case PARTIAL_DOWNLOAD_ERROR:
                // Retry partial download errors (might be transient)
                return true;

            case FILE_SYSTEM_ERROR:
            case CONFIGURATION_ERROR:
            case RANGE_NOT_SUPPORTED_ERROR:
            case INSUFFICIENT_DISK_SPACE_ERROR:
                // Don't retry these categories
                return false;

            case UNKNOWN_ERROR:
            default:
                // Conservative: don't retry unknown errors
                return false;
        }
    }

    /**
     * Determines if an HTTP response code is retryable.
     *
     * @param responseCode The HTTP response code
     * @return true if the response indicates a retryable condition
     */
    public boolean isRetryableResponseCode(int responseCode) {
        // Retry on server errors (5xx)
        if (responseCode >= 500 && responseCode < 600) {
            return true;
        }

        // Retry on specific client errors
        switch (responseCode) {
            case 408: // Request Timeout
            case 429: // Too Many Requests
                return true;

            default:
                // Don't retry other response codes
                return false;
        }
    }

    /**
     * Calculates the delay before the next retry attempt.
     *
     * @param attemptNumber The current attempt number (1-based)
     * @return Delay in milliseconds
     */
    public long calculateBackoffMs(int attemptNumber) {
        if (attemptNumber <= 0) {
            return 0;
        }

        // Calculate exponential backoff
        double baseDelay = initialBackoffMs * Math.pow(BACKOFF_MULTIPLIER, attemptNumber - 1);

        // Cap at maximum backoff
        baseDelay = Math.min(baseDelay, maxBackoffMs);

        // Add jitter to avoid thundering herd
        double jitterRange = baseDelay * JITTER_FACTOR;
        double jitter = (random.nextDouble() - 0.5) * 2 * jitterRange;

        long finalDelay = (long) Math.max(0, baseDelay + jitter);

        logger.debug("Retry backoff for attempt {}: {}ms (base: {}ms, jitter: {}ms)",
            attemptNumber, finalDelay, (long) baseDelay, (long) jitter);

        return finalDelay;
    }

    /**
     * Executes a retry with backoff.
     *
     * @param attemptNumber The current attempt number
     * @param operation Description of the operation for logging
     * @throws InterruptedException if sleep is interrupted
     */
    public void executeBackoff(int attemptNumber, String operation) throws InterruptedException {
        long backoffMs = calculateBackoffMs(attemptNumber);

        if (backoffMs > 0) {
            logger.info("Retry {} of {} for {} (waiting {}ms)...",
                attemptNumber, maxRetries, operation, backoffMs);

            Thread.sleep(backoffMs);
        }
    }

    /**
     * Wraps a download operation with retry logic.
     *
     * @param operation The operation to execute
     * @param operationName Description for logging
     * @param <T> Result type
     * @return The result of the operation
     * @throws ArtifactDownloadException if all retries fail
     */
    public <T> T executeWithRetry(RetryableOperation<T> operation, String operationName)
            throws ArtifactDownloadException {

        ArtifactDownloadException lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Execute backoff if not first attempt
                if (attempt > 1) {
                    executeBackoff(attempt, operationName);
                }

                // Try the operation
                logger.debug("Attempt {}/{} for {}", attempt, maxRetries, operationName);
                return operation.execute();

            } catch (ArtifactDownloadException e) {
                lastException = e;

                // Check if retryable
                if (!isRetryable(e)) {
                    logger.debug("Non-retryable error for {}: {}", operationName, e.getCategory());
                    throw e;
                }

                // Check if we have more retries
                if (attempt == maxRetries) {
                    logger.error("All {} retry attempts exhausted for {}", maxRetries, operationName);
                    throw e;
                }

                logger.warn("Retryable error on attempt {}/{} for {}: {}",
                    attempt, maxRetries, operationName, e.getSanitizedMessage());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ArtifactDownloadException(
                    ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR,
                    operationName,
                    "Operation interrupted",
                    e
                );
            } catch (Exception e) {
                // Wrap unexpected exceptions
                throw new ArtifactDownloadException(
                    ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR,
                    operationName,
                    "Unexpected error: " + e.getMessage(),
                    e
                );
            }
        }

        // Should not reach here
        if (lastException != null) {
            throw lastException;
        } else {
            throw new ArtifactDownloadException(
                ArtifactDownloadException.ErrorCategory.UNKNOWN_ERROR,
                operationName,
                "Retry logic error: no exception captured"
            );
        }
    }

    /**
     * Functional interface for retryable operations.
     */
    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }

    // Validation methods

    private int validateRetryCount(Integer retryCount) {
        if (retryCount == null) {
            return DEFAULT_RETRY_COUNT;
        }

        if (retryCount < 0) {
            logger.warn("Retry count cannot be negative ({}), using 0", retryCount);
            return 0;
        }

        if (retryCount > 10) {
            logger.warn("Retry count {} exceeds maximum (10), using 10", retryCount);
            return 10;
        }

        return retryCount;
    }

    private long validateInitialBackoff(Long initialBackoffMs) {
        if (initialBackoffMs == null) {
            return DEFAULT_INITIAL_BACKOFF_MS;
        }

        if (initialBackoffMs < 0) {
            logger.warn("Initial backoff cannot be negative ({}), using 0", initialBackoffMs);
            return 0;
        }

        if (initialBackoffMs > 60000) {
            logger.warn("Initial backoff {}ms exceeds maximum (60s), using 60s", initialBackoffMs);
            return 60000;
        }

        return initialBackoffMs;
    }

    private long validateMaxBackoff(Long maxBackoffMs, long initialBackoff) {
        if (maxBackoffMs == null) {
            return DEFAULT_MAX_BACKOFF_MS;
        }

        if (maxBackoffMs < initialBackoff) {
            logger.warn("Max backoff {}ms is less than initial backoff {}ms, using initial backoff",
                maxBackoffMs, initialBackoff);
            return initialBackoff;
        }

        if (maxBackoffMs > 300000) {
            logger.warn("Max backoff {}ms exceeds maximum (5min), using 5min", maxBackoffMs);
            return 300000;
        }

        return maxBackoffMs;
    }

    // Getters for configuration

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getInitialBackoffMs() {
        return initialBackoffMs;
    }

    public long getMaxBackoffMs() {
        return maxBackoffMs;
    }
}