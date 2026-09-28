package com.blackduck.integration.detectable.detectables.bazel.functional.bazel.pipeline.step;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectable.exception.DetectableException;
import com.blackduck.integration.detectable.detectables.bazel.pipeline.step.IntermediateStepParseShowRepoToUrlCandidates;

/**
 * Tests for {@link IntermediateStepParseShowRepoToUrlCandidates}.
 *
 * <p>Covers: explicit {@code url=}, {@code urls=[...]}, {@code remote=} (git_repository),
 * and {@code go_repository} importpath synthesis — including the per-block guard fix that
 * ensures go_repository synthesis is attempted even when a preceding block contributed URLs.
 */
public class IntermediateStepParseShowRepoToUrlCandidatesTest {

    private IntermediateStepParseShowRepoToUrlCandidates step;

    @BeforeEach
    public void setUp() {
        step = new IntermediateStepParseShowRepoToUrlCandidates();
    }

    // -------------------------------------------------------------------------
    // Basic attribute extraction
    // -------------------------------------------------------------------------

    @Test
    public void process_singleUrlAttribute_extractsUrl() throws DetectableException {
        String block = "http_archive(\n"
            + "    url = \"https://github.com/madler/zlib/archive/v1.3.tar.gz\",\n"
            + ")";
        List<String> result = step.process(Collections.singletonList(block));
        assertEquals(1, result.size());
        assertEquals("https://github.com/madler/zlib/archive/v1.3.tar.gz", result.get(0));
    }

    @Test
    public void process_urlsListAttribute_extractsAllUrls() throws DetectableException {
        String block = "http_archive(\n"
            + "    urls = [\n"
            + "        \"https://github.com/protocolbuffers/protobuf/archive/v31.0.tar.gz\",\n"
            + "        \"https://mirror.example.com/protobuf-31.0.tar.gz\",\n"
            + "    ],\n"
            + ")";
        List<String> result = step.process(Collections.singletonList(block));
        assertEquals(2, result.size());
        assertTrue(result.contains("https://github.com/protocolbuffers/protobuf/archive/v31.0.tar.gz"));
        assertTrue(result.contains("https://mirror.example.com/protobuf-31.0.tar.gz"));
    }

    @Test
    public void process_remoteAttribute_extractsGitRepositoryUrl() throws DetectableException {
        String block = "git_repository(\n"
            + "    remote = \"https://github.com/bazelbuild/rules_go.git\",\n"
            + "    tag = \"v0.39.1\",\n"
            + ")";
        List<String> result = step.process(Collections.singletonList(block));
        assertEquals(1, result.size());
        assertEquals("https://github.com/bazelbuild/rules_go.git", result.get(0));
    }

    // -------------------------------------------------------------------------
    // go_repository synthesis
    // -------------------------------------------------------------------------

    @Test
    public void process_goRepositoryWithGithubImportpath_synthesizesHttpsUrl() throws DetectableException {
        String block = "# Rule class: go_repository\n"
            + "go_repository(\n"
            + "    importpath = \"github.com/google/uuid\",\n"
            + ")";
        List<String> result = step.process(Collections.singletonList(block));
        assertEquals(1, result.size());
        assertEquals("https://github.com/google/uuid", result.get(0));
    }

    @Test
    public void process_goRepositoryWithNonGithubImportpath_producesNoUrl() throws DetectableException {
        String block = "# Rule class: go_repository\n"
            + "go_repository(\n"
            + "    importpath = \"golang.org/x/net\",\n"
            + ")";
        List<String> result = step.process(Collections.singletonList(block));
        assertTrue(result.isEmpty(), "Non-GitHub importpath must not produce a synthesized URL");
    }

    // -------------------------------------------------------------------------
    // Bug fix: per-block synthesis guard
    //
    // Before the fix, addSynthesizedGoUrlIfNoExplicit checked results.isEmpty() against
    // the accumulated list — so if block 1 contributed a URL, go_repository synthesis for
    // block 2 was silently skipped.  After the fix, the guard is per-block.
    // -------------------------------------------------------------------------

    @Test
    public void process_goRepositoryBlockAfterHttpArchiveBlock_synthesisNotSkipped() throws DetectableException {
        // Block 1: a normal http_archive with an explicit URL
        String block1 = "## @@somelib~:\n"
            + "http_archive(\n"
            + "    urls = [\"https://github.com/example/somelib/archive/v1.0.tar.gz\"],\n"
            + ")";

        // Block 2: a go_repository with a GitHub importpath — synthesis must still be attempted
        String block2 = "## @@golib~:\n"
            + "# Rule class: go_repository\n"
            + "go_repository(\n"
            + "    importpath = \"github.com/example/golib\",\n"
            + ")";

        List<String> result = step.process(Arrays.asList(block1, block2));

        assertEquals(2, result.size(),
            "Block 1 must contribute its explicit URL and block 2 must contribute the synthesized URL; "
                + "the per-block guard must not suppress block 2 because block 1 contributed a URL");
        assertEquals("https://github.com/example/somelib/archive/v1.0.tar.gz", result.get(0));
        assertEquals("https://github.com/example/golib", result.get(1));
    }

    @Test
    public void process_goRepositoryBlockWithExplicitUrl_synthesisSkippedForThatBlock() throws DetectableException {
        // go_repository that already has an explicit url= (unusual but valid) — synthesis must be suppressed
        String block = "# Rule class: go_repository\n"
            + "go_repository(\n"
            + "    importpath = \"github.com/example/golib\",\n"
            + "    url = \"https://github.com/example/golib/archive/v2.0.tar.gz\",\n"
            + ")";
        List<String> result = step.process(Collections.singletonList(block));
        // Only the explicit URL should appear; no synthesized URL on top
        assertEquals(1, result.size());
        assertEquals("https://github.com/example/golib/archive/v2.0.tar.gz", result.get(0));
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    public void process_nullInput_returnsEmpty() throws DetectableException {
        assertTrue(step.process(null).isEmpty());
    }

    @Test
    public void process_emptyInput_returnsEmpty() throws DetectableException {
        assertTrue(step.process(Collections.emptyList()).isEmpty());
    }

    @Test
    public void process_blockWithNoUrlAttributes_returnsEmpty() throws DetectableException {
        String block = "local_path_repository(\n"
            + "    name = \"local-dep\",\n"
            + "    path = \"/workspace/local-dep\",\n"
            + ")";
        assertTrue(step.process(Collections.singletonList(block)).isEmpty());
    }
}

