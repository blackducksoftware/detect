package com.blackduck.integration.detectable.detectables.bitbake.unit;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.Forge;
import com.blackduck.integration.bdio.model.externalid.ExternalId;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.annotations.UnitTest;
import com.blackduck.integration.detectable.detectable.util.EnumListFilter;
import com.blackduck.integration.detectable.detectables.bitbake.model.BitbakeGraph;
import com.blackduck.integration.detectable.detectables.bitbake.transform.BitbakeDependencyGraphTransformer;
import com.blackduck.integration.detectable.util.graph.GraphAssert;
import com.blackduck.integration.detectable.util.graph.NameVersionGraphAssert;

@UnitTest
public class BitbakeDependencyGraphTransformerTest {
    @Test
    public void parentHasChild() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BitbakeGraph bitbakeGraph = new BitbakeGraph();
        bitbakeGraph.addNode("example", "1:75-r50", "meta");
        bitbakeGraph.addNode("foobar", "12", "meta");
        bitbakeGraph.addChild("example", "foobar");

        Map<String, List<String>> recipeToLayerMap = new HashMap<>();
        recipeToLayerMap.put("example", Collections.singletonList("meta"));
        recipeToLayerMap.put("foobar", Collections.singletonList("meta"));

        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer = new BitbakeDependencyGraphTransformer(EnumListFilter.excludeNone());

        DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, recipeToLayerMap, null);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.YOCTO, dependencyGraph);

        ExternalId example = graphAssert.hasDependency(externalIdFactory.createYoctoExternalId("meta", "example", "1:75-r50"));
        ExternalId foobar = graphAssert.hasDependency(externalIdFactory.createYoctoExternalId("meta", "foobar", "12"));
        graphAssert.hasParentChildRelationship(example, foobar);
    }

    @Test
    public void ignoredNoVersionRelationship() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BitbakeGraph bitbakeGraph = new BitbakeGraph();
        bitbakeGraph.addNode("example", "75", "meta");
        bitbakeGraph.addNode("foobar", null, "meta");
        bitbakeGraph.addChild("example", "foobar");

        Map<String, List<String>> recipeToLayerMap = new HashMap<>();
        recipeToLayerMap.put("example", Collections.singletonList("meta"));
        recipeToLayerMap.put("foobar", Collections.singletonList("meta"));

        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer = new BitbakeDependencyGraphTransformer(EnumListFilter.excludeNone());
        DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, recipeToLayerMap, null);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.YOCTO, dependencyGraph);
        graphAssert.hasRootSize(1);
        ExternalId externalId = graphAssert.hasDependency(externalIdFactory.createYoctoExternalId("meta", "example", "75"));
        graphAssert.hasRelationshipCount(externalId, 0);
    }

    @Test
    public void ignoredNoVersion() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BitbakeGraph bitbakeGraph = new BitbakeGraph();
        bitbakeGraph.addNode("example", null, "meta");

        Map<String, List<String>> recipeToLayerMap = new HashMap<>();
        recipeToLayerMap.put("example", Collections.singletonList("meta"));

        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer = new BitbakeDependencyGraphTransformer(EnumListFilter.excludeNone());
        DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, recipeToLayerMap, null);

        GraphAssert graphAssert = new GraphAssert(Forge.YOCTO, dependencyGraph);
        graphAssert.hasNoDependency(externalIdFactory.createYoctoExternalId("meta", "example", null));
        graphAssert.hasRootSize(0);
    }

    @Test
    public void fallsBackToDependencyLayerWhenAuthoritativeLayerListIsEmpty() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BitbakeGraph bitbakeGraph = new BitbakeGraph();
        bitbakeGraph.addNode("example", "75", "meta-from-graph");

        Map<String, List<String>> recipeToLayerMap = new HashMap<>();
        recipeToLayerMap.put("example", Collections.emptyList());

        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer = new BitbakeDependencyGraphTransformer(EnumListFilter.excludeNone());
        DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, recipeToLayerMap, null);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.YOCTO, dependencyGraph);
        graphAssert.hasRootSize(1);
        graphAssert.hasDependency(externalIdFactory.createYoctoExternalId("meta-from-graph", "example", "75"));
    }

    @Test
    public void skipsDependencyWhenNoLayerIsAvailable() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BitbakeGraph bitbakeGraph = new BitbakeGraph();
        bitbakeGraph.addNode("example", "75", null);

        Map<String, List<String>> recipeToLayerMap = new HashMap<>();
        recipeToLayerMap.put("example", Collections.emptyList());

        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer = new BitbakeDependencyGraphTransformer(EnumListFilter.excludeNone());
        DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, recipeToLayerMap, null);

        GraphAssert graphAssert = new GraphAssert(Forge.YOCTO, dependencyGraph);
        graphAssert.hasNoDependency(externalIdFactory.createYoctoExternalId("meta", "example", "75"));
        graphAssert.hasRootSize(0);
    }

    /**
     * Graph layer parsed from task-depends.dot does not appear in the authoritative show-recipes list.
     * Expected: transformer ignores the invalid graph layer and uses the first authoritative layer instead.
     */
    @Test
    public void usesAuthoritativeLayerWhenGraphLayerIsInvalid() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BitbakeGraph bitbakeGraph = new BitbakeGraph();
        // node carries "wrong-layer" which is NOT in the authoritative map
        bitbakeGraph.addNode("example", "75", "wrong-layer");

        Map<String, List<String>> recipeToLayerMap = new HashMap<>();
        recipeToLayerMap.put("example", Collections.singletonList("meta-authoritative"));

        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer = new BitbakeDependencyGraphTransformer(EnumListFilter.excludeNone());
        DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, recipeToLayerMap, null);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.YOCTO, dependencyGraph);
        graphAssert.hasRootSize(1);
        // must use the authoritative layer, not the invalid graph layer
        graphAssert.hasDependency(externalIdFactory.createYoctoExternalId("meta-authoritative", "example", "75"));
        graphAssert.hasNoDependency(externalIdFactory.createYoctoExternalId("wrong-layer", "example", "75"));
    }

    /**
     * No layer is parsed from task-depends.dot for the node (null).
     * Expected: transformer falls back to the first authoritative layer from show-recipes.
     */
    @Test
    public void usesAuthoritativeLayerWhenGraphLayerIsNull() {
        ExternalIdFactory externalIdFactory = new ExternalIdFactory();
        BitbakeGraph bitbakeGraph = new BitbakeGraph();
        // node has no layer from task-depends.dot
        bitbakeGraph.addNode("example", "75", null);

        Map<String, List<String>> recipeToLayerMap = new HashMap<>();
        recipeToLayerMap.put("example", Collections.singletonList("meta-authoritative"));

        BitbakeDependencyGraphTransformer bitbakeDependencyGraphTransformer = new BitbakeDependencyGraphTransformer(EnumListFilter.excludeNone());
        DependencyGraph dependencyGraph = bitbakeDependencyGraphTransformer.transform(bitbakeGraph, recipeToLayerMap, null);

        NameVersionGraphAssert graphAssert = new NameVersionGraphAssert(Forge.YOCTO, dependencyGraph);
        graphAssert.hasRootSize(1);
        graphAssert.hasDependency(externalIdFactory.createYoctoExternalId("meta-authoritative", "example", "75"));
    }
}
