package com.blackduck.integration.detectable.detectables.go.vendor;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.go.vendor.parse.GoVendorModulesTxtParser;
import com.blackduck.integration.detectable.extraction.Extraction;

public class GoVendorV2Extractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExternalIdFactory externalIdFactory;

    public GoVendorV2Extractor(ExternalIdFactory externalIdFactory) {
        this.externalIdFactory = externalIdFactory;
    }

    public Extraction extract(File modulesTxtFile) {
        try {
            GoVendorModulesTxtParser modulesTxtParser = new GoVendorModulesTxtParser(externalIdFactory); // Should be injected if possible.
            String modulesTxtContents = FileUtils.readFileToString(modulesTxtFile, StandardCharsets.UTF_8);

            DependencyGraph dependencyGraph = modulesTxtParser.parseModulesTxt(modulesTxtContents);
            CodeLocation codeLocation = new CodeLocation(dependencyGraph);
            return new Extraction.Builder().success(codeLocation).build();
        } catch (Exception e) {
            logger.error("Failed to extract Go vendor modules.txt", e);
            return new Extraction.Builder().exception(e).build();
        }
    }
}