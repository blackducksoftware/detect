package com.blackduck.integration.detectable.detectables.bazel.pipeline.step;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BazelVariableSubstitutor {
    private final Map<String, String> stringSubstitutions;
    private final Map<String, List<String>> listInsertions;

    public BazelVariableSubstitutor(String bazelTarget, List<String> cqueryAdditionalOptions, List<String> queryAdditionalOptions) {
        // the keys are regex's, requiring regex special character escaping
        stringSubstitutions = new HashMap<>(1);
        stringSubstitutions.put("\\$\\{detect.bazel.target}", bazelTarget);

        // the keys are plain strings
        listInsertions = new HashMap<>(2);
        listInsertions.put("${detect.bazel.cquery.options}", cqueryAdditionalOptions);
        // Map query options placeholder to the separate query list
        listInsertions.put("${detect.bazel.query.options}", queryAdditionalOptions);
    }

    public List<String> substitute(List<String> origStrings, String input) {
        List<String> modifiedStrings = new ArrayList<>(origStrings.size());
        for (String origString : origStrings) {
            boolean foundListInsertionPlaceholder = handleListInsertion(modifiedStrings, origString, input);
            if (!foundListInsertionPlaceholder) {
                handleStringSubstitutions(input, modifiedStrings, origString);
            }
        }
        return modifiedStrings;
    }

    private void handleStringSubstitutions(String input, List<String> modifiedStrings, String origString) {
        modifiedStrings.add(substitute(origString, input));
    }

    private boolean handleListInsertion(List<String> modifiedStrings, String origString, String input) {
        boolean foundListInsertionVariable = false;
        for (Map.Entry<String, List<String>> listInsertion : listInsertions.entrySet()) {
            if (origString.equals(listInsertion.getKey())) {
                foundListInsertionVariable = true;
                List<String> valuesToInsert = listInsertion.getValue();
                if (valuesToInsert != null) {
                    for (String valueToInsert : valuesToInsert) {
                        // Substituting here gives user the ability to use ${detect.bazel.target} and ${input.item} in list insertion values
                        handleStringSubstitutions(input, modifiedStrings, valueToInsert);
                    }
                }
                break; // it'll only match one
            }
        }
        return foundListInsertionVariable;
    }

    private String substitute(String origString, String input) {
        String modifiedString = origString;
        if (input != null) {
            stringSubstitutions.put("\\$\\{input.item}", input);
        }
        for (Map.Entry<String, String> substitution : stringSubstitutions.entrySet()) {
            modifiedString = modifiedString.replaceAll(substitution.getKey(), substitution.getValue());
        }
        return modifiedString;
    }
}
