package com.blackduck.integration.detectable.detectables.bazel.aspect;

import com.blackduck.integration.detectable.ExecutableTarget;
import com.blackduck.integration.detectable.ExecutableUtils;
import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.blackduck.integration.executable.ExecutableOutput;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Runs the Bazel detect_deps_aspect against a target and returns the dependency
 * metadata it collected across the full transitive BUILD graph.
 *
 * <p>Lifecycle per scan:
 * <ol>
 *   <li>Extract {@code detect_deps_aspect.bzl} from classpath resources to the workspace root.</li>
 *   <li>Run {@code bazel build <target> --aspects=//:detect_deps_aspect.bzl%detect_deps_aspect --output_groups=dep_info}.</li>
 *   <li>Glob for {@code _detect_dep_meta_*.json} files under {@code bazel-bin/}.</li>
 *   <li>Parse each file into {@link AspectNodeMetadata}.</li>
 *   <li>Delete the generated JSON files and the .bzl file.</li>
 * </ol>
 *
 * <p>The caller's existing Bazel output base is reused (no {@code --output_base} override),
 * so Bazel can serve the analysis phase from its cached analysis graph — fast on large projects.
 */
public class BazelAspectExtractor {
    private static final Logger logger = LoggerFactory.getLogger(BazelAspectExtractor.class);

    private static final String ASPECT_RESOURCE_PATH = "/bazel/detect_deps_aspect.bzl";
    private static final String ASPECT_FILE_NAME = "detect_deps_aspect.bzl";
    private static final String ASPECT_FLAG = "--aspects=//:detect_deps_aspect.bzl%detect_deps_aspect";
    private static final String OUTPUT_GROUPS_FLAG = "--output_groups=dep_info";
    private static final String JSON_FILENAME_PREFIX = "_detect_dep_meta_";

    private final DetectableExecutableRunner runner;
    private final File workspaceDir;
    private final ExecutableTarget bazelExe;
    private final Gson gson;

    public BazelAspectExtractor(DetectableExecutableRunner runner, File workspaceDir, ExecutableTarget bazelExe) {
        this.runner = runner;
        this.workspaceDir = workspaceDir;
        this.bazelExe = bazelExe;
        this.gson = new Gson();
    }

    /**
     * Runs the aspect against {@code bazelTarget} and returns one {@link AspectNodeMetadata}
     * per visited BUILD target. Cleans up the aspect file and all generated JSON files before returning.
     */
    public List<AspectNodeMetadata> run(String bazelTarget) throws DetectableException {
        File aspectFile = writeAspectToWorkspace();
        List<File> jsonFiles = new ArrayList<>();
        try {
            runAspectBuild(bazelTarget);
            jsonFiles = findGeneratedJsonFiles();
            logger.info("Bazel aspect produced {} metadata file(s) for target '{}'", jsonFiles.size(), bazelTarget);
            return parseJsonFiles(jsonFiles);
        } finally {
            deleteQuietly(aspectFile);
            jsonFiles.forEach(this::deleteQuietly);
        }
    }

    private File writeAspectToWorkspace() throws DetectableException {
        File out = new File(workspaceDir, ASPECT_FILE_NAME);
        try (InputStream in = BazelAspectExtractor.class.getResourceAsStream(ASPECT_RESOURCE_PATH)) {
            if (in == null) {
                throw new DetectableException("Aspect resource not found on classpath: " + ASPECT_RESOURCE_PATH);
            }
            Files.copy(in, out.toPath(), StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Wrote aspect file to {}", out.getAbsolutePath());
        } catch (IOException e) {
            throw new DetectableException("Failed to write aspect file to workspace: " + e.getMessage(), e);
        }
        return out;
    }

    private void runAspectBuild(String bazelTarget) throws DetectableException {
        List<String> args = Arrays.asList("build", bazelTarget, ASPECT_FLAG, OUTPUT_GROUPS_FLAG);
        logger.debug("Running Bazel aspect: bazel {}", String.join(" ", args));
        try {
            ExecutableOutput output = runner.execute(ExecutableUtils.createFromTarget(workspaceDir, bazelExe, args));
            if (output.getReturnCode() != 0) {
                throw new DetectableException(
                    "Bazel aspect build failed with exit code " + output.getReturnCode()
                    + ". Check bazel stderr: " + output.getErrorOutput());
            }
        } catch (DetectableException e) {
            throw e;
        } catch (Exception e) {
            throw new DetectableException("Failed to invoke Bazel aspect build: " + e.getMessage(), e);
        }
    }

    private List<File> findGeneratedJsonFiles() {
        File bazelBin = new File(workspaceDir, "bazel-bin");
        if (!bazelBin.exists()) {
            logger.warn("bazel-bin not found at {}; aspect may not have produced output", bazelBin.getAbsolutePath());
            return new ArrayList<>();
        }
        Collection<File> found = FileUtils.listFiles(
            bazelBin,
            new WildcardFileFilter(JSON_FILENAME_PREFIX + "*.json"),
            TrueFileFilter.INSTANCE
        );
        return new ArrayList<>(found);
    }

    private List<AspectNodeMetadata> parseJsonFiles(List<File> files) {
        List<AspectNodeMetadata> nodes = new ArrayList<>();
        for (File file : files) {
            try {
                String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                AspectNodeMetadata node = gson.fromJson(json, AspectNodeMetadata.class);
                if (node != null) {
                    nodes.add(node);
                }
            } catch (Exception e) {
                logger.warn("Failed to parse aspect metadata file '{}': {}", file.getName(), e.getMessage());
            }
        }
        return nodes;
    }

    private void deleteQuietly(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                logger.debug("Could not delete aspect output file: {}", file.getAbsolutePath());
            }
        }
    }
}
