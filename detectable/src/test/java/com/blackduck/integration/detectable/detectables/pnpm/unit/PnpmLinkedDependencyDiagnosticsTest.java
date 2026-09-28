package com.blackduck.integration.detectable.detectables.pnpm.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyInfo;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmDependencyType;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmPackageInfo;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmPackageInfov5;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmProjectPackage;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.model.PnpmProjectPackagev5;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmLinkedPackageResolver;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmYamlTransformer;
import com.blackduck.integration.detectable.detectables.pnpm.lockfile.process.PnpmYamlTransformerv5;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

class PnpmLinkedDependencyDiagnosticsTest {
    private static final String INFO_SINGULAR = "local linked dependency to resolved lockfile package entries. "
        + "The corresponding direct dependency relationships could not be added to this module's BOM";
    private static final String INFO_PLURAL = "local linked dependencies to resolved lockfile package entries. "
        + "The corresponding direct dependency relationships could not be added to this module's BOM";
    private static final String DEBUG_MESSAGE = "local linked dependencies that could not be added as direct dependencies in the BOM";
    private static final String REASON = ". Detect could not match these links to resolved lockfile package entries.";

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void reportsMissingLinkWithUnresolvedEmptyOrResolvedVersion(String version) throws Exception {
        for (String resolvedVersion : Arrays.asList(null, "", "1.0.0")) {
            Fixture fixture = new Fixture(version);
            fixture.devDependencies.put("@acme/api", "link:../../packages/api");
            when(fixture.resolver.resolveVersionOfLinkedPackage("apps/expo", "../../packages/api")).thenReturn(resolvedVersion);
            fixture.addExternalDependencies();

            DependencyGraph graph = fixture.generate("apps/expo");

            fixture.assertExternalGraph(graph);
            assertEquals(Collections.singletonList(
                "Workspace module 'apps/expo': Detect could not match 1 " + INFO_SINGULAR
                    + ". Names and link targets are available at DEBUG."
            ), fixture.infoMessages());
            assertEquals(Collections.singletonList(
                "Workspace module 'apps/expo': " + DEBUG_MESSAGE + ": [@acme/api (DEV, link:../../packages/api)]" + REASON
            ), fixture.debugMessages());
            verify(fixture.resolver, times(1)).resolveVersionOfLinkedPackage("apps/expo", "../../packages/api");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void doesNotReportALinkThatActuallyBecomesADirectDependency(String version) throws Exception {
        Fixture fixture = new Fixture(version);
        fixture.devDependencies.put("@acme/api", "link:../../packages/api");
        when(fixture.resolver.resolveVersionOfLinkedPackage("apps/expo", "../../packages/api")).thenReturn("1.0.0");
        fixture.packages.put(fixture.packageId("@acme/api", "1.0.0"), new PnpmPackageInfo());

        DependencyGraph graph = fixture.generate("apps/expo");

        new NameVersionGraphAssert(Forge.NPMJS, graph).hasRootDependency("@acme/api", "1.0.0");
        assertTrue(fixture.infoMessages().isEmpty());
        assertTrue(fixture.debugMessages().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void ignoresFilteredLinksAndOrdinaryUnresolvedDependencies(String version) throws Exception {
        Fixture fixture = new Fixture(version, PnpmDependencyType.DEV, PnpmDependencyType.OPTIONAL);
        fixture.dependencies.put("missing-external", "1.0.0");
        fixture.devDependencies.put("@acme/api", "link:../../packages/api");
        fixture.optionalDependencies.put("@acme/optional", "link:../../packages/optional");

        DependencyGraph graph = fixture.generate("apps/expo");

        assertTrue(graph.getRootDependencies().isEmpty());
        assertTrue(fixture.infoMessages().isEmpty());
        assertTrue(fixture.debugMessages().isEmpty());
        verify(fixture.resolver, never()).resolveVersionOfLinkedPackage("apps/expo", "../../packages/api");
        verify(fixture.resolver, never()).resolveVersionOfLinkedPackage("apps/expo", "../../packages/optional");
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void aggregatesAllSelectedLinkTypesInStableOrder(String version) throws Exception {
        Fixture fixture = new Fixture(version);
        fixture.dependencies.put("@acme/z", "link:packages/z");
        fixture.devDependencies.put("@acme/a", "link:packages/a");
        fixture.optionalDependencies.put("@acme/m", "link:packages/m");

        fixture.generate(null);

        assertEquals(Collections.singletonList(
            "Workspace module '(root)': Detect could not match 3 " + INFO_PLURAL
                + ". Names and link targets are available at DEBUG."
        ), fixture.infoMessages());
        assertEquals(Collections.singletonList(
            "Workspace module '(root)': " + DEBUG_MESSAGE + ": "
                + "[@acme/a (DEV, link:packages/a), @acme/m (OPTIONAL, link:packages/m), @acme/z (DEPENDENCY, link:packages/z)]" + REASON
        ), fixture.debugMessages());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void respectsSectionPrecedenceForRepeatedNames(String version) throws Exception {
        Fixture fixture = new Fixture(version);
        fixture.dependencies.put("shared", "link:packages/shared");
        fixture.devDependencies.put("shared", "1.0.0");
        fixture.packages.put(fixture.packageId("shared", "1.0.0"), new PnpmPackageInfo());

        DependencyGraph graph = fixture.generate(".");

        new NameVersionGraphAssert(Forge.NPMJS, graph).hasRootDependency("shared", "1.0.0");
        assertTrue(fixture.infoMessages().isEmpty());
        assertTrue(fixture.debugMessages().isEmpty());

        fixture.optionalDependencies.put("shared", "link:packages/optional");
        graph = fixture.generate(".");

        assertTrue(graph.getRootDependencies().isEmpty());
        assertEquals(Collections.singletonList(
            "Workspace module '(root)': Detect could not match 1 " + INFO_SINGULAR
                + ". Names and link targets are available at DEBUG."
        ), fixture.infoMessages());
        assertEquals(Collections.singletonList(
            "Workspace module '(root)': " + DEBUG_MESSAGE + ": [shared (OPTIONAL, link:packages/optional)]" + REASON
        ), fixture.debugMessages());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void retainsAProductionLinkWhenOtherSectionsAreFiltered(String version) throws Exception {
        Fixture fixture = new Fixture(version, PnpmDependencyType.DEV, PnpmDependencyType.OPTIONAL);
        fixture.dependencies.put("shared", "link:packages/shared");
        fixture.devDependencies.put("shared", "link:packages/dev");
        fixture.optionalDependencies.put("shared", "link:packages/optional");

        fixture.generate(null);

        assertEquals(Collections.singletonList(
            "Workspace module '(root)': Detect could not match 1 " + INFO_SINGULAR
                + ". Names and link targets are available at DEBUG."
        ), fixture.infoMessages());
        assertEquals(Collections.singletonList(
            "Workspace module '(root)': " + DEBUG_MESSAGE + ": [shared (DEPENDENCY, link:packages/shared)]" + REASON
        ), fixture.debugMessages());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void reportsMissingPackagesSectionWithoutLosingTheLink(String version) throws Exception {
        Fixture fixture = new Fixture(version);
        fixture.devDependencies.put("@acme/api", "link:packages/api");
        fixture.packages = null;

        DependencyGraph graph = fixture.generate(null);

        assertTrue(graph.getRootDependencies().isEmpty());
        assertEquals(Collections.singletonList(
            "Workspace module '(root)': Detect could not match 1 " + INFO_SINGULAR
                + ". Names and link targets are available at DEBUG."
        ), fixture.infoMessages());
        assertEquals(Collections.singletonList(
            "Workspace module '(root)': " + DEBUG_MESSAGE + ": [@acme/api (DEV, link:packages/api)]" + REASON
        ), fixture.debugMessages());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void doesNotLeakSuccessfulMatchesBetweenModules(String version) throws Exception {
        Fixture fixture = new Fixture(version);
        fixture.dependencies.put("shared", "link:../shared");
        when(fixture.resolver.resolveVersionOfLinkedPackage("first", "../shared")).thenReturn("1.0.0");
        when(fixture.resolver.resolveVersionOfLinkedPackage("second", "../shared")).thenReturn("1.0.0");
        fixture.packages.put(fixture.packageId("shared", "1.0.0"), new PnpmPackageInfo());
        fixture.generate("first");
        fixture.packages.clear();
        fixture.generate("second");

        assertEquals(Collections.singletonList(
            "Workspace module 'second': Detect could not match 1 " + INFO_SINGULAR
                + ". Names and link targets are available at DEBUG."
        ), fixture.infoMessages());
        assertEquals(Collections.singletonList(
            "Workspace module 'second': " + DEBUG_MESSAGE + ": [shared (DEPENDENCY, link:../shared)]" + REASON
        ), fixture.debugMessages());
    }

    @ParameterizedTest
    @ValueSource(strings = {"5.4", "6.0", "9.0"})
    void logsCountWithoutDetailsAndDoesNotChangeGraphsWhenOnlyInfoIsEnabled(String version) throws Exception {
        Fixture fixture = new Fixture(version);
        when(fixture.logger.isDebugEnabled()).thenReturn(false);
        fixture.devDependencies.put("@acme/api", "link:../../packages/api");
        fixture.addExternalDependencies();

        DependencyGraph graph = fixture.generate("apps/expo");

        fixture.assertExternalGraph(graph);
        assertEquals(Collections.singletonList(
            "Workspace module 'apps/expo': Detect could not match 1 " + INFO_SINGULAR
                + ". Names and link targets are available at DEBUG."
        ), fixture.infoMessages());
        assertTrue(fixture.debugMessages().isEmpty());
    }

    private static class Fixture {
        private final String version;
        private final Logger logger = mock(Logger.class);
        private final PnpmLinkedPackageResolver resolver = mock(PnpmLinkedPackageResolver.class);
        private final Map<String, String> dependencies = new HashMap<>();
        private final Map<String, String> devDependencies = new HashMap<>();
        private final Map<String, String> optionalDependencies = new HashMap<>();
        private Map<String, PnpmPackageInfo> packages = new HashMap<>();
        private final Object transformer;

        private Fixture(String version, PnpmDependencyType... excluded) throws Exception {
            this.version = version;
            EnumListFilter<PnpmDependencyType> filter = EnumListFilter.fromExcluded(excluded);
            transformer = version.startsWith("5") ? new PnpmYamlTransformerv5(filter) : new PnpmYamlTransformer(filter, version);
            Field loggerField = transformer.getClass().getDeclaredField("logger");
            loggerField.setAccessible(true);
            loggerField.set(transformer, logger);
            when(logger.isInfoEnabled()).thenReturn(true);
            when(logger.isDebugEnabled()).thenReturn(true);
        }

        private String packageId(String name, String resolvedVersion) {
            if (version.startsWith("5")) {
                return "/" + name + "/" + resolvedVersion;
            }
            return (version.startsWith("6") ? "/" : "") + name + "@" + resolvedVersion;
        }

        private void addExternalDependencies() {
            dependencies.put("parent", "1.0.0");
            PnpmPackageInfo parent = new PnpmPackageInfo();
            parent.dependencies = Collections.singletonMap("child", "2.0.0");
            packages.put(packageId("parent", "1.0.0"), parent);
            packages.put(packageId("child", "2.0.0"), new PnpmPackageInfo());
        }

        private void assertExternalGraph(DependencyGraph graph) {
            NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.NPMJS, graph);
            graphAssert.hasRootSize(1);
            graphAssert.hasRootDependency("parent", "1.0.0");
            graphAssert.hasParentChildRelationship("parent", "1.0.0", "child", "2.0.0");
        }

        private DependencyGraph generate(String projectPath) throws Exception {
            if (transformer instanceof PnpmYamlTransformerv5) {
                PnpmProjectPackagev5 project = new PnpmProjectPackagev5();
                project.dependencies = dependencies;
                project.devDependencies = devDependencies;
                project.optionalDependencies = optionalDependencies;
                Map<String, PnpmPackageInfov5> v5Packages = null;
                if (packages != null) {
                    v5Packages = new HashMap<>();
                    for (Map.Entry<String, PnpmPackageInfo> entry : packages.entrySet()) {
                        PnpmPackageInfov5 info = new PnpmPackageInfov5();
                        info.name = entry.getValue().name;
                        info.version = entry.getValue().version;
                        info.dependencies = entry.getValue().dependencies;
                        v5Packages.put(entry.getKey(), info);
                    }
                }
                return ((PnpmYamlTransformerv5) transformer).generateCodeLocation(
                    new File("."), project, projectPath, null, v5Packages, resolver).getDependencyGraph();
            }
            PnpmProjectPackage project = new PnpmProjectPackage();
            project.dependencies = dependencyInfo(dependencies);
            project.devDependencies = dependencyInfo(devDependencies);
            project.optionalDependencies = dependencyInfo(optionalDependencies);
            return ((PnpmYamlTransformer) transformer).generateCodeLocation(
                new File("."), project, projectPath, null, packages, resolver, null).getDependencyGraph();
        }

        private Map<String, PnpmDependencyInfo> dependencyInfo(Map<String, String> declarations) {
            Map<String, PnpmDependencyInfo> result = new HashMap<>();
            declarations.forEach((name, value) -> {
                PnpmDependencyInfo info = new PnpmDependencyInfo();
                info.version = value;
                result.put(name, info);
            });
            return result;
        }

        private List<String> infoMessages() {
            return logMessages("info", "Detect could not match");
        }

        private List<String> debugMessages() {
            return logMessages("debug", DEBUG_MESSAGE);
        }

        private List<String> logMessages(String level, String marker) {
            return mockingDetails(logger).getInvocations().stream()
                .filter(invocation -> level.equals(invocation.getMethod().getName()))
                .map(invocation -> {
                    Object[] arguments = invocation.getArguments();
                    return MessageFormatter.arrayFormat((String) arguments[0], Arrays.copyOfRange(arguments, 1, arguments.length)).getMessage();
                })
                .filter(message -> message.contains(marker))
                .collect(Collectors.toList());
        }
    }
}
