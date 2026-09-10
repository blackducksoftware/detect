package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.Objects;

import com.blackduck.integration.detectable.detectables.bazel.query.BazelCommandArguments;

/**
 * Value type for a Bazel {@code mod graph} module key of the form {@code name@version}
 * (e.g. {@code protobuf@31.0}).
 *
 * <p>Centralizes the {@code name@version} parsing that was previously duplicated as ad-hoc
 * {@code indexOf('@')}/{@code substring(...)} logic. The parsing rules deliberately mirror the
 * historical {@code BzlmodGraphJsonParser.extractName}/{@code extractVersion} behavior exactly:
 * <ul>
 *   <li>The name is the substring before the first {@code @}, or the whole key when there is no
 *       {@code @} at an index greater than 0 (so a leading {@code @} is not treated as a separator).</li>
 *   <li>The version is the substring after the first {@code @}, or {@code null} when there is no
 *       {@code @}, the {@code @} is at index 0, or the {@code @} is the last character.</li>
 * </ul>
 */
public final class ModuleKey {
    private static final String SEPARATOR = BazelCommandArguments.MODULE_KEY_SEPARATOR;

    private final String rawKey;
    private final String name;
    private final String version; // nullable, mirrors extractVersion() returning null

    private ModuleKey(String rawKey, String name, String version) {
        this.rawKey = rawKey;
        this.name = name;
        this.version = version;
    }

    /**
     * Parses a raw module key ({@code name@version}) into a {@link ModuleKey}.
     *
     * @param moduleKey the raw key (must not be {@code null})
     * @return the parsed key
     */
    public static ModuleKey parse(String moduleKey) {
        int atIdx = moduleKey.indexOf(SEPARATOR);
        String name = atIdx > 0 ? moduleKey.substring(0, atIdx) : moduleKey;
        String version = (atIdx > 0 && atIdx < moduleKey.length() - 1) ? moduleKey.substring(atIdx + 1) : null;
        return new ModuleKey(moduleKey, name, version);
    }

    /** @return the module name (e.g. {@code protobuf} from {@code protobuf@31.0}). */
    public String getName() {
        return name;
    }

    /** @return the version, or {@code null} if the key has no {@code @version} part. */
    public String getVersion() {
        return version;
    }

    /** @return the original, unmodified key string. */
    public String getRawKey() {
        return rawKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ModuleKey that = (ModuleKey) o;
        return Objects.equals(name, that.name) && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    @Override
    public String toString() {
        return "ModuleKey{name='" + name + "', version='" + version + "'}";
    }
}



