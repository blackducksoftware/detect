package com.blackduck.integration.detectable.detectables.bun;

import com.blackduck.integration.util.NameVersion;

public class BunPackageNameUtils {
    public static final String PACKAGE_JSON_FILENAME = "package.json";

    private BunPackageNameUtils() {}

    // Splits "name@version" or "@scope/name@version" on the last '@'.
    // Returns null if no '@' is found (excluding a leading scope '@').
    public static NameVersion parseNameVersion(String s) {
        int lastAt = s.lastIndexOf('@');
        if (lastAt <= 0) {
            return null;
        }
        return new NameVersion(s.substring(0, lastAt), s.substring(lastAt + 1));
    }
}
