package com.blackduck.integration.detectable.detectables.bazel.functional.bazel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bazel.BazelWorkspaceFileParser;
import com.blackduck.integration.detectable.detectables.bazel.DependencySource;

class BazelWorkspaceFileParserTest {

    @Test
    void testSingleRule() throws IOException {
        File workspaceFile = new File("src/test/resources/detectables/functional/bazel/WORKSPACE");
        List<String> workspaceFileLines = FileUtils.readLines(workspaceFile, StandardCharsets.UTF_8);
        BazelWorkspaceFileParser bazelWorkspaceFileParser = new BazelWorkspaceFileParser();
        assertEquals(Sets.newHashSet(DependencySource.HTTP_ARCHIVE, DependencySource.MAVEN_INSTALL), bazelWorkspaceFileParser.parseDependencySources(workspaceFileLines));
    }

    @Test
    void testMultipleRules() throws IOException {
        File workspaceFile = new File("src/test/resources/detectables/functional/bazel/WORKSPACE_multipleRules");
        List<String> workspaceFileLines = FileUtils.readLines(workspaceFile, StandardCharsets.UTF_8);
        BazelWorkspaceFileParser bazelWorkspaceFileParser = new BazelWorkspaceFileParser();

        Set<DependencySource> rulesFound = bazelWorkspaceFileParser.parseDependencySources(workspaceFileLines);
        assertEquals(Sets.newHashSet(DependencySource.HTTP_ARCHIVE, DependencySource.MAVEN_INSTALL, DependencySource.HASKELL_CABAL_LIBRARY), rulesFound);
    }
}
