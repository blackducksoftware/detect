package com.blackduck.integration.detectable.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class KBComponentHelpers {

    private KBComponentHelpers() {
        // Prevent instantiation
    }

    private static final String INCOMPATIBLE_SUFFIX = "+incompatible";
    private static final String SHA1_REGEX = "[a-fA-F0-9]{40}";
    private static final String SHORT_SHA1_REGEX = "[a-fA-F0-9]{12}";
    private static final String GIT_VERSION_FORMAT = ".*(%s).*";
    private static final Pattern SHA1_VERSION_PATTERN = Pattern.compile(String.format(GIT_VERSION_FORMAT, SHA1_REGEX));
    private static final Pattern SHORT_SHA1_VERSION_PATTERN = Pattern.compile(String.format(GIT_VERSION_FORMAT, SHORT_SHA1_REGEX));

    public static String getKbCompatibleVersion(String version) {
        if (version == null || StringUtils.isBlank(version)) {
            return version;
        }
        String kbCompatibleVersion;
        kbCompatibleVersion = handleGitHash(version);
        kbCompatibleVersion = removeIncompatibleSuffix(kbCompatibleVersion);
        return kbCompatibleVersion;
    }

    // When a version contains a commit hash, the KB only accepts the git hash, so we must strip out the rest.
    private static String handleGitHash(String version) {
        return getVersionFromPattern(version, SHA1_VERSION_PATTERN)
            .orElseGet(() ->
                getVersionFromPattern(version, SHORT_SHA1_VERSION_PATTERN)
                    .orElse(version)
            );
    }

    private static Optional<String> getVersionFromPattern(String version, Pattern versionPattern) {
        Matcher matcher = versionPattern.matcher(version);
        if (matcher.matches()) {
            return Optional.ofNullable(StringUtils.trim(matcher.group(1)));
        }
        return Optional.empty();
    }

    // https://golang.org/ref/mod#incompatible-versions
    private static String removeIncompatibleSuffix(String version) {
        if (version.endsWith(INCOMPATIBLE_SUFFIX)) {
            // Trim incompatible suffix so that KB can match component
            version = version.substring(0, version.length() - INCOMPATIBLE_SUFFIX.length());
        }
        return version;
    }
    
}
