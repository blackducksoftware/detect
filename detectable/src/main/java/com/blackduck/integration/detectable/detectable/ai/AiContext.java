package com.blackduck.integration.detectable.detectable.ai;

/**
 * Marker interface for AI context data extracted from a project by an
 * {@link com.blackduck.integration.detectable.detectable.ai.AiContextAdapter}.
 * Each detector's context implementation carries the signals needed to drive
 * AI-assisted configuration Q&A for that detector.
 */
public interface AiContext {
    /**
     * Returns a human-readable summary of the extracted context,
     * suitable for inclusion in an LLM prompt.
     */
    String toPromptString();
}


