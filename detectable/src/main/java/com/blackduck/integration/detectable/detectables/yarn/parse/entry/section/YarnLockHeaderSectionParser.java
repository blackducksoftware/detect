package com.blackduck.integration.detectable.detectables.yarn.parse.entry.section;

import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.yarn.parse.YarnLockLineAnalyzer;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryBuilder;
import com.blackduck.integration.detectable.detectables.yarn.parse.entry.YarnLockEntryId;

public class YarnLockHeaderSectionParser implements YarnLockEntrySectionParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final YarnLockLineAnalyzer yarnLockLineAnalyzer;

    public YarnLockHeaderSectionParser(YarnLockLineAnalyzer yarnLockLineAnalyzer) {
        this.yarnLockLineAnalyzer = yarnLockLineAnalyzer;
    }

    @Override
    public boolean applies(String sectionFirstLine) {
        return yarnLockLineAnalyzer.measureIndentDepth(sectionFirstLine) == 0;
    }

    @Override
    public int parseSection(YarnLockEntryBuilder entryBuilder, List<String> yarnLockLines, int lineIndexOfStartOfSection) {
        String line = yarnLockLines.get(lineIndexOfStartOfSection).trim();
        line = StringUtils.removeEnd(line, ":").trim();
        line = yarnLockLineAnalyzer.unquote(line);
        if ("__metadata".equals(line)) {
            entryBuilder.setMetadataEntry(true);
        } else {
            StringTokenizer tokenizer = TokenizerFactory.createHeaderTokenizer(line);
            while (tokenizer.hasMoreTokens()) {
                String rawEntryString = tokenizer.nextToken().trim();
                String entryString = StringUtils.removeEnd(rawEntryString, ":").trim();
                String unquotedEntryString = yarnLockLineAnalyzer.unquote(entryString);
                // Keep the descriptor as written (before the trailing ':' strip, which would turn "name@npm:" into "name@npm")
                String rawDescriptor = yarnLockLineAnalyzer.unquote(rawEntryString);
                YarnLockEntryId entry = parseSingleEntry(unquotedEntryString, rawDescriptor);
                logger.trace("Entry header ID: name: {}, version: {}", entry.getName(), entry.getVersion());
                entryBuilder.addId(entry);
            }
        }
        return lineIndexOfStartOfSection;
    }

    //Takes an entry of format "name@version" or "@name@version" where name has an @ symbol.
    //Notice, this removes the workspace, so "name@workspace:version" will become simply "name@version"
    private YarnLockEntryId parseSingleEntry(String entry, String rawDescriptor) {
        YarnLockEntryId normalEntry = parseSingleEntryNormally(entry, rawDescriptor);
        if (normalEntry.getVersion().startsWith("npm:")) {
            return new YarnLockEntryId(normalEntry.getName(), StringUtils.substringAfter(normalEntry.getVersion(), ":"), rawDescriptor);
        } else {
            return normalEntry;
        }
    }

    private YarnLockEntryId parseSingleEntryNormally(String entry, String rawDescriptor) {
        if (StringUtils.countMatches(entry, "@") == 1 && entry.startsWith("@")) {
            return new YarnLockEntryId(entry, "", rawDescriptor);
        } else {
            String name = StringUtils.substringBeforeLast(entry, "@");
            String version = StringUtils.substringAfterLast(entry, "@");
            return new YarnLockEntryId(name, version, rawDescriptor);
        }
    }
}
