package com.blackduck.integration.detectable.detectables.bitbake.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.bitbake.data.ShowRecipesResults;
import com.blackduck.integration.detectable.detectables.bitbake.parse.BitbakeRecipesParser;

public class BitbakeRecipesParserTest {

    private final List<String> showRecipesOutputLines = Arrays.asList(
        "### Shell environment set up for builds. ###\n",
        "=== Available recipes: ===\n",
        "a2jmidid:\n",
        "  meta-oe              9\n",
        "abseil-cpp:\n",
        "  meta-oe              git (skipped: abseil-cpp-20190808+gitAUTOINC+aa844899c9 Needs support for corei7 on x86_64)\n",
        "acl:\n",
        "  meta                 2.2.53\n",
        "adcli:\n",
        "  meta-networking      0.8.2"
    );

    @Test
    void test() {
        BitbakeRecipesParser parser = new BitbakeRecipesParser();
        ShowRecipesResults results = parser.parseShowRecipes(showRecipesOutputLines);
        assertEquals(3, results.getLayerNames().size());
        assertTrue(results.getLayerNames().contains("meta"));
        assertEquals(4, results.getRecipesWithLayers().size());
        assertTrue(results.getRecipesWithLayers().containsKey("adcli"));
    }

    @Test
    void preservesRecipeWithEmptyLayerListWhenRecipeDetailsCannotBeParsed() {
        List<String> malformedShowRecipesOutputLines = Arrays.asList(
            "=== Available recipes: ===\n",
            "broken-recipe:\n",
            "  malformed-layer-line\n",
            "valid-recipe:\n",
            "  meta 1.0\n"
        );

        BitbakeRecipesParser parser = new BitbakeRecipesParser();
        ShowRecipesResults results = parser.parseShowRecipes(malformedShowRecipesOutputLines);

        assertTrue(results.getRecipesWithLayers().containsKey("broken-recipe"));
        assertTrue(results.getRecipesWithLayers().get("broken-recipe").isEmpty());
        assertEquals(Arrays.asList("meta"), results.getRecipesWithLayers().get("valid-recipe"));
    }
}
