package com.blackduck.integration.detectable.detectable.ai;

import java.io.File;
import java.util.List;

/**
 * Lightweight adapter that lives alongside a {@link com.blackduck.integration.detectable.Detectable}
 * in the {@code detectable} module and provides AI-assist context extraction for that detector.
 *
 * <p>Mirrors the detectable lifecycle (applicable / extractable) but can be instantiated
 * without the full dependency-injection graph required by the real detectable.</p>
 *
 * <p>To add AI support for a new detector:</p>
 * <ol>
 *   <li>Create {@code XyzAiContext} implementing {@link AiContext} in the detector's package.</li>
 *   <li>Create {@code XyzAiContextAdapter} implementing this interface in the same package.</li>
 *   <li>Register the adapter in {@code AiAssistanceManager}.</li>
 * </ol>
 */
public interface AiContextAdapter {

    /**
     * Mirrors the detectable's {@code applicable()} check.
     * Should be lightweight — file presence checks only, no I/O.
     *
     * @param sourceDirectory the project root directory
     * @return true if this detector's characteristic files are present
     */
    boolean isApplicable(File sourceDirectory);

    /**
     * Mirrors the detectable's {@code extractable()} check.
     * Checks that the required build tool is available on the system PATH.
     *
     * @param sourceDirectory the project root directory
     * @return true if the required build executable is available
     */
    boolean isExtractable(File sourceDirectory);

    /**
     * Reads project files and extracts the minimal signals needed to drive
     * AI-assisted Q&A and flag suggestion.
     *
     * @param sourceDirectory the project root directory
     * @return a populated {@link AiContext} for this detector
     */
    AiContext extractContext(File sourceDirectory);

    /**
     * @return the detector name, e.g. {@code "MAVEN"}, {@code "BAZEL"}.
     *         Used to load the corresponding flags metadata JSON.
     */
    String getDetectorName();

    /**
     * Returns the ordered list of interactive questions to ask the user for this detector.
     *
     * <p>The {@code context} extracted from the build files is passed so that implementations
     * can include project-specific hints (e.g., list of detected profile names).
     * The orchestrator handles the actual I/O -- this method only defines what to ask.</p>
     *
     * @param context the context previously returned by {@link #extractContext(File)}
     * @return ordered list of questions; never {@code null}
     */
    List<AiQuestion> getQuestions(AiContext context);

    /**
     * Express mode: derives flag suggestions directly from the project structure
     * without any user Q&A or LLM call.
     *
     * <p>This is the extension point for QuackStart Express. Each adapter that supports
     * Express mode overrides this method with its own analysis logic. The orchestrator
     * ({@code AiAssistanceManager.runExpress}) iterates over all registered adapters
     * and merges their suggestions -- it never needs to know which detectors exist.</p>
     *
     * <h3>Current design (rule-based):</h3>
     * <p>Implementations inspect build files via {@link #extractContext(File)} and apply
     * deterministic rules to decide which flags are needed. This is fast, requires no
     * network calls, and works without LLM credentials.</p>
     *
     * <h3>Future enhancement (LLM-backed):</h3>
     * <p>To switch a specific adapter to LLM-backed analysis, replace the body of its
     * override with an LLM client call. The method signature and return type stay the
     * same, so the orchestrator and all other adapters are unaffected.</p>
     *
     * <p>The default implementation returns an empty suggestion, which signals that
     * this adapter does not yet support Express mode. The orchestrator silently
     * skips adapters that return empty suggestions.</p>
     *
     * @param sourceDirectory the project root directory
     * @return an {@link ExpressFlagSuggestion} with recommended flags and explanations;
     *         never {@code null}
     */
    default ExpressFlagSuggestion suggestExpressFlags(File sourceDirectory) {
        return ExpressFlagSuggestion.empty();
    }
}



