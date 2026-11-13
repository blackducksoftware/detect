package com.blackduck.integration.detectable.detectables.yarn.parse.entry.section;

import java.util.List;
import java.util.StringTokenizer;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockLineAnalyzer;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryBuilder;

public class YarnLockKeyValuePairSectionParser implements YarnLockEntrySectionParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final YarnLockLineAnalyzer yarnLockLineAnalyzer;
    private final String targetKey;
    private final BiConsumer<YarnLockEntryBuilder, String> valueConsumer;

    public YarnLockKeyValuePairSectionParser(YarnLockLineAnalyzer yarnLockLineAnalyzer, String targetKey, BiConsumer<YarnLockEntryBuilder, String> valueConsumer) {
        this.yarnLockLineAnalyzer = yarnLockLineAnalyzer;
        this.targetKey = targetKey;
        this.valueConsumer = valueConsumer;
    }

    @Override
    public boolean applies(String sectionFirstLine) {
        if (yarnLockLineAnalyzer.measureIndentDepth(sectionFirstLine) != 1) {
            return false;
        }
        sectionFirstLine = sectionFirstLine.trim();
        if (!sectionFirstLine.contains(" ") && sectionFirstLine.endsWith(":")) {
            return false; // This is just a key:
        }
        StringTokenizer tokenizer = TokenizerFactory.createKeyValueTokenizer(sectionFirstLine);
        String parsedKey = tokenizer.nextToken();
        parsedKey = yarnLockLineAnalyzer.unquote(parsedKey);
        return targetKey.equalsIgnoreCase(parsedKey) && tokenizer.hasMoreTokens();
    }

    @Override
    public int parseSection(YarnLockEntryBuilder entryBuilder, List<String> yarnLockLines, int lineIndexOfStartOfSection) {
        StringTokenizer tokenizer = TokenizerFactory.createKeyValueTokenizer(yarnLockLines.get(lineIndexOfStartOfSection));
        tokenizer.nextToken(); // skip over key
        // Take the remainder of the line so values that contain ':' (e.g. resolution: "name@npm:1.0.0") stay whole
        String value = tokenizer.nextToken("").trim();
        if (value.startsWith(":")) {
            value = value.substring(1).trim();
        }
        value = yarnLockLineAnalyzer.unquote(value);
        logger.trace("\t{}: {}", targetKey, value);
        valueConsumer.accept(entryBuilder, value);
        return lineIndexOfStartOfSection;
    }
}
