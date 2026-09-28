package com.blackduck.integration.detectable.detectables.bazel.v2;

import java.util.Objects;

import com.blackduck.integration.detectable.detectables.bazel.query.BazelCommandArguments;

/**
 * Value type for a Bazel repository label token as emitted by {@code bazel query} / {@code cquery},
 * e.g. {@code @@abseil-cpp~//absl:strings} (canonical) or {@code @com_google_protobuf//:protobuf}
 * (apparent).
 *
 * <p>This type owns only the <b>pure structural</b> parse of a label:
 * <ul>
 *   <li>whether it is a repo label at all (starts with {@code @}),</li>
 *   <li>whether it is in canonical form ({@code @@}) vs apparent form ({@code @}),</li>
 *   <li>the repo name — the repository portion after the prefix and before {@code //}, with any
 *       version suffix ({@code ~}/{@code +}) left intact,</li>
 *   <li>whether that name denotes a module-extension sub-repo (contains {@code ++}).</li>
 * </ul>
 *
 * <p>It deliberately does <b>not</b> perform version-suffix stripping or repo-map alias resolution.
 * Those are version- and state-dependent and remain the responsibility of
 * {@link BzlmodRepoMappingResolver}, which consumes this type. Keeping that boundary makes the
 * structural parsing here trivially unit-testable while preserving the resolver's existing behavior.
 *
 * <p>The parsing rules mirror the historical inline logic in {@code BzlmodRepoMappingResolver.resolveLabel}
 * and {@code HttpFamilyProber.parseRepoLabels} exactly.
 */
public final class BazelLabel {
    private static final String REPO_PREFIX_SINGLE = BazelCommandArguments.REPO_PREFIX_SINGLE;       // "@"
    private static final String REPO_PREFIX_CANONICAL = BazelCommandArguments.REPO_PREFIX_CANONICAL; // "@@"
    private static final String LABEL_PATH_SEPARATOR = BazelCommandArguments.LABEL_PATH_SEPARATOR;   // "//"
    private static final String MODULE_EXTENSION_MARKER = BazelCommandArguments.MODULE_EXTENSION_MARKER; // "++"

    private final String raw;
    private final boolean repoLabel;
    private final boolean canonical;
    private final boolean hasPath;
    private final String repoName;

    private BazelLabel(String raw, boolean repoLabel, boolean canonical, boolean hasPath, String repoName) {
        this.raw = raw;
        this.repoLabel = repoLabel;
        this.canonical = canonical;
        this.hasPath = hasPath;
        this.repoName = repoName;
    }

    /**
     * Parses a raw label string. Tolerates {@code null} (treated as a non-repo label).
     *
     * @param raw the raw label string (e.g. {@code @@abseil-cpp~//absl:strings})
     * @return the parsed label
     */
    public static BazelLabel parse(String raw) {
        String safe = raw == null ? "" : raw;
        boolean repoLabel = safe.startsWith(REPO_PREFIX_SINGLE);
        boolean canonical = safe.startsWith(REPO_PREFIX_CANONICAL);

        String repoName;
        boolean hasPath;
        if (!repoLabel) {
            repoName = "";
            hasPath = safe.contains(LABEL_PATH_SEPARATOR);
        } else {
            int prefixLen = canonical ? REPO_PREFIX_CANONICAL.length() : REPO_PREFIX_SINGLE.length();
            String afterPrefix = safe.substring(prefixLen);
            int pathIdx = afterPrefix.indexOf(LABEL_PATH_SEPARATOR);
            hasPath = pathIdx >= 0;
            repoName = hasPath ? afterPrefix.substring(0, pathIdx) : afterPrefix;
        }
        return new BazelLabel(safe, repoLabel, canonical, hasPath, repoName);
    }

    /** @return true if this is an external repository label (starts with {@code @}). */
    public boolean isRepoLabel() {
        return repoLabel;
    }

    /** @return true if this label is in canonical form (starts with {@code @@}). */
    public boolean isCanonical() {
        return canonical;
    }

    /** @return true if this label contains a {@code //path:target} portion. */
    public boolean hasPath() {
        return hasPath;
    }

    /**
     * @return the repository name: the portion after the {@code @}/{@code @@} prefix and before
     *         {@code //}, with any version suffix ({@code ~}/{@code +}) left intact. Empty string
     *         for non-repo labels. This is the raw name as it appears in the label — callers that
     *         need the resolved BCR module name must still strip the suffix / resolve aliases.
     */
    public String getRepoName() {
        return repoName;
    }

    /** @return true if the repo name denotes a module-extension sub-repo (contains {@code ++}). */
    public boolean isModuleExtensionSubRepo() {
        return repoName.contains(MODULE_EXTENSION_MARKER);
    }

    /** @return the original, unmodified label string ({@code ""} if it was parsed from {@code null}). */
    public String getRaw() {
        return raw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BazelLabel that = (BazelLabel) o;
        return Objects.equals(raw, that.raw);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw);
    }

    @Override
    public String toString() {
        return "BazelLabel{raw='" + raw + "', canonical=" + canonical + ", repoName='" + repoName + "'}";
    }
}







