package com.blackduck.integration.detectable.detectables.npm.cli.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.dependency.Dependency;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.detectables.npm.packagejson.CombinedPackageJson;

class NpmCliParserTest {

    private final ExternalIdFactory externalIdFactory = new ExternalIdFactory();

    @Test
    void testParseNpmLsWithNonScopedAlias() {
        // Setup package.json with alias
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();
        combinedPackageJson.getDependencies().put("myalias", "npm:actualpackage@^1.0.0");

        // Setup npm ls output with the alias
        String npmLsOutput = "{\n" +
            "  \"name\": \"test-project\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"myalias\": {\n" +
            "      \"version\": \"1.0.0\",\n" +
            "      \"resolved\": \"https://registry.npmjs.org/actualpackage/-/actualpackage-1.0.0.tgz\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

        EnumListFilter<NpmDependencyType> filter = EnumListFilter.excludeNone();
        NpmCliParser parser = new NpmCliParser(externalIdFactory, filter);

        NpmPackagerResult result = parser.generateCodeLocation(npmLsOutput, combinedPackageJson);

        assertNotNull(result);
        assertNotNull(result.getCodeLocation());

        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        List<Dependency> rootDependencies = graph.getRootDependencies().stream().collect(Collectors.toList());

        assertEquals(1, rootDependencies.size());
        Dependency dependency = rootDependencies.get(0);

        // Verify the actual package name is used, not the alias
        assertEquals("actualpackage", dependency.getName());
        assertEquals("1.0.0", dependency.getVersion());
    }

    @Test
    void testParseNpmLsWithScopedAlias() {
        // Setup package.json with scoped alias
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();
        combinedPackageJson.getDependencies().put("scopedalias", "npm:@scope/package@^2.0.0");

        // Setup npm ls output with the scoped alias
        String npmLsOutput = "{\n" +
            "  \"name\": \"test-project\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"scopedalias\": {\n" +
            "      \"version\": \"2.1.0\",\n" +
            "      \"resolved\": \"https://registry.npmjs.org/@scope/package/-/package-2.1.0.tgz\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

        EnumListFilter<NpmDependencyType> filter = EnumListFilter.excludeNone();
        NpmCliParser parser = new NpmCliParser(externalIdFactory, filter);

        NpmPackagerResult result = parser.generateCodeLocation(npmLsOutput, combinedPackageJson);

        assertNotNull(result);
        assertNotNull(result.getCodeLocation());

        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        List<Dependency> rootDependencies = graph.getRootDependencies().stream().collect(Collectors.toList());

        assertEquals(1, rootDependencies.size());
        Dependency dependency = rootDependencies.get(0);

        // Verify the actual scoped package name is used, not the alias
        assertEquals("@scope/package", dependency.getName());
        assertEquals("2.1.0", dependency.getVersion());
    }

    @Test
    void testParseNpmLsWithMultipleAliases() {
        // Setup package.json with multiple aliases
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();
        combinedPackageJson.getDependencies().put("alias1", "npm:package1@^1.0.0");
        combinedPackageJson.getDependencies().put("alias2", "npm:package2@^2.0.0");
        combinedPackageJson.getDependencies().put("normalpackage", "^3.0.0");  // Non-alias dependency

        // Setup npm ls output with the aliases
        String npmLsOutput = "{\n" +
            "  \"name\": \"test-project\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"alias1\": {\n" +
            "      \"version\": \"1.0.0\"\n" +
            "    },\n" +
            "    \"alias2\": {\n" +
            "      \"version\": \"2.0.0\"\n" +
            "    },\n" +
            "    \"normalpackage\": {\n" +
            "      \"version\": \"3.0.0\"\n" +
            "    }\n" +
            "  }\n" +
            "}";

        EnumListFilter<NpmDependencyType> filter = EnumListFilter.excludeNone();
        NpmCliParser parser = new NpmCliParser(externalIdFactory, filter);

        NpmPackagerResult result = parser.generateCodeLocation(npmLsOutput, combinedPackageJson);

        assertNotNull(result);
        assertNotNull(result.getCodeLocation());

        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        List<Dependency> rootDependencies = graph.getRootDependencies().stream().collect(Collectors.toList());

        assertEquals(3, rootDependencies.size());

        // Verify all dependencies are present with correct names
        List<String> dependencyNames = rootDependencies.stream()
            .map(Dependency::getName)
            .collect(Collectors.toList());

        assertTrue(dependencyNames.contains("package1"));
        assertTrue(dependencyNames.contains("package2"));
        assertTrue(dependencyNames.contains("normalpackage"));
    }

    @Test
    void testParseNpmLsWithDevDependencyAlias() {
        // Setup package.json with dev dependency alias
        CombinedPackageJson combinedPackageJson = new CombinedPackageJson();
        combinedPackageJson.getDevDependencies().put("devalias", "npm:devpackage@^1.0.0");

        // Setup npm ls output
        String npmLsOutput = "{\n" +
            "  \"name\": \"test-project\",\n" +
            "  \"version\": \"1.0.0\",\n" +
            "  \"dependencies\": {\n" +
            "    \"devalias\": {\n" +
            "      \"version\": \"1.0.0\",\n" +
            "      \"dev\": true\n" +
            "    }\n" +
            "  }\n" +
            "}";

        // Include dev dependencies
        EnumListFilter<NpmDependencyType> filter = EnumListFilter.excludeNone();
        NpmCliParser parser = new NpmCliParser(externalIdFactory, filter);

        NpmPackagerResult result = parser.generateCodeLocation(npmLsOutput, combinedPackageJson);

        assertNotNull(result);
        assertNotNull(result.getCodeLocation());

        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        List<Dependency> rootDependencies = graph.getRootDependencies().stream().collect(Collectors.toList());

        assertEquals(1, rootDependencies.size());
        Dependency dependency = rootDependencies.get(0);

        // Verify the actual package name is used for dev dependency alias
        assertEquals("devpackage", dependency.getName());
        assertEquals("1.0.0", dependency.getVersion());
    }
}
