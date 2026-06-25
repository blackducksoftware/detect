package com.blackduck.integration.detectable.detectables.bitbake.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bitbake.parse.GraphNodeLabelParser;

public class GraphNodeLabelParserTest {

    @Test
    void testVersion() {
        String labelValue = "acl-native do_compile\\n:2.3.1-r0\\nvirtual:native:/workdir/poky/meta/recipes-support/attr/acl_2.3.1.bb";
        GraphNodeLabelParser parser = new GraphNodeLabelParser();

        Optional<String> version = parser.parseVersionFromLabel(labelValue);

        assertTrue(version.isPresent());
        assertEquals("2.3.1-r0", version.get());
    }

    @Test
    void testLayer() {
        String labelValue = "acl-native do_compile\\n:2.3.1-r0\\nvirtual:native:/workdir/poky/meta/recipes-support/attr/acl_2.3.1.bb";
        GraphNodeLabelParser parser = new GraphNodeLabelParser();
        Set<String> knownLayers = new HashSet<>();
        knownLayers.add("meta");

        Optional<String> layer = parser.parseLayerFromLabel(labelValue, knownLayers);

        assertTrue(layer.isPresent());
        assertEquals("meta", layer.get());
    }

    /**
     * Path: /home/user/wrlinux/project/poky/meta/recipes-support/openssl.bb
     *                  ^^^^^^^^              ^^^^
     *                  User folder           Actual layer (deepest)
     *
     * Verifies: Returns "meta" (deepest), not "wrlinux" (shallow parent folder).
     */
    @Test
    void testLayerWithParentFolderCollision() {
        String labelValue = "openssl do_compile\\n:3.0.8-r0\\n/home/user/wrlinux/project/poky/meta/recipes-support/openssl_3.0.8.bb";
        GraphNodeLabelParser parser = new GraphNodeLabelParser();

        Set<String> knownLayers = new LinkedHashSet<>();
        knownLayers.add("wrlinux");
        knownLayers.add("meta");
        knownLayers.add("meta-poky");

        Optional<String> layer = parser.parseLayerFromLabel(labelValue, knownLayers);

        assertTrue(layer.isPresent());
        assertEquals("meta", layer.get());
    }

    /**
     * Same path as above, but layers in different order.
     *
     * Verifies: Result is deterministic regardless of iteration order.
     */
    @Test
    void testLayerParsingDeterministic() {
        String labelValue = "openssl do_compile\\n:3.0.8-r0\\n/home/user/wrlinux/project/poky/meta/recipes-support/openssl_3.0.8.bb";
        GraphNodeLabelParser parser = new GraphNodeLabelParser();

        Set<String> knownLayers = new LinkedHashSet<>();
        knownLayers.add("meta");
        knownLayers.add("wrlinux");
        knownLayers.add("meta-poky");

        Optional<String> layer = parser.parseLayerFromLabel(labelValue, knownLayers);

        assertTrue(layer.isPresent());
        assertEquals("meta", layer.get());
    }

    /**
     * Path: /builds/core/yocto/meta-custom/recipes-support/curl.bb
     *               ^^^^       ^^^^^^^^^^^
     *               Parent     Actual layer (deepest)
     *
     * Verifies: Any parent folder collision is handled, not just specific names.
     */
    @Test
    void testLayerIgnoresArbitraryParentFolder() {
        String labelValue = "curl do_compile\\n:7.88.0-r0\\n/builds/core/yocto/meta-custom/recipes-support/curl_7.88.0.bb";
        GraphNodeLabelParser parser = new GraphNodeLabelParser();

        Set<String> knownLayers = new LinkedHashSet<>();
        knownLayers.add("core");
        knownLayers.add("meta-custom");
        knownLayers.add("meta");

        Optional<String> layer = parser.parseLayerFromLabel(labelValue, knownLayers);

        assertTrue(layer.isPresent());
        assertEquals("meta-custom", layer.get());
    }

    /**
     * Path: /home/user/wrlinux/project/poky/meta/recipes-support/openssl.bb
     *                  ^^^^^^^^        ^^^^  ^^^^
     *                  Pos 10          23    31 (deepest wins)
     *
     * Verifies: With multiple matches, deepest is selected.
     */
    @Test
    void testLayerSelectsDeepestMatch() {
        String labelValue = "openssl do_compile\\n:3.0.8-r0\\n/home/user/wrlinux/project/poky/meta/recipes-support/openssl_3.0.8.bb";
        GraphNodeLabelParser parser = new GraphNodeLabelParser();

        Set<String> knownLayers = new LinkedHashSet<>();
        knownLayers.add("wrlinux");
        knownLayers.add("poky");
        knownLayers.add("meta");

        Optional<String> layer = parser.parseLayerFromLabel(labelValue, knownLayers);

        assertTrue(layer.isPresent());
        assertEquals("meta", layer.get());
    }
}
