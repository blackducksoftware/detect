package com.blackduck.integration.detectable.detectables.rush;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.rush.model.RushJsonParseResult;
import com.blackduck.integration.detectable.detectables.rush.parse.RushJsonParser;
import com.blackduck.integration.detectable.detectables.rush.parse.RushLockFileParser;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.util.NameVersion;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class RushExtractor {
    private final RushJsonParser rushJsonParser;
    private final RushLockFileParser rushLockFileParser;

    public RushExtractor(RushJsonParser rushJsonParser, RushLockFileParser rushLockFileParser) {
        this.rushJsonParser = rushJsonParser;
        this.rushLockFileParser = rushLockFileParser;
    }

    public Extraction extract(File projectDirectory, File rushJsonFle, FileFinder fileFinder) {
        RushJsonParseResult rushJsonParseResult = rushJsonParser.parseRushJsonFile(rushJsonFle);

        try {
            rushJsonParseResult.findAllLockFiles(projectDirectory, fileFinder);
        } catch (IOException e) {
            return new Extraction.Builder().exception(e).build();
        }


        try {
            List<CodeLocation> codeLocations = rushLockFileParser.parse(fileFinder, projectDirectory, rushJsonParseResult);

            if (codeLocations.isEmpty()) {
                return Extraction.failure();
            }

            Optional<NameVersion> nameVersion = rushLockFileParser.parseNameVersion(projectDirectory, fileFinder);
            return new Extraction.Builder().success(codeLocations).nameVersionIfPresent(nameVersion).build();

        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }
}
