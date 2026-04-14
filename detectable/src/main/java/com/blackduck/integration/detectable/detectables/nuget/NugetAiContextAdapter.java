package com.blackduck.integration.detectable.detectables.nuget;

import com.blackduck.integration.detectable.detectable.ai.AiContext;
import com.blackduck.integration.detectable.detectable.ai.AiContextAdapter;
import com.blackduck.integration.detectable.detectable.ai.AiQuestion;
import com.blackduck.integration.detectable.detectables.nuget.future.ParsedProject;
import com.blackduck.integration.detectable.detectables.nuget.future.SolutionParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI context adapter for the NuGet detector.
 * Co-located with {@link NugetSolutionDetectable} in the {@code detectables/nuget} package.
 *
 * <p>Can be instantiated without any injected dependencies — only requires a source directory.
 * Mirrors the applicable/extractable checks of the real detectable without depending on it.</p>
 *
 * <p>Extracts two signals:</p>
 * <ul>
 *   <li>Dev-only dependencies ({@code <PackageReference ... PrivateAssets="all" />} in .csproj files)</li>
 *   <li>Project names from the .sln file, split into all projects and test-looking projects</li>
 * </ul>
 */
public class NugetAiContextAdapter implements AiContextAdapter {

    private static final String SLN_EXTENSION = ".sln";
    private static final Pattern TEST_PROJECT_PATTERN = Pattern.compile(
        ".*\\.(Tests?|IntegrationTests?|TestUtils|UnitTests?|FunctionalTests?)$",
        Pattern.CASE_INSENSITIVE
    );
    /** Matches PrivateAssets="all" in a PackageReference element (inline or child). */
    private static final Pattern PRIVATE_ASSETS_ALL_PATTERN = Pattern.compile(
        "PrivateAssets\\s*=\\s*\"all\"|<PrivateAssets>\\s*all\\s*</PrivateAssets>",
        Pattern.CASE_INSENSITIVE
    );

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Applicable if any {@code *.sln} file exists in the source directory.
     * Mirrors {@link NugetSolutionDetectable}'s supported solution patterns.
     */
    @Override
    public boolean isApplicable(File sourceDirectory) {
        File[] files = sourceDirectory.listFiles();
        if (files == null) return false;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(SLN_EXTENSION)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extractable if {@code dotnet} is found on the system PATH.
     * The NuGet inspector requires the .NET SDK to function.
     */
    @Override
    public boolean isExtractable(File sourceDirectory) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"dotnet", "--version"});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("Interrupted while checking for dotnet on PATH");
            return false;
        } catch (Exception e) {
            logger.debug("dotnet not found on PATH: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Parses the {@code .sln} file using the existing {@link SolutionParser} and scans
     * referenced {@code .csproj} files for dev-only dependencies.
     */
    @Override
    public NugetAiContext extractContext(File sourceDirectory) {
        File slnFile = findSlnFile(sourceDirectory);
        if (slnFile == null) {
            logger.warn("Could not find .sln file in {} for AI context extraction", sourceDirectory);
            return new NugetAiContext(false, new ArrayList<>(), new ArrayList<>());
        }

        try {
            // Parse .sln to get project names
            List<String> solutionLines = Files.readAllLines(slnFile.toPath());
            SolutionParser parser = new SolutionParser();
            List<ParsedProject> parsedProjects = parser.projectsFromSolution(solutionLines);

            List<String> projectNames = parsedProjects.stream()
                .map(ParsedProject::getName)
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());

            // Identify test projects by naming convention
            List<String> testProjectNames = projectNames.stream()
                .filter(name -> TEST_PROJECT_PATTERN.matcher(name).matches())
                .collect(Collectors.toList());

            // Scan .csproj files for dev-only dependencies (PrivateAssets="all")
            boolean hasDevDependencies = detectDevDependencies(sourceDirectory, parsedProjects);

            return new NugetAiContext(hasDevDependencies, projectNames, testProjectNames);

        } catch (Exception e) {
            logger.warn("Failed to parse .sln for AI context extraction: {}", e.getMessage());
            logger.debug("AI context extraction error details", e);
            return new NugetAiContext(false, new ArrayList<>(), new ArrayList<>());
        }
    }

    @Override
    public String getDetectorName() {
        return "NUGET";
    }

    /**
     * Returns the two NuGet-specific questions, with hints populated from the
     * signals found in the .sln and .csproj files.
     */
    @Override
    public List<AiQuestion> getQuestions(AiContext context) {
        NugetAiContext ctx = (NugetAiContext) context;
        List<AiQuestion> questions = new ArrayList<>();

        // Q1 — boolean: exclude dev dependencies from the BOM?
        String devHint = ctx.hasDevDependencies
            ? "Dev-only dependencies detected (analyzers/source generators with PrivateAssets=all)."
            : "No dev-only dependencies detected in .csproj files.";
        questions.add(new AiQuestion(
            "Exclude dev dependencies from the scan? (recommended for a production BOM)",
            AiQuestion.Type.YES_NO,
            devHint
        ));

        // Q2 — text: exclude test projects from the solution?
        String testHint;
        if (!ctx.testProjectNames.isEmpty()) {
            testHint = "Test projects detected in solution: " + String.join(", ", ctx.testProjectNames)
                + "\nAll projects: " + String.join(", ", ctx.projectNames);
        } else if (!ctx.projectNames.isEmpty()) {
            testHint = "Projects detected in solution: " + String.join(", ", ctx.projectNames)
                + "\nNo test projects detected by naming convention.";
        } else {
            testHint = "No projects detected in solution file.";
        }
        questions.add(new AiQuestion(
            "Exclude any projects from the scan? Enter project name(s) or press Enter to skip:",
            AiQuestion.Type.TEXT,
            testHint
        ));

        return questions;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private File findSlnFile(File sourceDirectory) {
        File[] files = sourceDirectory.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().endsWith(SLN_EXTENSION)) {
                    return f;
                }
            }
        }
        return null;
    }

    /**
     * Scans .csproj files referenced by the solution for dev-only PackageReferences.
     * A dev-only package is identified by {@code PrivateAssets="all"} attribute or child element.
     */
    private boolean detectDevDependencies(File sourceDirectory, List<ParsedProject> projects) {
        for (ParsedProject project : projects) {
            String path = project.getPath();
            if (path == null || !path.endsWith(".csproj")) {
                continue;
            }
            File csprojFile = new File(sourceDirectory, path.replace('\\', File.separatorChar));
            if (!csprojFile.exists()) {
                continue;
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(csprojFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (PRIVATE_ASSETS_ALL_PATTERN.matcher(line).find()) {
                        return true;
                    }
                }
            } catch (IOException e) {
                logger.debug("Could not read .csproj file {}: {}", csprojFile.getPath(), e.getMessage());
            }
        }
        return false;
    }
}

