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
     * The orchestrator handles the actual I/O — this method only defines what to ask.</p>
     *
     * @param context the context previously returned by {@link #extractContext(File)}
     * @return ordered list of questions; never {@code null}
     */
    List<AiQuestion> getQuestions(AiContext context);
}



