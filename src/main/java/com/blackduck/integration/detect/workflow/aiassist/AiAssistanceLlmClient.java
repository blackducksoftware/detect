package com.blackduck.integration.detect.workflow.aiassist;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.blackduck.integration.log.SilentIntLogger;
import com.blackduck.integration.rest.HttpMethod;
import com.blackduck.integration.rest.HttpUrl;
import com.blackduck.integration.rest.body.StringBodyContent;
import com.blackduck.integration.rest.client.IntHttpClient;
import com.blackduck.integration.rest.proxy.ProxyInfo;
import com.blackduck.integration.rest.request.Request;
import com.blackduck.integration.rest.response.Response;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Calls an OpenAI-compatible LLM API to decide which Detect flags are appropriate
 * for the scanned project, and to explain each choice.
 *
 * <p>Uses the same three LLM properties already defined for Quack Patch:
 * {@code detect.llm.api.key}, {@code detect.llm.api.endpoint}, {@code detect.llm.name}.</p>
 *
 * <p>The LLM is given:
 * <ul>
 *   <li>project context (extracted from build files)</li>
 *   <li>the full flags metadata catalog for the detector</li>
 * </ul>
 * and is asked to return a JSON object in the form:
 * <pre>
 * {
 *   "flags":        { "detect.maven.excluded.scopes": "test", ... },
 *   "explanations": { "detect.maven.excluded.scopes": "test deps detected …", ... }
 * }
 * </pre></p>
 */
public class AiAssistanceLlmClient {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int TIMEOUT_SECONDS = 30;
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final String llmApiKey;
    private final String llmApiEndpoint;
    private final String llmName;
    private final Gson gson;

    public AiAssistanceLlmClient(String llmApiKey, String llmApiEndpoint, String llmName, Gson gson) {
        this.llmApiKey      = llmApiKey == null? "" : llmApiKey.trim();
        this.llmApiEndpoint = llmApiEndpoint == null? "" : llmApiEndpoint.trim().replaceAll("/+$", ""); // remove trailing slashes for consistent URL building
        this.llmName        = llmName;
        this.gson           = gson;
    }

    /**
     * Sends the user's Q&A answers and the flags catalog to the LLM and asks it to decide
     * which flags to apply and why.
     *
     * @param detectorName e.g. {@code "MAVEN"}
     * @param qanda        ordered map of question → user's answer, collected interactively
     * @param flagsMetadata JSON string from {@link AiFlagsMetadataLoader}
     * @return an {@link LlmFlagSuggestion} with recommended flags + per-flag explanations;
     *         never {@code null} — returns {@link LlmFlagSuggestion#empty()} on any failure
     */
    public LlmFlagSuggestion suggestFlags(String detectorName, Map<String, String> qanda, String flagsMetadata) {

        // ── DEMO MOCK MODE ──────────────────────────────────────────────────────────
        // LLM credentials are not configured — simulate the LLM decision locally
        // by reading the user's Q&A answers and mapping them to the right flags.
        // This lets the full interactive flow run end-to-end without a real LLM key.
        //
        // ✦ When real creds are available (detect.llm.api.key / detect.llm.api.endpoint /
        //   detect.llm.name are all set), remove this early-return block and the real
        //   HTTP call below takes over automatically.
        if (llmApiKey.isEmpty() || llmApiEndpoint.isEmpty() || llmName.isEmpty()) {
            logger.info("[AI Assist] No LLM credentials configured — using mock suggestion.");
            return buildMockSuggestion(qanda);
        }
        // ── END MOCK MODE — real LLM path below ─────────────────────────────────────

        // Build Q&A block for the prompt
        StringBuilder qandaBlock = new StringBuilder();
        for (Map.Entry<String, String> entry : qanda.entrySet()) {
            qandaBlock.append("  Q: ").append(entry.getKey())
                      .append("\n  A: ").append(entry.getValue())
                      .append("\n");
        }

//        String systemPrompt =
//            "You are a Black Duck Detect auto-config expert.\n"
//            + "Given the user's answers about their project and a catalog of available Detect "
//            + "configuration flags, decide which flags should be applied to optimise the scan.\n"
//            + "Return ONLY valid JSON — no markdown fences, no extra text — in exactly this format:\n"
//            + "{\n"
//            + "  \"flags\":        { \"<detect-property-key>\": \"<value>\", ... },\n"
//            + "  \"explanations\": { \"<detect-property-key>\": \"<one-sentence reason>\", ... }\n"
//            + "}\n"
//            + "Only include flags that are genuinely relevant based on the user's answers. "
//            + "If no flags are needed, return {\"flags\":{},\"explanations\":{}}.";
//
//        String userPrompt =
//            "Detected: " + detectorName + "\n\n"
//            + "User responses:\n" + qandaBlock + "\n"
//            + "Available flags catalog (JSON):\n" + flagsMetadata + "\n\n"
//            + "Based on the user's answers, select the appropriate flags from the catalog "
//            + "and return your decision as the JSON described in the system prompt.";

        String systemPrompt =
                "You are a Senior Black Duck Software Composition Analysis (SCA) Engineer. "
                        + "Your task is to configure the Black Duck Detect CLI based on a user's environment answers.\n\n"
                        + "RULES:\n"
                        + "1. You are provided with a strictly allowed catalog of Detect flags. DO NOT invent or guess flags outside of this catalog.\n"
                        + "2. Review the user's Q&A. If their answer requires a flag from the catalog, apply it.\n"
                        + "3. If an answer is 'No' or '(skipped)', do NOT apply the associated flag.\n"
                        + "4. You MUST output your response in strict JSON format. Do not use Markdown formatting (like ```json).\n\n"
                        + "JSON SCHEMA:\n"
                        + "{\n"
                        + "  \"analysis\": \"<Write a brief, step-by-step reasoning evaluating the Q&A against the catalog to decide which flags are necessary>\",\n"
                        + "  \"flags\": { \"<detect.property.key>\": \"<exact_value>\" },\n"
                        + "  \"explanations\": { \"<detect.property.key>\": \"<One clear sentence explaining to the user why this was added based on their answer>\" }\n"
                        + "}\n"
                        + "If no flags are needed, leave the 'flags' and 'explanations' objects empty.";

        String userPrompt =
                "Target Detector: " + detectorName + "\n\n"
                        + "Allowed Flags Catalog:\n" + flagsMetadata + "\n\n"
                        + "User Q&A Responses:\n" + qandaBlock + "\n\n"
                        + "Return the JSON output now.";


        try {
            String requestBody = buildRequestBody(systemPrompt, userPrompt);

            IntHttpClient httpClient = new IntHttpClient(
                new SilentIntLogger(), gson, TIMEOUT_SECONDS, true, ProxyInfo.NO_PROXY_INFO);

            Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.CONTENT_TYPE, "application/json");
            headers.put(HttpHeaders.AUTHORIZATION, "Bearer " + llmApiKey);

            Request request = new Request(
                new HttpUrl(llmApiEndpoint + CHAT_COMPLETIONS_PATH),
                HttpMethod.POST,
                null,
                new HashMap<>(),
                headers,
                StringBodyContent.json(requestBody)
            );

            try (Response response = httpClient.execute(request)) {
                if (response.isStatusCodeSuccess()) {
                    String content = extractMessageContent(response.getContentString());
                    return content != null ? parseSuggestion(content) : LlmFlagSuggestion.empty();
                } else {
                    logger.warn("LLM API returned status {}: {}", response.getStatusCode(), response.getStatusMessage());
                    return LlmFlagSuggestion.empty();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to contact LLM API at {}: {}", llmApiEndpoint, e.getMessage());
            logger.debug("LLM API error details", e);
            return LlmFlagSuggestion.empty();
        }
    }

    /**
     * Mock LLM decision engine used when no credentials are configured.
     *
     * <p>Reads the user's Q&A answers and maps them to the correct Detect flags,
     * mimicking exactly what the real LLM would return. This lets the full
     * interactive flow run end-to-end during demos without a live LLM key.</p>
     */
    private LlmFlagSuggestion buildMockSuggestion(Map<String, String> qanda) {
        Map<String, String> flags        = new LinkedHashMap<>();
        Map<String, String> explanations = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : qanda.entrySet()) {
            String q = entry.getKey().toLowerCase();
            String a = entry.getValue().trim();

            // Q1 — test dependencies (question is now "Exclude test deps?")
            if (q.contains("test") && a.equalsIgnoreCase("yes")) {
                flags.put("detect.maven.excluded.scopes", "test");
                explanations.put("detect.maven.excluded.scopes",
                    "User chose to exclude test dependencies — produces a clean production-only BOM.");
            }

            // Q2 — Maven profile
            if (q.contains("profile") && !a.equalsIgnoreCase("(skipped)") && !a.isEmpty()) {
                flags.put("detect.maven.build.command", "-P" + a);
                explanations.put("detect.maven.build.command",
                    "User activated the '" + a + "' profile — ensures the correct environment-specific "
                    + "dependencies are resolved during the scan.");
            }

            // Q3 — exclude sub-modules
            if (q.contains("module") && !a.equalsIgnoreCase("(skipped)") && !a.isEmpty()) {
                flags.put("detect.maven.excluded.modules", a);
                explanations.put("detect.maven.excluded.modules",
                    "User excluded '" + a + "' — these are test/utility modules that should not "
                    + "appear in the production Bill of Materials.");
            }
        }

        return new LlmFlagSuggestion(flags, explanations);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);

        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(systemMessage);
        messages.add(userMessage);

        JsonObject body = new JsonObject();
        body.addProperty("model", llmName);
        body.addProperty("temperature", 0.1); // deterministic output
        body.add("messages", messages);

        return gson.toJson(body);
    }

    private String extractMessageContent(String responseJson) {
        try {
            JsonObject root = JsonParser.parseString(responseJson).getAsJsonObject();
            return root.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
        } catch (Exception e) {
            logger.warn("Failed to parse LLM response JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses the LLM's JSON reply into an {@link LlmFlagSuggestion}.
     * Gracefully returns {@link LlmFlagSuggestion#empty()} if the content cannot be parsed.
     */
    private LlmFlagSuggestion parseSuggestion(String content) {
        // Strip optional markdown fences the LLM may have added despite instructions
        String json = content.trim()
            .replaceAll("^```(?:json)?\\s*", "")
            .replaceAll("\\s*```$", "")
            .trim();

        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            Map<String, String> flags        = parseStringMap(root, "flags");
            Map<String, String> explanations = parseStringMap(root, "explanations");

            return new LlmFlagSuggestion(flags, explanations);
        } catch (Exception e) {
            logger.warn("Could not parse LLM flag suggestion JSON: {}", e.getMessage());
            logger.debug("Raw LLM content was:\n{}", content);
            return LlmFlagSuggestion.empty();
        }
    }

    private Map<String, String> parseStringMap(JsonObject root, String memberName) {
        Map<String, String> map = new LinkedHashMap<>();
        if (!root.has(memberName) || root.get(memberName).isJsonNull()) {
            return map;
        }
        JsonObject obj = root.getAsJsonObject(memberName);
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            map.put(entry.getKey(), entry.getValue().getAsString());
        }
        return map;
    }
}



