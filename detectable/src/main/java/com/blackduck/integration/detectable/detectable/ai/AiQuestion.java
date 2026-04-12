package com.blackduck.integration.detectable.detectable.ai;

/**
 * Describes a single interactive question to ask the user during AI-assist pre-scan mode.
 *
 * <p>Adapters return a list of these from
 * {@link AiContextAdapter#getQuestions(AiContext)}; the orchestrator
 * (in the main {@code detect} module) handles the actual I/O so that this
 * class stays free of any application-level dependencies.</p>
 */
public class AiQuestion {

    /** The question type determines how the orchestrator reads the answer. */
    public enum Type {
        /** Answered with yes/no — maps to {@code askYesOrNo()}. */
        YES_NO,
        /** Free-text entry — maps to {@code askQuestion()}. */
        TEXT
    }

    /** The question text displayed to the user. */
    public final String prompt;

    /** How the answer is collected. */
    public final Type type;

    /**
     * Optional one-line hint shown before the prompt (e.g., what was detected in the build file).
     * May be {@code null} if there is nothing useful to show.
     */
    public final String hint;

    public AiQuestion(String prompt, Type type, String hint) {
        this.prompt = prompt;
        this.type   = type;
        this.hint   = hint;
    }

    /** Convenience constructor with no hint. */
    public AiQuestion(String prompt, Type type) {
        this(prompt, type, null);
    }
}

