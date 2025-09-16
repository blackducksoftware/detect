package com.blackduck.integration.detect.lifecycle.boot.product.version;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;

public class BlackDuckVersionParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Pattern versionPattern = Pattern.compile("^([0-9]{4})\\.(\\d+)\\.(\\d+).*?");

    public Optional<BlackDuckVersion> parse(String blackDuckVersionString) {
        try {
            Matcher m = versionPattern.matcher(blackDuckVersionString);
            if (!m.matches()) {
                return Optional.empty();
            }
            String[] parts = { m.group(1), m.group(2), m.group(3) };
            // we are guaranteed that parts will be integers from the pattern match at this point.
            int major_version_from_bd = Integer.parseInt(parts[0]);
            int minor_version_from_bd = Integer.parseInt(parts[1]);
            int patch_version_from_bd = Integer.parseInt(parts[2]);
            logger.trace("Black Duck SCA version parsed from version string {}: major: {}, minor: {}, patch: {}",
                blackDuckVersionString, major_version_from_bd, minor_version_from_bd, patch_version_from_bd
            );
            return Optional.of(new BlackDuckVersion(major_version_from_bd, minor_version_from_bd, patch_version_from_bd));
        } catch (Exception e) {
            logger.warn("Unable to determine the Black Duck SCA version from version string {}", blackDuckVersionString);
            return Optional.empty();
        }
    }
}
