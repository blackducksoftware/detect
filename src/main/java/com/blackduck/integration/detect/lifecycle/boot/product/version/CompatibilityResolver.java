package com.blackduck.integration.detect.lifecycle.boot.product.version;

import java.util.List;
import java.util.Optional;

import com.blackduck.integration.blackduck.version.BlackDuckVersion;

public class CompatibilityResolver {

    private static final String WILDCARD_SUFFIX = ".x";

    public CompatibilityResult resolve(CompatibilityMatrix matrix, BlackDuckVersion serverVersion, String rawDetectVersion) {
        String detectVersion = normalizeDetectVersion(rawDetectVersion);
        String serverVersionString = formatServerVersion(serverVersion);

        if (detectVersion.isEmpty()) {
            return CompatibilityResult.unknown("Detect version is not available; skipping compatibility check.");
        }

        // One-sided suppress: if this Detect is NEWER than anything the matrix knows about, the
        // matrix simply hasn't caught up yet (typical for a freshly cut release or a SNAPSHOT
        // dev build) — skip. Older-than-matrix Detect versions intentionally fall through to
        // the row check below and produce a legitimate INCOMPATIBLE warning.
        if (isDetectVersionNewerThanMatrix(detectVersion, matrix)) {
            return CompatibilityResult.unknown(String.format(
                "Detect %s is newer than the embedded compatibility matrix; skipping compatibility check.",
                detectVersion
            ));
        }

        Optional<CompatibilityRow> matchedRow = findRowForServer(serverVersion, matrix);
        if (matchedRow.isEmpty()) {
            return CompatibilityResult.unknown(String.format(
                "Black Duck SCA %s is not in the embedded compatibility matrix; skipping compatibility check.",
                serverVersionString
            ));
        }

        List<String> compatibleDetectVersions = matchedRow.get().getDetectVersions();
        if (isDetectInList(detectVersion, compatibleDetectVersions)) {
            return CompatibilityResult.compatible(String.format(
                "Detect %s is compatible with Black Duck SCA %s.",
                detectVersion, serverVersionString
            ));
        }

        String message = String.format(
            "Detect %s is NOT listed as compatible with Black Duck SCA %s. "
                + "Compatible Detect versions for Black Duck SCA %s: %s. "
                + "See the compatibility matrix: %s",
            detectVersion,
            serverVersionString,
            serverVersionString,
            String.join(", ", compatibleDetectVersions),
            matrix.getMatrixSourceUrl()
        );
        return CompatibilityResult.incompatible(message);
    }

    private String formatServerVersion(BlackDuckVersion version) {
        return version.getMajor() + "." + version.getMinor() + "." + version.getPatch();
    }

    // Strip anything after the digits/dots portion: "12.1.0-SIGQA9-SNAPSHOT" -> "12.1.0"
    // Also handles bare numeric like "12.1.0" (unchanged) and null/empty (returns empty).
    private String normalizeDetectVersion(String rawDetectVersion) {
        if (rawDetectVersion == null) {
            return "";
        }
        String trimmed = rawDetectVersion.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        return trimmed.split("[^0-9.]", 2)[0];
    }

    private boolean isDetectVersionNewerThanMatrix(String detectVersion, CompatibilityMatrix matrix) {
        Optional<int[]> running = CompatibilityVersions.parse(detectVersion);
        // empty matrix; row check will produce its own UNKNOWN
        // can't parse; err on the side of running the row check
        return running.map(ints -> matrix.getRows().stream()
            .flatMap(row -> row.getDetectVersions().stream())
            .map(CompatibilityVersions::parse)
            .flatMap(Optional::stream)
            .max(CompatibilityVersions::compare)
            .map(max -> CompatibilityVersions.compare(ints, max) > 0)
            .orElse(false)).orElse(false);
    }

    // Row label examples on the public page: "2026.7.x", "2026.4.x". Match on major+minor only.
    private Optional<CompatibilityRow> findRowForServer(BlackDuckVersion serverVersion, CompatibilityMatrix matrix) {
        for (CompatibilityRow row : matrix.getRows()) {
            if (rowMatchesServer(row.getBlackDuckScaRelease(), serverVersion)) {
                return Optional.of(row);
            }
        }
        return Optional.empty();
    }

    private boolean rowMatchesServer(String rowLabel, BlackDuckVersion serverVersion) {
        return CompatibilityVersions.parse(rowLabel)
            .map(parts -> serverVersion.getMajor() == parts[0] && serverVersion.getMinor() == parts[1])
            .orElse(false);
    }

    // Entry examples: exact "12.0.0" or minor wildcard "9.10.x".
    private boolean isDetectInList(String detectVersion, List<String> candidates) {
        if (candidates == null) {
            return false;
        }
        Optional<int[]> parsedDetectVersion = CompatibilityVersions.parse(detectVersion);
        for (String candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            if (candidate.equals(detectVersion)) {
                return true;
            }
            if (parsedDetectVersion.isPresent()
                && candidate.endsWith(WILDCARD_SUFFIX)
                && matchesMinorWildcard(parsedDetectVersion.get(), candidate)) {
                return true;
            }
        }
        return false;
    }

    // "9.10.5" matches wildcard "9.10.x" (major.minor equal, any patch).
    private boolean matchesMinorWildcard(int[] parsedDetectVersion, String wildcardCandidate) {
        return CompatibilityVersions.parse(wildcardCandidate)
            .map(wild -> parsedDetectVersion[0] == wild[0] && parsedDetectVersion[1] == wild[1])
            .orElse(false);
    }
}
