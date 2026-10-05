package com.blackduck.integration.detect.lifecycle.boot.product.version;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.blackduck.version.BlackDuckVersion;

class CompatibilityResolverTest {

    private static final Gson GSON = new Gson();
    private static final String MATRIX_URL = "https://docs.blackduck.com/example";

    private final CompatibilityResolver resolver = new CompatibilityResolver();

    @Test
    void exactMatchIsCompatible() {
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0", "11.5.1", "11.4.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "12.0.0");

        assertEquals(CompatibilityResult.Status.COMPATIBLE, result.getStatus());
        assertTrue(result.getMessage().contains("12.0.0"));
        assertTrue(result.getMessage().contains("2026.4.0"));
    }

    @Test
    void wildcardEntryMatchesAnyPatch() {
        CompatibilityMatrix matrix = matrixOf(
            row("2025.7.x", "11.2.1", "9.10.x")
        );

        assertEquals(CompatibilityResult.Status.COMPATIBLE,
            resolver.resolve(matrix, new BlackDuckVersion(2025, 7, 0), "9.10.0").getStatus());
        assertEquals(CompatibilityResult.Status.COMPATIBLE,
            resolver.resolve(matrix, new BlackDuckVersion(2025, 7, 0), "9.10.5").getStatus());
        assertEquals(CompatibilityResult.Status.COMPATIBLE,
            resolver.resolve(matrix, new BlackDuckVersion(2025, 7, 0), "9.10.15").getStatus());
    }

    @Test
    void wildcardEntryDoesNotMatchDifferentMinor() {
        // 9.9.0 is "known" (present in another row), but 2025.7.x only lists 9.10.x.
        CompatibilityMatrix matrix = matrixOf(
            row("2025.7.x", "11.0.0", "9.10.x"),
            row("2024.7.x", "9.9.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2025, 7, 0), "9.9.0");

        assertEquals(CompatibilityResult.Status.INCOMPATIBLE, result.getStatus());
        assertTrue(result.getMessage().contains("9.9.0"));
        assertTrue(result.getMessage().contains("2025.7.0"));
    }

    @Test
    void detectInMatrixButNotInRowIsIncompatibleWithFullMessage() {
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0", "11.5.0", "11.4.0"),
            row("2024.1.x", "8.11.x")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "8.11.5");

        assertEquals(CompatibilityResult.Status.INCOMPATIBLE, result.getStatus());
        String message = result.getMessage();
        assertTrue(message.contains("8.11.5"),   "message should include the running Detect version");
        assertTrue(message.contains("2026.4.0"), "message should include the connected server version");
        assertTrue(message.contains("12.0.0"),   "message should include a compatible Detect version");
        assertTrue(message.contains(MATRIX_URL), "message should include the matrix source URL");
    }

    @Test
    void unknownServerLineReturnsUnknown() {
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2027, 1, 0), "12.0.0");

        assertEquals(CompatibilityResult.Status.UNKNOWN, result.getStatus());
        assertTrue(result.getMessage().contains("2027.1.0"));
    }

    @Test
    void detectNewerThanMatrixReturnsUnknown() {
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0", "11.5.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "12.2.0");

        assertEquals(CompatibilityResult.Status.UNKNOWN, result.getStatus());
        assertTrue(result.getMessage().contains("12.2.0"));
        assertTrue(result.getMessage().toLowerCase().contains("newer"),
            "UNKNOWN message should explain the version is newer than the matrix");
    }

    @Test
    void detectOlderThanMatrixIsIncompatible() {
        // 7.9.0 is below every entry in the matrix (which bottoms out at 8.11.x). Older-than-matrix
        // must WARN — the customer is on an unsupported combination. See IDETECT-5310.
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0", "11.5.0"),
            row("2024.1.x", "8.11.x")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "7.9.0");

        assertEquals(CompatibilityResult.Status.INCOMPATIBLE, result.getStatus());
        String message = result.getMessage();
        assertTrue(message.contains("7.9.0"),    "message should include the running Detect version");
        assertTrue(message.contains("2026.4.0"), "message should include the connected server version");
        assertTrue(message.contains(MATRIX_URL), "message should include the matrix source URL");
    }

    @Test
    void snapshotSuffixIsStrippedBeforeLookup() {
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "12.0.0-SIGQA9-SNAPSHOT");

        assertEquals(CompatibilityResult.Status.COMPATIBLE, result.getStatus());
    }

    @Test
    void snapshotDevBuildNewerThanMatrixReturnsUnknown() {
        // Combines SNAPSHOT normalization with the newer-than-matrix skip.
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "12.2.0-SIGQA9-SNAPSHOT");

        assertEquals(CompatibilityResult.Status.UNKNOWN, result.getStatus());
        assertTrue(result.getMessage().contains("12.2.0"));
    }

    @Test
    void nullDetectVersionReturnsUnknown() {
        CompatibilityMatrix matrix = matrixOf(
            row("2026.4.x", "12.0.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), null);

        assertEquals(CompatibilityResult.Status.UNKNOWN, result.getStatus());
    }

    @Test
    void emptyMatrixReturnsUnknown() {
        CompatibilityMatrix matrix = matrixOf();

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "12.0.0");

        assertEquals(CompatibilityResult.Status.UNKNOWN, result.getStatus());
    }

    @Test
    void rowWithMalformedLabelIsSkipped() {
        // A garbage row label should not crash the resolver; a well-formed row later still wins.
        CompatibilityMatrix matrix = matrixOf(
            row("not-a-version", "12.0.0"),
            row("2026.4.x",      "12.0.0")
        );

        CompatibilityResult result = resolver.resolve(matrix, new BlackDuckVersion(2026, 4, 0), "12.0.0");

        assertEquals(CompatibilityResult.Status.COMPATIBLE, result.getStatus());
    }

    // ---- helpers ----

    private static CompatibilityMatrix matrixOf(CompatibilityRow... rows) {
        StringBuilder json = new StringBuilder("{\"schemaVersion\":1,\"matrixSourceUrl\":\"");
        json.append(MATRIX_URL).append("\",\"rows\":[");
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append(GSON.toJson(rows[i]));
        }
        json.append("]}");
        return GSON.fromJson(json.toString(), CompatibilityMatrix.class);
    }

    private static CompatibilityRow row(String label, String... detectVersions) {
        StringBuilder json = new StringBuilder("{\"blackDuckScaRelease\":\"");
        json.append(label).append("\",\"detectVersions\":[");
        for (int i = 0; i < detectVersions.length; i++) {
            if (i > 0) {
                json.append(",");
            }
            json.append("\"").append(detectVersions[i]).append("\"");
        }
        json.append("]}");
        return GSON.fromJson(json.toString(), CompatibilityRow.class);
    }
}
