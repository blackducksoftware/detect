package com.blackduck.integration.detectable.detectables.maven.resolver.mirror;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Stateless utility for matching repositories against Maven mirror configurations.
 *
 * <p>Encapsulates the {@code <mirrorOf>} pattern matching syntax defined by Maven:
 * <ul>
 *   <li>{@code *} or {@code external:*} — matches any repository</li>
 *   <li>{@code repoId} — matches that single repository id</li>
 *   <li>{@code !repoId} — explicitly excludes that repository id</li>
 *   <li>Tokens are comma-separated, e.g. {@code *,!internal-repo}</li>
 * </ul>
 *
 * <p>This class exists so that every code path that needs mirror substitution
 * (Aether-driven JAR download, raw POM download, fallback download, ...) shares a
 * single, well-tested implementation. Callers should never re-implement matching inline.
 *
 * <p>Thread-safety: stateless and thread-safe.
 */
public final class MavenMirrorResolver {

    private MavenMirrorResolver() {
        // utility class — not instantiable
    }

    /**
     * Returns the first mirror whose {@code mirrorOf} pattern matches the given repository id,
     * or {@code null} when no mirror applies.
     *
     * @param mirrors mirror configurations to consult — may be null or empty
     * @param repoId  id of the repository being requested — may be null (returns null)
     * @return the matching mirror, or {@code null}
     */
    @Nullable
    public static MavenMirrorConfig findMatchingMirror(@Nullable List<MavenMirrorConfig> mirrors, @Nullable String repoId) {
        if (mirrors == null || mirrors.isEmpty() || repoId == null) {
            return null;
        }
        for (MavenMirrorConfig mirror : mirrors) {
            if (matchesMirrorOf(mirror.getMirrorOf(), repoId)) {
                return mirror;
            }
        }
        return null;
    }

    /**
     * Tests whether the given {@code mirrorOf} pattern matches a repository id.
     *
     * @param mirrorOf comma-separated mirror-of expression (see class javadoc)
     * @param repoId   repository id to test
     * @return {@code true} if the pattern includes (and does not exclude) the repository id
     */
    public static boolean matchesMirrorOf(@Nullable String mirrorOf, @Nullable String repoId) {
        if (mirrorOf == null || mirrorOf.trim().isEmpty() || repoId == null) {
            return false;
        }

        boolean included = false;
        boolean excluded = false;

        for (String part : mirrorOf.split(",")) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.startsWith("!")) {
                if (token.substring(1).equals(repoId)) {
                    excluded = true;
                }
            } else if ("*".equals(token) || "external:*".equals(token)) {
                included = true;
            } else if (token.equals(repoId)) {
                included = true;
            }
        }

        return included && !excluded;
    }
}

