package com.blackduck.integration.detectable.detectables.go.vendor;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.common.util.finder.FileFinder;
import com.blackduck.integration.detectable.Detectable;
import com.blackduck.integration.detectable.DetectableEnvironment;
import com.blackduck.integration.detectable.detectable.DetectableAccuracyType;
import com.blackduck.integration.detectable.detectable.Requirements;
import com.blackduck.integration.detectable.detectable.annotation.DetectableInfo;
import com.blackduck.integration.detectable.detectable.result.DetectableResult;
import com.blackduck.integration.detectable.detectable.result.PassedDetectableResult;
import com.blackduck.integration.detectable.extraction.Extraction;
import com.blackduck.integration.detectable.extraction.ExtractionEnvironment;

@DetectableInfo(name = "Go Vendor V2", language = "Golang", forge = "GitHub", accuracy = DetectableAccuracyType.HIGH, requirementsMarkdown = "File: vendor/modules.txt.")
public class GoVendorV2Detectable extends Detectable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String VENDOR_DIRNAME = "vendor";
    private static final String MODULES_TXT_FILENAME = "modules.txt";

    private final FileFinder fileFinder;
    private final GoVendorV2Extractor goVendorV2Extractor;

    private File modulesTxt;

    public GoVendorV2Detectable(DetectableEnvironment environment, FileFinder fileFinder, GoVendorV2Extractor goVendorV2Extractor) {
        super(environment);
        this.fileFinder = fileFinder;
        this.goVendorV2Extractor = goVendorV2Extractor;
    }

    @Override
    public DetectableResult applicable() {
        Requirements requirements = new Requirements(fileFinder, environment);
        File vendorDir = requirements.directory(VENDOR_DIRNAME);
        modulesTxt = requirements.file(vendorDir, MODULES_TXT_FILENAME);
        return requirements.result();
    }

    @Override
    public DetectableResult extractable() {
        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(ExtractionEnvironment extractionEnvironment) {
        return goVendorV2Extractor.extract(modulesTxt);
    }
}