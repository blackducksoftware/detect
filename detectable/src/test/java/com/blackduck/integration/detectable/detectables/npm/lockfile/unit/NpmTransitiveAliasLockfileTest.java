package com.blackduck.integration.detectable.detectables.npm.lockfile.unit;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.npm.NpmDependencyType;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockFileProjectIdTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfileGraphTransformer;
import com.blackduck.integration.detectable.detectables.npm.lockfile.parse.NpmLockfilePackager;
import com.blackduck.integration.detectable.detectables.npm.lockfile.result.NpmPackagerResult;
import com.blackduck.integration.detectable.util.graph.GraphAssert;

class NpmTransitiveAliasLockfileTest {

    private Gson gson;
    private ExternalIdFactory externalIdFactory;
    private NpmLockfilePackager packager;

    @BeforeEach
    void setup() {
        gson = new Gson();
        externalIdFactory = new ExternalIdFactory();
        NpmLockFileProjectIdTransformer projectIdTransformer = new NpmLockFileProjectIdTransformer(gson, externalIdFactory);
        NpmLockfileGraphTransformer graphTransformer = new NpmLockfileGraphTransformer(EnumListFilter.excludeNone());
        packager = new NpmLockfilePackager(gson, externalIdFactory, projectIdTransformer, graphTransformer);
    }

    @Test
    void transitiveAliasPackagesAppearInBOMWithActualPackageName() throws IOException {
        // pretty-format declares aliases for two versions of react-is; neither alias appears in the
        // root package.json, so they are purely transitive.
        String packageJsonText = "{\"name\":\"test-project\",\"version\":\"1.0.0\","
            + "\"dependencies\":{\"pretty-format\":\"^30.0.0\"}}";

        String lockFileText = "{"
            + "\"name\":\"test-project\",\"version\":\"1.0.0\",\"lockfileVersion\":3,"
            + "\"packages\":{"
            + "\"\":{\"dependencies\":{\"pretty-format\":\"^30.0.0\"}},"
            + "\"node_modules/pretty-format\":{\"version\":\"30.0.0\","
            +   "\"dependencies\":{"
            +     "\"react-is-18\":\"npm:react-is@^18.3.1\","
            +     "\"react-is-19\":\"npm:react-is@^19.2.5\"}},"
            + "\"node_modules/react-is-18\":{\"name\":\"react-is\",\"version\":\"18.3.1\"},"
            + "\"node_modules/react-is-19\":{\"name\":\"react-is\",\"version\":\"19.2.7\"}"
            + "}}";

        NpmPackagerResult result = packager.parseAndTransform(null, packageJsonText, lockFileText);

        DependencyGraph graph = result.getCodeLocation().getDependencyGraph();
        GraphAssert graphAssert = new GraphAssert(Forge.NPMJS, graph);

        ExternalId reactIs18 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "react-is", "18.3.1");
        ExternalId reactIs19 = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "react-is", "19.2.7");
        ExternalId prettyFormat = externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "pretty-format", "30.0.0");
        graphAssert.hasDependency(reactIs18);
        graphAssert.hasDependency(reactIs19);
        graphAssert.hasParentChildRelationship(prettyFormat, reactIs18);
        graphAssert.hasParentChildRelationship(prettyFormat, reactIs19);

        graphAssert.hasNoDependency(externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "react-is-18", "18.3.1"));
        graphAssert.hasNoDependency(externalIdFactory.createNameVersionExternalId(Forge.NPMJS, "react-is-19", "19.2.7"));
    }
}
