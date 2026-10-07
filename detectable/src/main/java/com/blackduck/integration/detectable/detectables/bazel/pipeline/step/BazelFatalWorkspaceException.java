package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

/**
 * Thrown when {@link BazelCommandExecutor} detects that the Bazel workspace itself is fatally
 * misconfigured (not merely that a single query failed) — specifically, a repository rule
 * (e.g. {@code local_repository}, {@code git_repository}) points at a location with no
 * {@code MODULE.bazel}, {@code REPO.bazel}, or {@code WORKSPACE} file.
 *
 * <p>This is a structural problem with the scanned project, not a transient Bazel failure: every
 * subsequent Bazel command that touches the same dependency graph will hit the same error, and on
 * some Bazel versions (a known bug present in at least 7.1) a repeat encounter with this exact
 * failure can cause Bazel to hang indefinitely instead of failing fast a second time. Because of
 * that, {@link BazelCommandExecutor} deliberately does not retry/re-invoke Bazel once this signature
 * has been observed once in a given extraction — it fails fast immediately on every subsequent call.
 *
 * <p>This is an unchecked exception so it can propagate cleanly through methods (e.g.
 * {@code BazelEnvironmentAnalyzer.detectMode()}, {@code BazelGraphProber} probes) that do not declare
 * checked exceptions. Detectable entry points ({@code BazelV2Detectable#extract},
 * {@code BazelExtractor#extract}) catch it and convert it into a {@code DetectableException} so the
 * extraction fails outright rather than silently reporting an incomplete/incorrect BOM for a
 * misconfigured project.
 */
public class BazelFatalWorkspaceException extends RuntimeException {
    public BazelFatalWorkspaceException(String message) {
        super(message);
    }
}

