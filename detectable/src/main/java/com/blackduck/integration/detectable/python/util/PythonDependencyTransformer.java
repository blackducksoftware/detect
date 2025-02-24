package com.blackduck.integration.detectable.python.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class PythonDependencyTransformer {

    private static final List<String> OPERATORS_IN_PRIORITY_ORDER = Arrays.asList("==", ">=", "~=", "<=", ">", "<");
    private static final List<String> IGNORE_AFTER_CHARS = Arrays.asList("#", ";");
    private static final List<String> TOKEN_CLEANUP_CHARS = Arrays.asList("\"", "'");
    private static final List<String> TOKEN_IGNORE_AFTER_CHARS = Arrays.asList(",", "[", "==", ">=", "~=", "<=", ">", "<");

    public List<PythonDependency> transform(File requirementsFile) throws IOException {

        List<PythonDependency> dependencies = new LinkedList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(requirementsFile))) {
            for (String line; (line = bufferedReader.readLine()) != null; ) {
                
                PythonDependency requirementsFileDependency = transformLine(line);
                
                if (requirementsFileDependency != null) {
                    dependencies.add(requirementsFileDependency);
                }
            }
        }
        return dependencies;
    }

    public PythonDependency transformLine(String line) {
        // Ignore comments (i.e. lines starting with #) and empty/whitespace lines.
        String formattedLine = formatLine(line);
        if (formattedLine.isEmpty() || formattedLine.startsWith("#")) {
            return null;
        }

        // Extract tokens before and after the operator that was found in the line
        List<List<String>> extractedTokens = extractTokens(formattedLine);
        List<String> tokensBeforeOperator = extractedTokens.get(0);
        List<String> tokensAfterOperator = extractedTokens.get(1);
        
        // Extract dependency. This will always be the first token or a substring of first token for each valid line.
        // Format and cleanup each token
        String dependency = "";
        if (tokensBeforeOperator != null && !tokensBeforeOperator.isEmpty()) {
            dependency = formatToken(tokensBeforeOperator.get(0));
        }
        
        // Extract version. Version extracted will be the next token after operator.
        String version = "";
        if (tokensAfterOperator != null && !tokensAfterOperator.isEmpty()) {
            version = formatToken(tokensAfterOperator.get(0));
        }
        
        // Create a dependency entry and add it to the list
        // Version can be an empty string but dependency name should always be non-empty
        if (!dependency.isEmpty()) {
             return new PythonDependency(dependency, version);
        } else {
            return null;
        }
    }

    public List<List<String>> extractTokens(String formattedLine) {
        // Note: The line is always a valid line to extract from at this point since it has passed all the checks
        // Hence it will contain at least the dependency. Version may or may not be present.

        List<String> tokensBeforeOperator;
        List<String> tokensAfterOperator = null;

        // Find the operator with its index that separates a dependency from its version.
        List<Object> operatorWithIndex = findOperatorWithIndex(formattedLine);
        String operatorFound = (String) operatorWithIndex.get(0);
        if (!operatorFound.isEmpty()) {
            int operatorStartIndex = (int) operatorWithIndex.get(1);
            int operatorEndIndex = operatorStartIndex + operatorFound.length() - 1;

            // Get strings before and after operator
            String stringBeforeOperator = formattedLine.substring(0, operatorStartIndex).trim();
            String stringAfterOperator = formattedLine.substring(operatorEndIndex + 1).trim();

            // Tokenize based on whitespace as the parser should allow special characters in version and dependency strings
            tokensBeforeOperator = Arrays.asList(stringBeforeOperator.split(" "));
            tokensAfterOperator = Arrays.asList(stringAfterOperator.split(" "));
        } else {
            // No operator found. Implies version is missing. Hence, only set tokensBeforeOperator as a tokenized version of original input line.
            tokensBeforeOperator = Arrays.asList(formattedLine.split(" "));
        }
        return Arrays.asList(tokensBeforeOperator, tokensAfterOperator);
    }

    public List<Object> findOperatorWithIndex(String line) {
        int operatorIndex;
        List<Object> operatorWithIndex = new ArrayList<>();
        for (String operator : OPERATORS_IN_PRIORITY_ORDER) {
            operatorIndex = line.indexOf(operator);
            if (operatorIndex != -1) {
                operatorWithIndex.add(operator);
                operatorWithIndex.add(operatorIndex);
                return operatorWithIndex;
            }
        }
        return Arrays.asList("", -1);
    }

    public String formatLine(String line) {
        // Replace null characters (\u0000) with empty string if any
        line = line.replace("\u0000", "");

        // Replace the replacement characters (\uFFFD) with empty string if any
        line = line.replace("\uFFFD", "");
        
        int ignoreAfterIndex;
        String formattedLine = line.trim();

        // Ignore any lines that start with hyphen as these are flags, not dependency entries (can be - or --)
        // Flags are handled in the extractor, not during dependency parsing, as they involve pre-parsing decisions (example: -r flag)
        if (formattedLine.startsWith("-")) {
            return "";
        }

        // Ignore any characters that appear after the chars in the IGNORE_AFTER_CHARACTERS list
        for (String ignoreAfterChar : IGNORE_AFTER_CHARS) {
            ignoreAfterIndex = formattedLine.indexOf(ignoreAfterChar);
            if (ignoreAfterIndex >= 0) {
                formattedLine = formattedLine.substring(0, ignoreAfterIndex);
            }
        }
        return formattedLine.trim();
    }

    public String formatToken(String token) {
        // If a token was extracted with operators or a comma, ignore all characters after the operator/comma
        // Ignore any Python package "extras" present in the token name. For example, if token is requests["foo", "bar"], it should be cleaned up to show as "requests"
        int ignoreAfterIndex;
        for (String ignoreAfterChar : TOKEN_IGNORE_AFTER_CHARS) {
            ignoreAfterIndex = token.indexOf(ignoreAfterChar);
            if (ignoreAfterIndex >= 0) {
                token = token.substring(0, ignoreAfterIndex);
            }
        }
        // Clean up any irrelevant symbols/chars from token, like double quotes, single quotes, etc.
        for (String charToRemove : TOKEN_CLEANUP_CHARS) {
            token = token.replace(charToRemove, "");
        }
        return token;
    }
}
