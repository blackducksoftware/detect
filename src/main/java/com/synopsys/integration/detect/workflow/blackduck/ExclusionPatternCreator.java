/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.workflow.blackduck;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.common.util.finder.FileFinder;

public class ExclusionPatternCreator {
    private final Logger logger = LoggerFactory.getLogger(ExclusionPatternCreator.class);

    private final FileFinder fileFinder;
    private final Predicate<File> fileFilter;
    private final File scanTarget;

    public ExclusionPatternCreator(final FileFinder fileFinder, final Predicate<File> fileFilter, final File scanTarget) {
        this.fileFinder = fileFinder;
        this.fileFilter = fileFilter;
        this.scanTarget = scanTarget;
    }

    public Set<String> determineExclusionPatterns(final int maxDepth, final List<String> exclusionPatterns) {
        if (CollectionUtils.isEmpty(exclusionPatterns) && scanTarget.isDirectory()) {
            return Collections.emptySet();
        }
        final Set<String> scanExclusionPatterns = new HashSet<>();

        // Now use patterns to resolve exclusions that we will format and pass on to the signature scanner
        try {
            final String scanTargetPath = scanTarget.getCanonicalPath();
            // TODO should we only collect directories since the scanner can only exclude directories?
            final List<File> matchingFiles = fileFinder.findFiles(scanTarget, fileFilter, maxDepth, false); //TODO: re-add the depth hit message creator?
            for (final File matchingFile : matchingFiles) {
                final String matchingFilePath = matchingFile.getCanonicalPath();
                final String scanExclusionPattern = createExclusionPatternFromPaths(scanTargetPath, matchingFilePath);
                scanExclusionPatterns.add(scanExclusionPattern);
            }
        } catch (final IOException e) {
            logger.warn("Problem encountered finding the exclusion patterns for the scanner. " + e.getMessage());
            logger.debug(e.getMessage(), e);
        }
        return scanExclusionPatterns;
    }

    private String createExclusionPatternFromPaths(final String rootPath, final String targetPath) {
        String scanExclusionPattern = targetPath.replace(rootPath, "/");
        if (scanExclusionPattern.contains("\\\\")) {
            scanExclusionPattern = scanExclusionPattern.replace("\\\\", "/");
        }
        if (scanExclusionPattern.contains("\\")) {
            scanExclusionPattern = scanExclusionPattern.replace("\\", "/");
        }
        if (scanExclusionPattern.contains("//")) {
            scanExclusionPattern = scanExclusionPattern.replace("//", "/");
        }
        if (!scanExclusionPattern.endsWith("/")) {
            scanExclusionPattern = scanExclusionPattern + "/";
        }
        return scanExclusionPattern;
    }
}
