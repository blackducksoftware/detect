package com.blackduck.integration.detectable.detectables.cargo.parse;

import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.util.NameVersion;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests workspace inheritance syntax parsing in Cargo.toml files.
 * Verifies that dependencies with .workspace = true are correctly resolved
 * from the root workspace's [workspace.dependencies], [workspace.dev-dependencies],
 * and [workspace.build-dependencies] sections.
 */
class CargoTomlWorkspaceParsingTest {

    private final String rootWorkspaceToml = buildRootWorkspaceToml();
    private final String memberTomlWithInheritance = buildMemberTomlWithInheritance();
    private final String memberTomlWithMixedDependencies = buildMemberTomlWithMixedDependencies();

    private static String buildRootWorkspaceToml() {
        StringBuilder sb = new StringBuilder();
        sb.append("[workspace]\n");
        sb.append("resolver = \"2\"\n");
        sb.append("members = [\"crates/lib1\", \"crates/lib2\"]\n\n");

        sb.append("[workspace.package]\n");
        sb.append("version = \"1.0.0\"\n");
        sb.append("edition = \"2021\"\n\n");

        sb.append("[workspace.dependencies]\n");
        sb.append("serde = \"1.0.152\"\n");
        sb.append("log = \"0.4.17\"\n");
        sb.append("anyhow = \"1.0.68\"\n\n");

        sb.append("[workspace.dev-dependencies]\n");
        sb.append("tokio = \"1.35.0\"\n");
        sb.append("tempfile = \"3.3.0\"\n\n");

        sb.append("[workspace.build-dependencies]\n");
        sb.append("cc = \"1.0.79\"\n");

        return sb.toString();
    }

    private static String buildMemberTomlWithInheritance() {
        StringBuilder sb = new StringBuilder();
        sb.append("[package]\n");
        sb.append("name = \"lib1\"\n");
        sb.append("version.workspace = true\n");
        sb.append("edition.workspace = true\n\n");

        sb.append("[dependencies]\n");
        sb.append("serde.workspace = true\n");
        sb.append("log.workspace = true\n\n");

        sb.append("[dev-dependencies]\n");
        sb.append("tokio.workspace = true\n\n");

        sb.append("[build-dependencies]\n");
        sb.append("cc.workspace = true\n");

        return sb.toString();
    }

    private static String buildMemberTomlWithMixedDependencies() {
        StringBuilder sb = new StringBuilder();
        sb.append("[package]\n");
        sb.append("name = \"lib2\"\n");
        sb.append("version.workspace = true\n\n");

        sb.append("[dependencies]\n");
        sb.append("serde.workspace = true\n");
        sb.append("clap = \"4.1.4\"\n");
        sb.append("anyhow.workspace = true\n\n");

        sb.append("[dev-dependencies]\n");
        sb.append("tokio.workspace = true\n");
        sb.append("criterion = \"0.5.1\"\n");

        return sb.toString();
    }

    @Test
    void testParseWorkspaceDependencies() {
        CargoTomlParser parser = new CargoTomlParser();
        Map<String, String> workspaceDeps = parser.parseWorkspaceDependencies(rootWorkspaceToml);

        assertEquals(6, workspaceDeps.size());
        assertEquals("1.0.152", workspaceDeps.get("serde"));
        assertEquals("0.4.17", workspaceDeps.get("log"));
        assertEquals("1.0.68", workspaceDeps.get("anyhow"));
        assertEquals("1.35.0", workspaceDeps.get("tokio"));
        assertEquals("3.3.0", workspaceDeps.get("tempfile"));
        assertEquals("1.0.79", workspaceDeps.get("cc"));
    }

    @Test
    void testParseMemberWithWorkspaceInheritance() {
        CargoTomlParser parser = new CargoTomlParser();
        Map<String, String> workspaceDeps = parser.parseWorkspaceDependencies(rootWorkspaceToml);

        Set<NameVersion> result = parser.parseDependenciesToInclude(
            memberTomlWithInheritance,
            EnumListFilter.excludeNone(),
            workspaceDeps
        );

        assertEquals(4, result.size());
        assertTrue(result.contains(new NameVersion("serde", "1.0.152")));
        assertTrue(result.contains(new NameVersion("log", "0.4.17")));
        assertTrue(result.contains(new NameVersion("tokio", "1.35.0")));
        assertTrue(result.contains(new NameVersion("cc", "1.0.79")));
    }

    @Test
    void testParseMemberWithMixedDependencies() {
        CargoTomlParser parser = new CargoTomlParser();
        Map<String, String> workspaceDeps = parser.parseWorkspaceDependencies(rootWorkspaceToml);

        Set<NameVersion> result = parser.parseDependenciesToInclude(
            memberTomlWithMixedDependencies,
            EnumListFilter.excludeNone(),
            workspaceDeps
        );

        assertEquals(5, result.size());
        // Workspace inherited
        assertTrue(result.contains(new NameVersion("serde", "1.0.152")));
        assertTrue(result.contains(new NameVersion("anyhow", "1.0.68")));
        assertTrue(result.contains(new NameVersion("tokio", "1.35.0")));
        // Direct specification
        assertTrue(result.contains(new NameVersion("clap", "4.1.4")));
        assertTrue(result.contains(new NameVersion("criterion", "0.5.1")));
    }
}
