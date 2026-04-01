package com.blackduck.integration.detectable.detectables.dart;

import java.util.List;
import java.util.Optional;

import com.blackduck.integration.util.NameVersion;

public class PubSpecYamlNameVersionParser {
    private static final String NAME_KEY = "name:";
    private static final String VERSION_KEY = "version:";

    public Optional<NameVersion> parseNameVersion(List<String> pubSpecYamlLines) {
        String name = null;
        String version = null;
        for (String line : pubSpecYamlLines) {
            if (isTopLevel(line) && line.trim().startsWith(NAME_KEY)) {
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    name = parts[1].trim();
                }
            } else if (isTopLevel(line) && line.trim().startsWith(VERSION_KEY)) {
                String[] parts = line.trim().split("\\s+", 2);
                if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                    version = parts[1].trim();
                }
            }
        }
        if (name == null && version == null) {
            return Optional.empty();
        }
        return Optional.of(new NameVersion(name, version));
    }

    private boolean isTopLevel(String line) {
        return !line.isEmpty() && !Character.isWhitespace(line.charAt(0));
    }
}
