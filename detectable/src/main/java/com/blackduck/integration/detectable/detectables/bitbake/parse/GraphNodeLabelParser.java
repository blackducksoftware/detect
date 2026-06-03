package com.blackduck.integration.detectable.detectables.bitbake.parse;

import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.bitbake.data.GraphNodeLabelDetails;

// Example of a GraphNode label value:
// acl-native do_compile\n:2.3.1-r0\nvirtual:native:/workdir/poky/meta/recipes-support/attr/acl_2.3.1.bb
// Split into:
//  acl-native do_compile -> GraphNodeLabelDetails.nameType
//  2.3.1-r0 -> GraphNodeLabelDetails.version
//  virtual:native:/workdir/poky/meta/recipes-support/attr/acl_2.3.1.bb -> GraphNodeLabelDetails.recipeSpec
public class GraphNodeLabelParser {
    private static final String LABEL_PATH_SEPARATOR = "/";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Optional<String> parseVersionFromLabel(String label) {
        return parseLabelParts(label).map(GraphNodeLabelDetails::getVersion);
    }

    public Optional<String> parseLayerFromLabel(String label, Set<String> knownLayerNames) {
        Optional<String> recipeSpec = parseLabelParts(label)
            .map(GraphNodeLabelDetails::getRecipeSpec);
        if (!recipeSpec.isPresent()) {
            return Optional.empty();
        }

        // Find the deepest (rightmost) matching layer in the path
        // This ensures we match the actual layer folder, not parent folders
        // that happen to share a name with a known layer
        String deepestMatch = null;
        int deepestPosition = -1;

        for (String candidateLayerName : knownLayerNames) {
            String possibleLayerPathSubstring = LABEL_PATH_SEPARATOR + candidateLayerName + LABEL_PATH_SEPARATOR;
            int position = recipeSpec.get().lastIndexOf(possibleLayerPathSubstring);
            if (position > deepestPosition) {
                deepestPosition = position;
                deepestMatch = candidateLayerName;
            }
        }

        if (deepestMatch == null) {
            logger.warn("Graph Node recipe '{}' does not correspond to any known layer ({})", label, knownLayerNames);
        }
        return Optional.ofNullable(deepestMatch);
    }

    @NotNull
    private Optional<GraphNodeLabelDetails> parseLabelParts(String label) {
        String[] labelParts = label.split("\\\\n:|\\\\n");
        if (labelParts.length < 3) {
            logger.warn("Error parsing Graph Node label '{}'; unexpected format.", label);
            return Optional.empty();
        }
        return Optional.of(new GraphNodeLabelDetails(labelParts[0], labelParts[1], labelParts[2]));
    }
}
