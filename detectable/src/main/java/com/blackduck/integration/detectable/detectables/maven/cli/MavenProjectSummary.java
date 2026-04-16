package com.blackduck.integration.detectable.detectables.maven.cli;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Structured summary of a Maven project (root + all sub-modules) extracted for
 * the QuackStart Express mode.
 *
 * <p>Only build metadata is captured — no source code. Target: under 1,500 tokens
 * for a typical multi-module project.</p>
 */
public class MavenProjectSummary {

    /** Summary of a single Maven module (one pom.xml). */
    public static class ModuleSummary {
        public final String artifactId;
        public final String packaging;
        public final String parentArtifactId;
        /** Scope → list of dependency artifactIds. Only non-empty scopes are stored. */
        public final Map<String, List<String>> depsByScope;
        /** Profile id → list of compile-scope dep artifactIds declared inside that profile. */
        public final Map<String, List<String>> profileCompileDeps;
        public final List<String> submodules;

        public ModuleSummary(
                String artifactId,
                String packaging,
                String parentArtifactId,
                Map<String, List<String>> depsByScope,
                Map<String, List<String>> profileCompileDeps,
                List<String> submodules) {
            this.artifactId         = artifactId;
            this.packaging          = packaging;
            this.parentArtifactId   = parentArtifactId;
            this.depsByScope        = depsByScope;
            this.profileCompileDeps = profileCompileDeps;
            this.submodules         = submodules;
        }
    }

    private final List<ModuleSummary> modules = new ArrayList<>();

    public void addModule(ModuleSummary module) { modules.add(module); }
    public List<ModuleSummary> getModules()     { return modules; }
    public boolean isEmpty()                    { return modules.isEmpty(); }

    /**
     * Renders the project summary as compact plain text for inclusion in an LLM prompt.
     * Plain text uses fewer tokens than JSON.
     */
    public String toPromptString() {
        if (modules.isEmpty()) {
            return "PROJECT SUMMARY — Maven\n(no modules found)\n";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("PROJECT SUMMARY — Maven\n");
        sb.append("═══════════════════════\n");

        for (int i = 0; i < modules.size(); i++) {
            ModuleSummary m = modules.get(i);
            String label = (i == 0) ? "Root" : "Module";
            sb.append(label).append(": ").append(m.artifactId)
              .append("  [").append(m.packaging).append("]");
            if (m.parentArtifactId != null) {
                sb.append("  parent: ").append(m.parentArtifactId);
            }
            sb.append("\n");

            if (!m.profileCompileDeps.isEmpty()) {
                sb.append("  Profiles: ").append(String.join(", ", m.profileCompileDeps.keySet())).append("\n");
                for (Map.Entry<String, List<String>> e : m.profileCompileDeps.entrySet()) {
                    sb.append("  Profile[").append(e.getKey()).append("]  compile-deps: ")
                      .append(String.join(", ", e.getValue())).append("\n");
                }
            }
            if (!m.submodules.isEmpty()) {
                sb.append("  Sub-modules: ").append(String.join(", ", m.submodules)).append("\n");
            }
            for (Map.Entry<String, List<String>> e : m.depsByScope.entrySet()) {
                sb.append("  ").append(e.getKey()).append(": ")
                  .append(String.join(", ", e.getValue())).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

