package com.blackduck.integration.detect.util.bdio.protobuf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.zip.ZipOutputStream;

import com.blackducksoftware.bdio.proto.ProtobufBdioWriter;
import com.blackducksoftware.bdio.proto.api.BdioHeader;
import com.blackduck.integration.detect.util.DetectZipUtil;
import com.blackduck.integration.util.NameVersion;

public class DetectProtobufBdioHeaderUtil {
    private final String scanId;
    private final String scanType;
    private final String codeLocationName;

    private final NameVersion projectNameVersion;
    private final String publisherName;
    private final String publisherVersion;
    private final String publisherComment;
    private static final String CREATOR_NAME = "DETECT";
    private final Instant creationTime;
    private final String sourceRepository;
    private final String sourceBranch;
    private final String projectGroupName;
    private final UUID correlationId;
    private final Long matchConfidenceThreshold;
    private final String baseDir;
    private final Boolean withStringSearch;
    private final Boolean withSnippetMatching;
    private final Boolean retainUnmatchedFiles;
    private final Long fileSystemSizeInBytes;

    public DetectProtobufBdioHeaderUtil(String scanId,
        String scanType,
        NameVersion projectNameVersion,
        String projectGroupName,
        String codeLocationName,
        Long fileSystemSizeInBytes,
        String correlationId
    ) {
        this.scanId = scanId;
        this.scanType = scanType;
        this.codeLocationName = codeLocationName;
        this.projectNameVersion = projectNameVersion;
        this.publisherName = "";
        this.publisherVersion = "";
        this.publisherComment = "";
        this.creationTime = Instant.now();
        this.sourceRepository = null;
        this.sourceBranch = null;
        this.projectGroupName = projectGroupName;
        this.correlationId = correlationId != null ? UUID.fromString(correlationId) : null;
        this.matchConfidenceThreshold = 1L;
        this.baseDir = "/";
        this.withStringSearch = false;
        this.withSnippetMatching = false;
        this.retainUnmatchedFiles = null;
        this.fileSystemSizeInBytes = fileSystemSizeInBytes;
    }

    public File createProtobufBdioHeader(File targetDirectory) throws IOException {
        BdioHeader bdioHeader = new BdioHeader(
            scanId,
            scanType,
            codeLocationName,
            projectNameVersion.getName(),
            projectNameVersion.getVersion(),
            publisherName,
            publisherVersion,
            publisherComment,
            CREATOR_NAME,
            creationTime,
            sourceRepository,
            sourceBranch,
            projectGroupName,
            correlationId,
            matchConfidenceThreshold,
            baseDir,
            withStringSearch,
            withSnippetMatching,
            retainUnmatchedFiles,
            fileSystemSizeInBytes)
            ;

        String tempBdioArchivePath = targetDirectory.toPath() + "/bdio-protobuf.zip";
        try (
            FileOutputStream outputStream = new FileOutputStream(tempBdioArchivePath);
            ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
            ProtobufBdioWriter protobufBdioWriter = new ProtobufBdioWriter(zipOutputStream)
        ) {
            protobufBdioWriter.writeHeader(bdioHeader);
        }
        File bdioZipFile = new File(tempBdioArchivePath);
        DetectZipUtil.unzip(bdioZipFile, targetDirectory);
        return new File(targetDirectory.toPath() + "/bdio-header.pb");
    }
}
