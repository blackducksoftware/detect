package com.blackduck.integration.detect.workflow.codelocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import com.blackduck.integration.detect.workflow.file.DetectFileUtils;
import com.blackduck.integration.util.NameVersion;

public class CodeLocationNameGenerator {
    private static final int MAXIMUM_CODE_LOCATION_NAME_LENGTH = 250;

    private final Map<String, Integer> nameCounters = new HashMap<>();

    @Nullable
    private final String codeLocationNameOverride;
    @Nullable
    private final String prefix;
    @Nullable
    private final String suffix;

    public static CodeLocationNameGenerator withOverride(String codeLocationNameOverride) {
        return new CodeLocationNameGenerator(codeLocationNameOverride, null, null);
    }

    public static CodeLocationNameGenerator withPrefixSuffix(String prefix, String suffix) {
        return new CodeLocationNameGenerator(null, prefix, suffix);
    }

    public static CodeLocationNameGenerator noChanges() {
        return new CodeLocationNameGenerator(null, null, null);
    }

    private CodeLocationNameGenerator(@Nullable String codeLocationNameOverride, @Nullable String prefix, @Nullable String suffix) {
        this.codeLocationNameOverride = codeLocationNameOverride;
        this.prefix = prefix;
        this.suffix = suffix;
    }

    public String createBomCodeLocationName(
        File detectSourcePath,
        File sourcePath,
        String projectName,
        String projectVersionName,
        DetectCodeLocation detectCodeLocation
    ) {
        String canonicalDetectSourcePath = DetectFileUtils.tryGetCanonicalPath(detectSourcePath);
        String canonicalSourcePath = DetectFileUtils.tryGetCanonicalPath(sourcePath);
        String pathPiece = FileNameUtils.relativize(canonicalDetectSourcePath, canonicalSourcePath);

        String externalIdPiece = StringUtils.join(detectCodeLocation.getExternalId().getExternalIdPieces(), "/");

        // misc pieces
        String codeLocationTypeString = CodeLocationNameType.BOM.getName();
        String bomToolTypeString = deriveCreator(detectCodeLocation).toLowerCase();

        List<String> bomCodeLocationNamePieces = new ArrayList<>();
        bomCodeLocationNamePieces.add(projectName);
        bomCodeLocationNamePieces.add(projectVersionName);
        if (StringUtils.isNotBlank(pathPiece)) {
            bomCodeLocationNamePieces.add(pathPiece);
        }
        bomCodeLocationNamePieces.add(externalIdPiece);
        List<String> bomCodeLocationEndPieces = Arrays.asList(bomToolTypeString, codeLocationTypeString);

        return createCodeLocationName(prefix, bomCodeLocationNamePieces, suffix, bomCodeLocationEndPieces);
    }

    public String createDockerScanCodeLocationName(File dockerTar, String projectName, String projectVersionName) {
        String codeLocationTypeString = CodeLocationNameType.SIGNATURE.getName();

        String dockerTarFileName = DetectFileUtils.tryGetCanonicalName(dockerTar);
        List<String> fileCodeLocationNamePieces = Arrays.asList(dockerTarFileName, projectName, projectVersionName);
        List<String> fileCodeLocationEndPieces = Collections.singletonList(codeLocationTypeString);

        return createCodeLocationName(prefix, fileCodeLocationNamePieces, suffix, fileCodeLocationEndPieces);
    }

    public String createScanCodeLocationName(File sourcePath, File scanTargetPath, String projectName, String projectVersionName) {
        String pathPiece = cleanScanTargetPath(scanTargetPath, sourcePath);
        String codeLocationTypeString = CodeLocationNameType.SIGNATURE.getName();

        List<String> fileCodeLocationNamePieces = Arrays.asList(pathPiece, projectName, projectVersionName);
        List<String> fileCodeLocationEndPieces = Collections.singletonList(codeLocationTypeString);

        return createCodeLocationName(prefix, fileCodeLocationNamePieces, suffix, fileCodeLocationEndPieces);
    }

    public String createBinaryScanCodeLocationName(File targetFile, String projectName, String projectVersionName) {
        String codeLocationTypeString = CodeLocationNameType.BINARY.getName();

        String canonicalFileName = DetectFileUtils.tryGetCanonicalName(targetFile);
        List<String> fileCodeLocationNamePieces = Arrays.asList(canonicalFileName, projectName, projectVersionName);
        List<String> fileCodeLocationEndPieces = Collections.singletonList(codeLocationTypeString);

        return createCodeLocationName(prefix, fileCodeLocationNamePieces, suffix, fileCodeLocationEndPieces);
    }

    public String createContainerScanCodeLocationName(File targetFile, String projectName, String projectVersionName) {
        String codeLocationTypeString = CodeLocationNameType.CONTAINER.getName();

        String canonicalFileName = DetectFileUtils.tryGetCanonicalName(targetFile);
        List<String> fileCodeLocationNamePieces = Arrays.asList(canonicalFileName, projectName, projectVersionName);
        List<String> fileCodeLocationEndPieces = Collections.singletonList(codeLocationTypeString);

        return createCodeLocationName(prefix, fileCodeLocationNamePieces, suffix, fileCodeLocationEndPieces);
    }

    public String createImpactAnalysisCodeLocationName(File sourceDirectory, String projectName, String projectVersionName) {
        String codeLocationTypeString = CodeLocationNameType.IMPACT_ANALYSIS.getName();

        String canonicalFileName = DetectFileUtils.tryGetCanonicalName(sourceDirectory);
        List<String> fileCodeLocationNamePieces = Arrays.asList(canonicalFileName, projectName, projectVersionName);
        List<String> fileCodeLocationEndPieces = Collections.singletonList(codeLocationTypeString);

        return createCodeLocationName(prefix, fileCodeLocationNamePieces, suffix, fileCodeLocationEndPieces);
    }

    public String createIacScanCodeLocationName(
        File targetFile, String projectName, String projectVersionName, @Nullable String prefix,
        @Nullable String suffix
    ) {
        String codeLocationTypeString = CodeLocationNameType.IAC.getName();

        String canonicalFileName = DetectFileUtils.tryGetCanonicalName(targetFile);
        List<String> fileCodeLocationNamePieces = Arrays.asList(canonicalFileName, projectName, projectVersionName);
        List<String> fileCodeLocationEndPieces = Collections.singletonList(codeLocationTypeString);

        return createCodeLocationName(prefix, fileCodeLocationNamePieces, suffix, fileCodeLocationEndPieces);
    }

    public String createAggregateStandardCodeLocationName(NameVersion projectNameVersion) {
        // Add the user supplied prefix and suffix, if they exist, to the project name and project version name.
        // Add Black Duck I/O Export at the end of the code location name, even after the user suffix. This
        // indicates a package manager scan.
        List<String> codeLocationNamePieces = Arrays.asList(projectNameVersion.getName(), projectNameVersion.getVersion());
        List<String> codeLocationEndPieces = Collections.singletonList(CodeLocationNameType.BOM.getName());

        return createCodeLocationName(prefix, codeLocationNamePieces, suffix, codeLocationEndPieces);
    }

    private String createCodeLocationName(@Nullable String prefix, List<String> codeLocationNamePieces, @Nullable String suffix, List<String> codeLocationEndPieces) {
        String codeLocationName = createCommonName(prefix, codeLocationNamePieces, suffix, codeLocationEndPieces);

        if (codeLocationName.length() > MAXIMUM_CODE_LOCATION_NAME_LENGTH) {
            codeLocationName = createShortenedCodeLocationName(codeLocationNamePieces, codeLocationEndPieces);
        }

        return codeLocationName;
    }

    private String cleanScanTargetPath(File scanTargetPath, File sourcePath) {
        String canonicalTargetPath = DetectFileUtils.tryGetCanonicalPath(scanTargetPath);
        String canonicalSourcePath = DetectFileUtils.tryGetCanonicalPath(sourcePath);

        String finalSourcePathPiece = DetectFileUtils.extractFinalPieceFromPath(canonicalSourcePath);
        String cleanedTargetPath = "";
        if (StringUtils.isNotBlank(canonicalTargetPath) && StringUtils.isNotBlank(finalSourcePathPiece)) {
            cleanedTargetPath = canonicalTargetPath.replace(canonicalSourcePath, finalSourcePathPiece);
        }

        return cleanedTargetPath;
    }

    private String createShortenedCodeLocationName(List<String> namePieces, List<String> endPieces) {
        List<String> shortenedNamePieces = namePieces.stream().map(this::shortenPiece).collect(Collectors.toList());

        String shortenedPrefix = shortenPiece(this.prefix);
        String shortenedSuffix = shortenPiece(this.suffix);

        return createCommonName(shortenedPrefix, shortenedNamePieces, shortenedSuffix, endPieces);
    }

    private String shortenPiece(@Nullable String piece) {
        if (piece == null || piece.length() <= 40) {
            return piece;
        } else {
            return piece.substring(0, 19) + "..." + piece.substring(piece.length() - 18);
        }
    }

    private String createCommonName(@Nullable String prefix, List<String> namePieces, @Nullable String suffix, List<String> endPieces) {
        ArrayList<String> commonNamePieces = new ArrayList<>();

        if (StringUtils.isNotBlank(prefix)) {
            commonNamePieces.add(prefix);
        }

        commonNamePieces.addAll(namePieces);

        if (StringUtils.isNotBlank(suffix)) {
            commonNamePieces.add(suffix);
        }

        String name = StringUtils.join(commonNamePieces, "/");
        String endPiece = StringUtils.join(endPieces, "/");

        return String.format("%s %s", name, endPiece);
    }

    public boolean useCodeLocationOverride() {
        return StringUtils.isNotBlank(codeLocationNameOverride);
    }

    public String getNextCodeLocationOverrideNameUnSourced(CodeLocationNameType codeLocationNameType) {
        String baseName = codeLocationNameOverride + " " + codeLocationNameType.getName();
        int nameIndex = deriveNameNumber(baseName);
        return deriveUniqueCodeLocationName(baseName, nameIndex);
    }

    public String deriveCreator(DetectCodeLocation detectCodeLocation) {
        return detectCodeLocation.getCreatorName().orElse("detect");
    }

    public void resetNameOverrideCounter(CodeLocationNameType codeLocationNameType) {
        String baseName = codeLocationNameOverride + " " + codeLocationNameType.getName();
            
        if (nameCounters.containsKey(baseName)) {
            nameCounters.put(baseName, 0);
        }
    }

    private String deriveUniqueCodeLocationName(String baseName, int nameIndex) {
        String nextName;
        if (nameIndex > 1) {
            nextName = baseName + " " + nameIndex;
        } else {
            nextName = baseName;
        }
        return nextName;
    }

    private int deriveNameNumber(String baseName) {
        int nameIndex;
        if (nameCounters.containsKey(baseName)) {
            nameIndex = nameCounters.get(baseName);
            nameIndex++;
        } else {
            nameIndex = 1;
        }
        nameCounters.put(baseName, nameIndex);
        return nameIndex;
    }
}
