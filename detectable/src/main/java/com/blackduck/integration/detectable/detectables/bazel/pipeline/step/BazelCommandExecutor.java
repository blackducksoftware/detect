package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.detectable.detectable.executable.ExecutableFailedException;
import com.blackduck.integration.executable.ExecutableOutput;

public class BazelCommandExecutor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DetectableExecutableRunner executableRunner;
    private final File workspaceDir;
    private final ExecutableTarget bazelExe;
    private static final String BAZEL = "bazel";

    // Default timeout used by the backward-compatible 3-arg constructor (unit tests, and any
    // caller that does not have a configured value available, e.g. the legacy BazelExtractor
    // path). Production callers that have access to DetectProperties should use the 4-arg
    // constructor and pass detect.bazel.command.timeout instead.
    public static final int DEFAULT_COMMAND_TIMEOUT_SECONDS = 1800;

    private final int commandTimeoutSeconds;

    // Bazel commands are run on a bounded-lifetime daemon-thread executor so a hung subprocess
    // (e.g. Process.waitFor() blocking forever on a stalled repository fetch) can be bounded with
    // Future#get(timeout) instead of blocking the calling thread indefinitely. Daemon threads
    // ensure a timed-out worker never prevents JVM shutdown.
    private static final ThreadFactory DAEMON_THREAD_FACTORY;
    private static final AtomicLong THREAD_COUNTER = new AtomicLong();
    static {
        DAEMON_THREAD_FACTORY = runnable -> {
            Thread thread = new Thread(runnable, "bazel-command-executor-" + THREAD_COUNTER.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }
    private final ExecutorService commandExecutor = Executors.newCachedThreadPool(DAEMON_THREAD_FACTORY);

    // Memoization of read-only Bazel command results for the lifetime of a single extraction.
    // The workspace is never modified between Bazel invocations during a scan, so an identical
    // command is deterministic — we cache its raw ExecutableOutput and reuse it. Caching at this
    // lowest layer (executeToleratingExitCode) means every read-only path that funnels through it —
    // executeQueryToString (query/cquery) and executeModCommandToString (mod graph / show_repo) —
    // shares one cache. This eliminates redundant re-execution of, e.g., the kind(.*library, deps(T))
    // query (issued during probing and again during extraction) and 'mod graph --output json'
    // (issued by both HttpFamilyProber's fast path and BzlmodBcrExtractor), without relying on
    // Bazel's server-side analysis cache (which may be evicted by intervening commands).
    // Keyed on the exact argument list; only successful (non-throwing) results are cached, and each
    // caller still applies its own exit-code interpretation to the shared raw output.
    private final Map<List<String>, ExecutableOutput> rawOutputCache = new HashMap<>();

    /**
     * Backward-compatible constructor using {@link #DEFAULT_COMMAND_TIMEOUT_SECONDS}.
     */
    public BazelCommandExecutor(DetectableExecutableRunner executableRunner, File workspaceDir, ExecutableTarget bazelExe) {
        this(executableRunner, workspaceDir, bazelExe, DEFAULT_COMMAND_TIMEOUT_SECONDS);
    }

    /**
     * @param commandTimeoutSeconds Maximum time, in seconds, to wait for any single Bazel command
     *                              to complete before aborting it. See {@code detect.bazel.command.timeout}.
     */
    public BazelCommandExecutor(DetectableExecutableRunner executableRunner, File workspaceDir, ExecutableTarget bazelExe, int commandTimeoutSeconds) {
        this.executableRunner = executableRunner;
        this.workspaceDir = workspaceDir;
        this.bazelExe = bazelExe;
        this.commandTimeoutSeconds = commandTimeoutSeconds > 0 ? commandTimeoutSeconds : DEFAULT_COMMAND_TIMEOUT_SECONDS;
    }


    public Optional<String> executeToString(List<String> args) throws ExecutableFailedException {
        // Route through the cached raw-output path (executeToleratingExitCode) so an identical
        // read-only command issued elsewhere in the extraction (e.g. the same maven cquery run by a
        // pipeline via executeQueryToString) is reused instead of re-executed.
        //
        // Semantics are preserved exactly: this method is strict — ANY non-zero exit code is a hard
        // failure surfaced as ExecutableFailedException (it does NOT tolerate exit 3, unlike
        // executeQueryToString). A launch failure still propagates as the RuntimeException thrown by
        // executeToleratingExitCode; callers (BazelGraphProber probes, HttpFamilyProber) catch broadly.
        ExecutableOutput result = executeToleratingExitCode(args);
        if (result.getReturnCode() != 0) {
            throw new ExecutableFailedException(
                ExecutableUtils.createFromTarget(workspaceDir, bazelExe, args),
                result
            );
        }
        String cmdStdErr = result.getErrorOutput();
        if (cmdStdErr != null && cmdStdErr.contains("ERROR")) {
            logger.warn("Bazel error: {}", cmdStdErr.trim());
        }
        String cmdStdOut = result.getStandardOutput();
        if (StringUtils.isBlank(cmdStdOut)) {
            logger.debug("bazel command produced no output");
            return Optional.empty();
        }
        return Optional.of(cmdStdOut);
    }

    /**
     * Executes a Bazel {@code mod} command (e.g., {@code mod show_repo}, {@code mod graph}),
     * accepting stdout even when the exit code is non-zero.
     *
     * <p>This is intentionally permissive for {@code mod} commands because a broken but unrelated
     * module extension (e.g., {@code bazel_jar_jar+} on Bazel 9) poisons the process exit code to 2
     * while the requested output is still fully and correctly written to stdout. For these commands,
     * stdout presence is the authoritative success signal — if Bazel resolved the repo/graph it
     * writes the definition to stdout; if it genuinely failed (e.g., "no such repo") stdout is empty.</p>
     *
     * <p><b>NOT</b> safe for {@code query}/{@code cquery}: partial query output on a non-zero exit
     * could be misleading (some targets resolved before the failure). Use {@link #executeToString}
     * for those commands.</p>
     *
     * @param args Bazel command arguments (should start with "mod")
     * @return stdout content if non-empty, {@link Optional#empty()} otherwise
     */
    public Optional<String> executeModCommandToString(List<String> args) {
        ExecutableOutput result = executeToleratingExitCode(args);
        int exitCode = result.getReturnCode();
        if (exitCode != 0) {
            String stderr = result.getErrorOutput();
            String firstStderrLine = "";
            if (!StringUtils.isBlank(stderr)) {
                String[] lines = stderr.split("\\r?\\n", 2);
                firstStderrLine = lines.length > 0 ? lines[0] : "(none)";
            } else {
                firstStderrLine = "(none)";
            }
            logger.debug("Bazel mod command returned exit code {}; first stderr line: {}", exitCode, firstStderrLine);
        }
        String stdout = result.getStandardOutput();
        if (StringUtils.isBlank(stdout)) {
            logger.debug("Bazel mod command produced no stdout output");
            return Optional.empty();
        }
        return Optional.of(stdout);
    }

    /**
     * Executes a Bazel {@code query} or {@code cquery} command, tolerating exit code 3.
     *
     * <p>Bazel exit code 3 is the documented "partial success with {@code --keep_going}" signal:
     * some repository fetches failed during workspace loading, but the query itself completed
     * and wrote valid dependency data to stdout. This is the expected outcome when a workspace
     * contains cross-platform or stale repository declarations (e.g., Windows-only pip repos
     * declared unconditionally on a Linux CI host).</p>
     *
     * <ul>
     *   <li>Exit code 0 → clean success, return stdout</li>
     *   <li>Exit code 3 + non-empty stdout → partial success, log warning, return stdout</li>
     *   <li>Exit code 3 + empty stdout → return empty</li>
     *   <li>Any other non-zero → throw {@link ExecutableFailedException} using the already-obtained result (no re-invocation)</li>
     * </ul>
     *
     * @param args Bazel command arguments
     * @return stdout content if non-empty, {@link Optional#empty()} otherwise
     * @throws ExecutableFailedException if exit code is non-zero and not 3
     */
    public Optional<String> executeQueryToString(List<String> args) throws ExecutableFailedException {
        // Raw output is served from the shared per-extraction cache (see executeToleratingExitCode);
        // this method re-applies query exit-code interpretation to it on every call.
        ExecutableOutput result = executeToleratingExitCode(args);
        int exitCode = result.getReturnCode();

        if (exitCode == BAZEL_EXIT_CODE_PARTIAL_SUCCESS) {
            String stdout = result.getStandardOutput();
            if (!StringUtils.isBlank(stdout)) {
                logger.warn(
                    "Bazel query exited with code 3 (partial success with --keep_going). "
                    + "Some repository fetches failed — dependency results may be incomplete. "
                    + "Check Bazel stderr output above for details."
                );
                return Optional.of(stdout);
            }
            logger.debug("Bazel query exited with code 3 but produced no stdout; treating as empty result.");
            return Optional.empty();
        }

        if (exitCode != 0) {
            throw new ExecutableFailedException(
                ExecutableUtils.createFromTarget(workspaceDir, bazelExe, args),
                result
            );
        }

        String stderr = result.getErrorOutput();
        if (stderr != null && stderr.contains("ERROR")) {
            logger.warn("Bazel error: {}", stderr.trim());
        }
        String stdout = result.getStandardOutput();
        if (StringUtils.isBlank(stdout)) {
            logger.debug("bazel command produced no output");
            return Optional.empty();
        }
        return Optional.of(stdout);
    }

    // Bazel's documented exit code for partial success with --keep_going
    private static final int BAZEL_EXIT_CODE_PARTIAL_SUCCESS = 3;

    /**
     * Executes a Bazel command and returns the raw {@link ExecutableOutput} <b>without throwing on a
     * non-zero exit code</b>, so the caller can inspect the return code, stdout, and stderr itself.
     * This is the counterpart to {@link #executeToString} / {@code executeSuccessfully}, which treat
     * any non-zero exit as a hard failure. Probing commands need the opposite: a non-zero exit is
     * frequently expected and still carries usable output (e.g. a broken but unrelated module
     * extension poisons the exit code to 2 while the requested graph is fully written to stdout).
     *
     * <p><b>Why this method still throws:</b> the {@code catch} below only triggers on an
     * {@link com.blackduck.integration.executable.ExecutableRunnerException} — i.e. Bazel could not be
     * launched at all (executable missing, IO error, interrupted). That is a genuinely unrecoverable
     * environment problem, distinct from "Bazel ran and returned a non-zero exit code." There is no
     * meaningful {@code ExecutableOutput} to hand back when the process never started, and returning a
     * fabricated empty result would silently mask a broken setup as an empty query result. So a
     * launch failure is surfaced as a {@link RuntimeException}; a non-zero exit is not.
     * <p><b>Caching:</b> successful results are memoized per extraction in {@link #rawOutputCache}
     * keyed on the exact argument list, so an identical read-only command issued more than once
     * (e.g. during probing and again during extraction) runs Bazel only once. A launch failure is
     * never cached.
     *
     * <p><b>Timeout / hang protection:</b> the command is run on a bounded-lifetime worker thread
     * and bounded with {@link Future#get(long, TimeUnit)} using {@code commandTimeoutSeconds}
     * (see {@code detect.bazel.command.timeout}). If Bazel hangs — e.g. its subprocess stalls
     * fetching a broken {@code local_repository}/{@code git_repository} instead of failing fast —
     * the worker thread is interrupted and the timeout is surfaced as a {@link RuntimeException}
     * instead of blocking the scan forever. Because Bazel uses a persistent client/server model,
     * interrupting/abandoning the client-side thread does not guarantee the server-side fetch also
     * stops; the logged error advises a manual {@code bazel shutdown} if this occurs repeatedly.
     *
     * @param args Bazel command arguments
     * @return ExecutableOutput containing return code, stdout, and stderr
     */
    public ExecutableOutput executeToleratingExitCode(List<String> args) {
        List<String> cacheKey = (args == null) ? Collections.emptyList() : new ArrayList<>(args);
        ExecutableOutput cached = rawOutputCache.get(cacheKey);
        if (cached != null) {
            logger.debug("Reusing cached Bazel command result for args: {}", cacheKey);
            return cached;
        }

        String command = (bazelExe != null ? bazelExe.toCommand() : BAZEL) + " " + String.join(" ", args == null ? Collections.emptyList() : args);
        Future<ExecutableOutput> future = commandExecutor.submit(() ->
            executableRunner.execute(ExecutableUtils.createFromTarget(workspaceDir, bazelExe, args))
        );
        try {
            ExecutableOutput output = future.get(commandTimeoutSeconds, TimeUnit.SECONDS);
            rawOutputCache.put(cacheKey, output);
            return output;
        } catch (TimeoutException e) {
            future.cancel(true); // interrupt the worker thread, unblocking any local Process.waitFor()
            String msg = String.format(
                "Bazel command timed out after %d second(s) and was aborted: '%s'. "
                + "This usually means Bazel itself is stalled (for example, fetching a broken local_repository/git_repository "
                + "instead of failing fast) rather than Detect. Increase detect.bazel.command.timeout if this command is "
                + "simply slow on a large workspace. Because Bazel uses a persistent background server, the stalled fetch "
                + "may still be running server-side; running 'bazel shutdown' in the project directory may be required "
                + "before retrying the scan.",
                commandTimeoutSeconds, command
            );
            logger.error(msg);
            throw new RuntimeException(msg, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String msg = String.format("Interrupted while waiting for Bazel command '%s'", command);
            logger.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            String msg = String.format("Failed to execute Bazel command '%s': %s", command, cause.getMessage());
            logger.error(msg, cause);
            throw new RuntimeException(msg, cause);
        }
    }
}
