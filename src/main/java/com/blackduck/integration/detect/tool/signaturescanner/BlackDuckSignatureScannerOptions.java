package com.blackduck.integration.detect.tool.signaturescanner;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.IndividualFileMatching;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ReducedPersistence;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.SnippetMatching;
import com.blackduck.integration.detect.configuration.enumeration.RapidCompareMode;

public class BlackDuckSignatureScannerOptions {
    private final List<Path> signatureScannerPaths;
    private final List<String> exclusionPatterns;
    @Nullable
    private final Path localScannerInstallPath;

    private final Integer scanMemory;
    private final Integer parallelProcessors;
    private final Boolean dryRun;

    @Nullable //Just to note that if you do not want snippet matching, this should be null.
    private final SnippetMatching snippetMatching;
    
    @Nullable //Just to note that if you want server defaults this should be null.
    private final ReducedPersistence reducedPersistence;

    @Nullable
    private final Boolean uploadSource;
    @Nullable
    private final String additionalArguments;
    private final Integer maxDepth;
    @Nullable
    private final IndividualFileMatching individualFileMatching;
    private final Boolean licenseSearch;
    private final Boolean copyrightSearch;
    private final Boolean followSymLinks;
    private final Boolean treatSkippedScansAsSuccess;
    private final Boolean isStateless;
    private final Boolean correlatedScanningEnabled;
    private final RapidCompareMode bomCompareMode;
    private final Boolean csvArchive;

    public BlackDuckSignatureScannerOptions(
        List<Path> signatureScannerPaths,
        List<String> exclusionPatterns,
        @Nullable Path localScannerInstallPath,
        Integer scanMemory,
        Integer parallelProcessors,
        Boolean dryRun,
        @Nullable SnippetMatching snippetMatching,
        @Nullable Boolean uploadSource,
        @Nullable String additionalArguments,
        Integer maxDepth,
        @Nullable IndividualFileMatching individualFileMatching,
        Boolean licenseSearch,
        Boolean copyrightSearch,
        Boolean followSymLinks,
        Boolean treatSkippedScansAsSuccess,
        Boolean isStateless, 
        ReducedPersistence reducedPersistence,
        Boolean correlatedScanningEnabled,
        RapidCompareMode bomCompareMode,
        Boolean csvArchive
    ) {

        this.signatureScannerPaths = signatureScannerPaths;
        this.exclusionPatterns = exclusionPatterns;
        this.localScannerInstallPath = localScannerInstallPath;
        this.scanMemory = scanMemory;
        this.parallelProcessors = parallelProcessors;
        this.dryRun = dryRun;
        this.snippetMatching = snippetMatching;
        this.uploadSource = uploadSource;
        this.additionalArguments = additionalArguments;
        this.maxDepth = maxDepth;
        this.individualFileMatching = individualFileMatching;
        this.licenseSearch = licenseSearch;
        this.copyrightSearch = copyrightSearch;
        this.followSymLinks = followSymLinks;
        this.treatSkippedScansAsSuccess = treatSkippedScansAsSuccess;
        this.isStateless = isStateless;
        this.reducedPersistence = reducedPersistence;
        this.correlatedScanningEnabled = correlatedScanningEnabled;
        this.bomCompareMode = bomCompareMode;
        this.csvArchive = csvArchive;
    }

    public List<Path> getSignatureScannerPaths() {
        return signatureScannerPaths;
    }

    public List<String> getExclusionPatterns() {
        return exclusionPatterns;
    }

    public Integer getScanMemory() {
        return scanMemory;
    }

    public Integer getParallelProcessors() {
        return parallelProcessors;
    }

    public Boolean getDryRun() {
        return dryRun;
    }

    public Optional<SnippetMatching> getSnippetMatching() {
        return Optional.ofNullable(snippetMatching);
    }

    public Boolean getUploadSource() {
        return uploadSource;
    }

    public Optional<String> getAdditionalArguments() {
        return Optional.ofNullable(additionalArguments);
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public Optional<Path> getLocalScannerInstallPath() {
        return Optional.ofNullable(localScannerInstallPath);
    }

    public Optional<IndividualFileMatching> getIndividualFileMatching() {
        return Optional.ofNullable(individualFileMatching);
    }

    public Boolean getLicenseSearch() {
        return licenseSearch;
    }

    public Boolean getCopyrightSearch() {
        return copyrightSearch;
    }

    public Boolean followSymLinks() {
        return followSymLinks;
    }

    public Boolean getTreatSkippedScansAsSuccess() {
        return treatSkippedScansAsSuccess;
    }

    public Boolean getIsStateless() {
        return isStateless;
    }
    
    public Optional<ReducedPersistence> getReducedPersistence() {
        return Optional.ofNullable(reducedPersistence);
    }

    public Boolean isCorrelatedScanningEnabled() {
        return correlatedScanningEnabled;
    }
    
    public RapidCompareMode getBomCompareMode() {
        return bomCompareMode;
    }
    
    public Boolean getCsvArchive() {
        return csvArchive;
    }
}
