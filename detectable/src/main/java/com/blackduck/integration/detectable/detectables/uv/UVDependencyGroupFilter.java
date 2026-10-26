package com.blackduck.integration.detectable.detectables.uv;

import org.slf4j.Logger;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves effective dependency groups by applying the only-vs-excluded filtering logic.
 * Centralizes the conflict detection and warning logging that both the CLI (buildless)
 * and lockfile detector paths share.
 */
public class UVDependencyGroupFilter {

    private final Set<String> onlyGroups;
    private final Set<String> excludedGroups;
    private final Set<String> conflictingGroups;
    private final Set<String> effectiveOnlyGroups;

    public UVDependencyGroupFilter(UVDetectorOptions options) {
        this.onlyGroups = options.getOnlyDependencyGroups();
        this.excludedGroups = options.getExcludedDependencyGroups();
        this.conflictingGroups = computeConflictingGroups();
        this.effectiveOnlyGroups = computeEffectiveOnlyGroups();
    }

    /**
     * Groups that appear in both onlyGroups and excludedGroups.
     */
    public Set<String> getConflictingGroups() {
        return conflictingGroups;
    }

    /**
     * The onlyGroups after removing any that are also excluded.
     * If onlyGroups was empty, returns empty (meaning "no only-filter active").
     * If excludedGroups is empty, returns onlyGroups as-is (no subtraction needed).
     */
    public Set<String> getEffectiveOnlyGroups() {
        return effectiveOnlyGroups;
    }

    /**
     * Returns true if onlyGroups was specified and at least one group survives exclusion.
     * Returns true also when onlyGroups is empty (no only-filter active, default behavior).
     * Returns false only when onlyGroups is non-empty but all are cancelled by exclusion.
     */
    public boolean hasEffectiveGroups() {
        if (onlyGroups.isEmpty()) {
            return true;
        }
        return !effectiveOnlyGroups.isEmpty();
    }

    public Set<String> getOnlyGroups() {
        return onlyGroups;
    }

    public Set<String> getExcludedGroups() {
        return excludedGroups;
    }

    /**
     * Logs warnings about conflicting groups and empty effective groups.
     * Both the buildless (CLI) and lockfile paths call this once to produce identical messages.
     */
    public void logGroupConflictWarnings(Logger logger) {
        if (!conflictingGroups.isEmpty()) {
            logger.warn(
                    "Dependency groups {} are present in both 'detect.uv.dependency.groups.only' and "
                    + "'detect.uv.dependency.groups.excluded'. The exclusion setting takes precedence; "
                    + "these groups will be excluded.",
                    conflictingGroups
            );
        }
        if (!onlyGroups.isEmpty() && effectiveOnlyGroups.isEmpty()) {
            logger.warn("No dependency groups remain to be scanned. Returning an empty BOM.");
        }
    }

    private Set<String> computeConflictingGroups() {
        if (excludedGroups.isEmpty() || onlyGroups.isEmpty()) {
            return Collections.emptySet();
        }
        return onlyGroups.stream()
                .filter(excludedGroups::contains)
                .collect(Collectors.toSet());
    }

    private Set<String> computeEffectiveOnlyGroups() {
        if (onlyGroups.isEmpty()) {
            return Collections.emptySet();
        }
        if (excludedGroups.isEmpty()) {
            return onlyGroups;
        }
        return onlyGroups.stream()
                .filter(group -> !excludedGroups.contains(group))
                .collect(Collectors.toSet());
    }
}

