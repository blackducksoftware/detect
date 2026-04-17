package com.blackduck.integration.detect.lifecycle.boot.cache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.configuration.config.PropertyConfiguration;
import com.blackduck.integration.configuration.property.Property;
import com.blackduck.integration.configuration.source.MapPropertySource;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.DetectPropertyUtil;

// TODO: No file-level locking is implemented for concurrent Detect runs against the same project directory.
//       If multiple instances run simultaneously, the last writer wins. This is acceptable for v1 as the
//       cache is advisory, but should be addressed with file locking (e.g., FileLock) in a future iteration.
public class ScanConfigCacheService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String CACHE_DIR_NAME = "scan-config-cache";
    private static final String CACHE_FILE_PREFIX = "cache_";
    private static final String CACHE_FILE_SUFFIX = ".properties";

    public Optional<MapPropertySource> loadCachedProperties() {
        if (!isInteractiveTerminal()) {
            return Optional.empty();
        }

        File cacheFile = getCacheFile();
        if (!cacheFile.exists()) {
            return Optional.empty();
        }

        try {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                props.load(fis);
            }

            if (props.isEmpty()) {
                return Optional.empty();
            }

            Map<String, String> cachedMap = new HashMap<>();
            for (String key : props.stringPropertyNames()) {
                cachedMap.put(key, props.getProperty(key));
            }

            System.out.println();
            System.out.println("========================================================");
            System.out.println("  Found saved scan configuration for this project directory");
            System.out.println("========================================================");
            System.out.println();
            for (Map.Entry<String, String> entry : cachedMap.entrySet()) {
                System.out.printf("    %-50s = %s%n", entry.getKey(), entry.getValue());
            }
            System.out.println();

            String response = promptUser(
                "NOTE: Cached settings are applied at the lowest priority. If you have provided any of\n"
                    + "the same flags explicitly on the command line or via environment variables, those\n"
                    + "values will override the cached ones for this run.\n"
                    + "\n"
                    + "Apply these cached settings? (Y/n)"
            );
            if (response == null || response.trim().isEmpty() || response.trim().toLowerCase().startsWith("y")) {
                logger.debug("User accepted cached scan configuration.");
                System.out.println("Cached settings will be applied for this scan.");
                return Optional.of(new MapPropertySource("cached-scan-config", cachedMap));
            } else {
                logger.debug("User declined cached scan configuration.");
                System.out.println("Skipping cached settings. Proceeding with provided configuration only.");
                return Optional.empty();
            }
        } catch (IOException e) {
            logger.warn("Unable to read cached scan configuration: {}", e.getMessage());
            logger.debug("Cache read error details: ", e);
            return Optional.empty();
        }
    }

    public void promptAndSaveConfiguration(PropertyConfiguration propertyConfiguration) {
        if (!isInteractiveTerminal()) {
            return;
        }

        Predicate<String> sensitiveKeyPredicate = DetectPropertyUtil.getPasswordsAndTokensPredicate();
        Set<Property> knownProperties = new HashSet<>(DetectProperties.allProperties().getProperties());
        Map<String, String> rawValueMap = propertyConfiguration.getRawValueMap(knownProperties);

        Properties propsToSave = new Properties();
        for (Map.Entry<String, String> entry : rawValueMap.entrySet()) {
            if (sensitiveKeyPredicate.test(entry.getKey())) {
                continue;
            }
            propsToSave.setProperty(entry.getKey(), entry.getValue());
        }

        if (propsToSave.isEmpty()) {
            logger.debug("No cacheable configuration properties found. Skipping cache save.");
            return;
        }

        // Skip prompt if the current config is identical to what is already cached.
        File cacheFile = getCacheFile();
        if (cacheFile.exists()) {
            try {
                Properties existing = new Properties();
                try (FileInputStream fis = new FileInputStream(cacheFile)) {
                    existing.load(fis);
                }
                if (existing.equals(propsToSave)) {
                    logger.info("Scan configuration is unchanged since last save. Cache is up to date.");
                    return;
                }
            } catch (IOException e) {
                logger.debug("Could not read existing cache file for comparison: {}", e.getMessage());
            }
        }

        String response = promptUser(
            "Scan completed successfully. Save this configuration for future runs in this directory?\n"
                + "(Sensitive values like API tokens and passwords are never saved.) (Y/n)"
        );
        if (response != null && !response.trim().isEmpty() && !response.trim().toLowerCase().startsWith("y")) {
            logger.debug("User declined to save scan configuration to cache.");
            System.out.println("Save skipped. Configuration was not cached.");
            return;
        }

        File parentDir = cacheFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            String workingDir = System.getProperty("user.dir");
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String comment = String.format(
                "Black Duck Detect cached scan configuration%nGenerated: %s%nProject directory: %s",
                timestamp, workingDir
            );
            propsToSave.store(fos, comment);
            logger.info("Scan configuration saved to: {}", cacheFile.getAbsolutePath());
            System.out.println("Configuration saved. It will be offered for reuse on the next run in this directory.");
        } catch (IOException e) {
            logger.warn("Unable to save scan configuration to cache: {}", e.getMessage());
            logger.debug("Cache write error details: ", e);
        }
    }

    private boolean isInteractiveTerminal() {
        return System.getenv("CI") == null;
    }

    private String promptUser(String prompt) {
        System.out.println(prompt);
        System.out.print("> ");
        System.out.flush();
        if (System.console() != null) {
            return System.console().readLine();
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException e) {
            logger.debug("Unable to read user input: {}", e.getMessage());
            return null;
        }
    }

    private File getCacheFile() {
        String userHome = System.getProperty("user.home");
        File cacheDir = new File(userHome, "blackduck" + File.separator + CACHE_DIR_NAME);
        String workingDir = System.getProperty("user.dir");
        String hash = sha256Hex(workingDir);
        return new File(cacheDir, CACHE_FILE_PREFIX + hash + CACHE_FILE_SUFFIX);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

