package com.blackduck.integration.detectable.detectables.go.vendor;

import java.io.File;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.blackduck.integration.bdio.graph.DependencyGraph;
import com.blackduck.integration.bdio.model.externalid.ExternalIdFactory;
import com.blackduck.integration.detectable.detectable.codelocation.CodeLocation;
import com.blackduck.integration.detectable.detectables.go.vendor.parse.GoVendorJsonParser;
import com.blackduck.integration.detectable.extraction.Extraction;

public class GoVendorExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Gson gson;
    private final ExternalIdFactory externalIdFactory;

    public GoVendorExtractor(Gson gson, ExternalIdFactory externalIdFactory) {
        this.gson = gson;
        this.externalIdFactory = externalIdFactory;
    }

    public Extraction extract(File vendorJsonFile) {
        try {
            GoVendorJsonParser vendorJsonParser = new GoVendorJsonParser(externalIdFactory); //TODO: This should be injected.
            String vendorJsonContents = FileUtils.readFileToString(vendorJsonFile, StandardCharsets.UTF_8);

            DependencyGraph dependencyGraph = vendorJsonParser.parseVendorJson(gson, vendorJsonContents);
            CodeLocation codeLocation = new CodeLocation(dependencyGraph);
            return new Extraction.Builder().success(codeLocation).build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }
}
