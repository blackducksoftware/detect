package com.blackduck.integration.detect.workflow.aiassist;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured output from the LLM flag-suggestion call.
 *
 * <p>The LLM is asked to return a JSON object with two maps:</p>
 * <ul>
 *   <li>{@link #flags}        — flag property key → value to apply</li>
 *   <li>{@link #explanations} — flag property key → one-sentence reason why it was selected</li>
 * </ul>
 */
public class LlmFlagSuggestion {

    /** Detect property key → value that should be applied for this scan. */
    public final Map<String, String> flags;

    /** Detect property key → human-readable explanation for the flag choice. */
    public final Map<String, String> explanations;

    public LlmFlagSuggestion(Map<String, String> flags, Map<String, String> explanations) {
        this.flags        = flags        != null ? flags        : new LinkedHashMap<>();
        this.explanations = explanations != null ? explanations : new LinkedHashMap<>();
    }

    /** Convenience factory that returns an empty (no-op) suggestion. */
    public static LlmFlagSuggestion empty() {
        return new LlmFlagSuggestion(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public boolean isEmpty() {
        return flags.isEmpty();
    }
}

