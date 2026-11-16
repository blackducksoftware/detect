package com.blackduck.integration.detectable.detectables.yarn.unit.parse.entry.element;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockLineAnalyzer;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntry;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryBuilder;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryId;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.section.YarnLockKeyValuePairSectionParser;

public class YarnLockKeyValuePairSectionParserTest {
    private static YarnLockKeyValuePairSectionParser yarnLockKeyValuePairElementParser;

    @BeforeAll
    static void setup() {
        yarnLockKeyValuePairElementParser = new YarnLockKeyValuePairSectionParser(
            new YarnLockLineAnalyzer(), "version", YarnLockEntryBuilder::setVersion);
    }

    @Test
    void testWithoutColonWithoutQuotes() {
        doTest("  version test.version.value", "test.version.value");
    }

    @Test
    void testWithoutColonWithQuotes() {
        doTest("  version \"test.version.value\"", "test.version.value");
    }

    @Test
    void testWithColonWithoutQuotes() {
        doTest("  version: test.version.value", "test.version.value");
    }

    @Test
    void testWithColonWithQuotes() {
        doTest("  version: \"test.version.value\"", "test.version.value");
    }

    @Test
    void testQuotedKey() {
        doTest("  \"version\" \"0.2.1\"", "0.2.1");
    }

    private void doTest(String line, String versionValue) {
        Assertions.assertTrue(yarnLockKeyValuePairElementParser.applies(line));

        YarnLockEntryBuilder builder = new YarnLockEntryBuilder();
        builder.addId(new YarnLockEntryId("idname", "idversion"));
        List<String> lines = Arrays.asList(line);
        yarnLockKeyValuePairElementParser.parseSection(builder, lines, 0);

        YarnLockEntry entry = builder.build();
        Assertions.assertEquals(versionValue, entry.getVersion());
    }
}
