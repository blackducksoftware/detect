package com.blackduck.integration.detectable.detectable.ai;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight container for Express-mode flag suggestions produced by an
 * {@link AiContextAdapter} without any user interaction.
 *
 * <p>In Express mode, each adapter analyses the project structure and returns
 * deterministic flag suggestions. This class carries those suggestions back to
 * the orchestrator ({@code AiAssistanceManager}), which merges them into the
 * final property source presented to the user.</p>
 *
 * <p>This class lives in the {@code detectable} module so that adapters can
 * return it without depending on the root module. Its structure intentionally
 * mirrors {@code LlmFlagSuggestion} in the root module so the two paths
 * (rule-based Express and LLM-based) converge seamlessly in the orchestrator.</p>
 */
public class ExpressFlagSuggestion {

    /** Detect property key to the value that should be applied for this scan. */
    public final Map<String, String> flags;

    /** Detect property key to a human-readable explanation for the flag choice. */
    public final Map<String, String> explanations;

    public ExpressFlagSuggestion(Map<String, String> flags, Map<String, String> explanations) {
        this.flags        = flags        != null ? flags        : new LinkedHashMap<>();
        this.explanations = explanations != null ? explanations : new LinkedHashMap<>();
    }

    /** Convenience factory that returns an empty (no-op) suggestion. */
    public static ExpressFlagSuggestion empty() {
        return new ExpressFlagSuggestion(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    /** Returns true if no flags were suggested. */
    public boolean isEmpty() {
        return flags.isEmpty();
    }
}

