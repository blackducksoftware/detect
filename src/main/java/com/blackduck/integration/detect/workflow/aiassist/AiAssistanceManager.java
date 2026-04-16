package com.blackduck.integration.detect.workflow.aiassist;

import com.blackduck.integration.configuration.source.MapPropertySource;
import com.blackduck.integration.configuration.source.PropertySource;
import com.blackduck.integration.detectable.detectable.ai.AiContext;
import com.blackduck.integration.detectable.detectable.ai.AiContextAdapter;
import com.blackduck.integration.detectable.detectable.ai.AiQuestion;
import com.blackduck.integration.detectable.detectables.gradle.ai.GradleAiContextAdapter;
import com.blackduck.integration.detectable.detectables.maven.cli.MavenAiContextAdapter;
import com.blackduck.integration.detect.interactive.InteractiveWriter;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates the AI-assisted configuration pre-scan phase.
 *
 * <p>Workflow per applicable detector:</p>
 * <ol>
 *   <li>Check {@link AiContextAdapter#isApplicable} and {@link AiContextAdapter#isExtractable}</li>
 *   <li>Extract project context via {@link AiContextAdapter#extractContext}</li>
 *   <li>Load flags metadata JSON via {@link AiFlagsMetadataLoader}</li>
 *   <li>Send context + flags catalog to the LLM — the LLM decides which flags to apply and why</li>
 *   <li>Present the suggested command with per-flag explanations and ask accept / reject</li>
 *   <li>Return a {@link MapPropertySource} with the accepted flags at highest priority</li>
 * </ol>
 *
 * <p>To add a new detector: register its {@link AiContextAdapter} in {@link #buildAdapters()}.
 * No other changes are required — the LLM handles flag selection generically.</p>
 */
public class AiAssistanceManager {

    private static final String SEPARATOR = "─────────────────────────────────────────────────────────";
    private static final String PROPERTY_SOURCE_NAME = "ai-assist";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Gson gson;
    private final List<AiContextAdapter> adapters;

    public AiAssistanceManager(Gson gson) {
        this.gson     = gson;
        this.adapters = buildAdapters();
    }

    /**
     * Runs the full AI-assist pre-scan phase.
     *
     * @param sourceDirectory the project root to analyse
     * @param writer          terminal I/O (reused from interactive mode)
     * @param propertySources raw property sources — used to read LLM credentials
     * @return a {@link MapPropertySource} with AI-suggested flags, or an empty one if the user
     *         declined or no detectors were applicable
     */
    public MapPropertySource run(File sourceDirectory, InteractiveWriter writer, List<PropertySource> propertySources) {
        writer.println();
        writer.println("╔═════════════════════════════════════════════════════════╗");
        writer.println("║     Detect AI Assistance Quackstart — Pre-scan Mode     ║");
        writer.println("╚═════════════════════════════════════════════════════════╝");
        writer.println();
        writer.println("Analysing project at: " + sourceDirectory.getAbsolutePath());
        writer.println();

        //Strict Environment Variable check for LLM credentials
        String llmApiKey      = System.getenv("DETECT_LLM_API_KEY");
        String llmApiEndpoint = System.getenv("DETECT_LLM_API_ENDPOINT");
        String llmName        = System.getenv("DETECT_LLM_MODEL_NAME");
        boolean llmAvailable = llmApiKey != null && !llmApiKey.isEmpty() &&
                                llmApiEndpoint != null && !llmApiEndpoint.isEmpty() &&
                                llmName != null && !llmName.isEmpty();

        if (!llmAvailable) {
            writer.println("  LLM credentials not configured — running in MOCK mode.");
            writer.println("   (Set DETECT_LLM_API_KEY, DETECT_LLM_API_ENDPOINT, DETECT_LLM_MODEL_NAME for real LLM suggestions)");
            writer.println();
            // Continue — AiAssistanceLlmClient will use the mock suggestion path.
        }

        AiFlagsMetadataLoader flagsLoader  = new AiFlagsMetadataLoader();
        Map<String, String>   allFlags     = new LinkedHashMap<>();
        Map<String, String>   allExplain   = new LinkedHashMap<>();

        for (AiContextAdapter adapter : adapters) {
            if (!adapter.isApplicable(sourceDirectory)) {
                logger.debug("AI adapter '{}' not applicable for {}", adapter.getDetectorName(), sourceDirectory);
                continue;
            }
            if (!adapter.isExtractable(sourceDirectory)) {
                writer.println("[" + adapter.getDetectorName() + "] Project type detected but build tool not found on PATH — skipping.");
                writer.println();
                continue;
            }

            writer.println(" Detected: " + adapter.getDetectorName() + " project");
            writer.println("  Analysing build files...");

            AiContext context      = adapter.extractContext(sourceDirectory);
            String    flagsCatalog = flagsLoader.loadFlagsJson(adapter.getDetectorName());

            // Ask the detector-specific questions; hints come from the parsed build files
            writer.println();
            writer.println("Answer a few questions so we can configure the scan correctly:");
            writer.println();
            Map<String, String> qanda = collectUserAnswers(adapter.getQuestions(context), writer);

            writer.println("  Sending your answers + flags catalog to LLM for analysis...");
            writer.println();
            logger.info("Invoking LLM for {} with context:\n{}\nFlags catalog:\n{}",
                    adapter.getDetectorName(), context.toPromptString(), flagsCatalog);

            AiAssistanceLlmClient llmClient = new AiAssistanceLlmClient(llmApiKey, llmApiEndpoint, llmName, gson);
            LlmFlagSuggestion suggestion = llmClient.suggestFlags(
                adapter.getDetectorName(), qanda, flagsCatalog);

            logger.info("LLM suggestion for {}: flags={}, explanations={}",
                    adapter.getDetectorName(), suggestion.flags, suggestion.explanations);

            if (suggestion.isEmpty()) {
                writer.println("  LLM returned no flag suggestions for " + adapter.getDetectorName() + ".");
                writer.println();
            } else {
                allFlags.putAll(suggestion.flags);
                allExplain.putAll(suggestion.explanations);
            }
        }

        if (allFlags.isEmpty()) {
            writer.println("No AI configuration suggestions for this project.");
            writer.println();
            return new MapPropertySource(PROPERTY_SOURCE_NAME, new LinkedHashMap<>());
        }

        presentSuggestedCommand(allFlags, allExplain, writer);

        Boolean accepted = writer.askYesOrNo("Accept this configuration and run the scan?");
        writer.println();

        if (Boolean.TRUE.equals(accepted)) {
            writer.println("Configuration accepted. Starting scan with AI-suggested flags.");
            writer.println();
            return new MapPropertySource(PROPERTY_SOURCE_NAME, allFlags);
        } else {
            writer.println("Configuration declined. Running scan with original settings.");
            writer.println();
            return new MapPropertySource(PROPERTY_SOURCE_NAME, new LinkedHashMap<>());
        }
    }

    // ── Display ───────────────────────────────────────────────────────────────

    /**
     * Asks each question from the adapter interactively and returns a question → answer map.
     * YES_NO questions use {@link InteractiveWriter#askYesOrNo}; TEXT questions use
     * {@link InteractiveWriter#askQuestion}.
     */
    private Map<String, String> collectUserAnswers(List<AiQuestion> questions, InteractiveWriter writer) {
        Map<String, String> qanda = new LinkedHashMap<>();
        for (AiQuestion q : questions) {
            if (q.hint != null && !q.hint.isEmpty()) {
                writer.println("  ℹ  " + q.hint);
            }
            if (q.type == AiQuestion.Type.YES_NO) {
                Boolean ans = writer.askYesOrNo(q.prompt);
                qanda.put(q.prompt, Boolean.TRUE.equals(ans) ? "Yes" : "No");
            } else {
                String ans = writer.askQuestion(q.prompt);
                qanda.put(q.prompt, (ans != null && !ans.trim().isEmpty()) ? ans.trim() : "(skipped)");
            }
            writer.println();
        }
        return qanda;
    }

    private void presentSuggestedCommand(Map<String, String> flags,
            Map<String, String> explanations, InteractiveWriter writer) {

        writer.println(SEPARATOR);
        writer.println("AI-Suggested Detect Configuration:");
        writer.println();
        writer.println("  ./detect.sh \\");

        String[] keys = flags.keySet().toArray(new String[0]);
        for (int i = 0; i < keys.length; i++) {
            String suffix = (i < keys.length - 1) ? " \\" : "";
            writer.println("    --" + keys[i] + "=" + flags.get(keys[i]) + suffix);
        }

        if (!explanations.isEmpty()) {
            writer.println();
            writer.println("Why:");
            for (Map.Entry<String, String> entry : explanations.entrySet()) {
                writer.println("  ✔ " + entry.getKey() + "=" + flags.getOrDefault(entry.getKey(), "?")
                    + "  →  " + entry.getValue());
            }
        }

        writer.println(SEPARATOR);
        writer.println();
    }

    // ── Utilities ─────────────────────────────────────────────────────────────


    /** Returns the registered adapters. Add new detector adapters here. */
    private List<AiContextAdapter> buildAdapters() {
        List<AiContextAdapter> list = new ArrayList<>();
        list.add(new MavenAiContextAdapter());
        list.add(new GradleAiContextAdapter());
        // list.add(new BazelAiContextAdapter());  ← register here when Bazel is implemented
        return list;
    }
}





