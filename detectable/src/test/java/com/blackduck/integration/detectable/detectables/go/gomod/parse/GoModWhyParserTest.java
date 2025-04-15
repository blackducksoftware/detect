package com.blackduck.integration.detectable.detectables.go.gomod.parse;

import com.blackduck.integration.detectable.detectables.go.gomod.parse.GoModWhyParser;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.util.FunctionalTestFiles;

public class GoModWhyParserTest {

    @Test
    public void testParserUnused() throws IOException {
        Set<String> expectedExclusionSet = new LinkedHashSet<>();
        expectedExclusionSet.add("example.com/invalid-module-1");
        expectedExclusionSet.add("example.com/invalid-module-2");
        expectedExclusionSet.add("example.com/invalid-module-3");

        // Upgrades to the GoModWhyParser means this test case no longer breaks the parser
        expectedExclusionSet.add("example.com/invalid-missing-end-paren");

        List<String> lines = FunctionalTestFiles.asListOfStrings("/go/gomod-test1/gomodwhy.xout");
        GoModWhyParser goModWhyParser = new GoModWhyParser();

        Set<String> actualExclusionSet = goModWhyParser.createModuleExclusionList(lines);

        assertEquals(expectedExclusionSet, actualExclusionSet);
    }

}
