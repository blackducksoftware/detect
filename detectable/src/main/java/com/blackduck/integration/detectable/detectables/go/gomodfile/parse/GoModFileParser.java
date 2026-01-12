package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoReplaceDirective;
import com.blackduck.integration.detectable.util.KBComponentHelpers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive parser for go.mod files that handles all directives including:
 * - module declaration
 * - go version
 * - toolchain version
 * - require blocks (direct and indirect dependencies)
 * - exclude directives
 * - replace directives
 * - retract directives
 */
public class GoModFileParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    // Regular expressions for parsing different sections
    private static final Pattern MODULE_PATTERN = Pattern.compile("^module\\s+(.+)$");
    private static final Pattern GO_VERSION_PATTERN = Pattern.compile("^go\\s+(.+)$");
    private static final Pattern TOOLCHAIN_PATTERN = Pattern.compile("^toolchain\\s+(.+)$");
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile("^\\s*([^\\s]+)\\s+([^\\s]+)(?:\\s+//\\s*(.+))?$");
    private static final Pattern REPLACE_PATTERN = Pattern.compile("^\\s*([^\\s]+)(?:\\s+([^\\s]+))?\\s+=>");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("^\\s*//.*$");
    private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*$");
    
    private enum ParseState {
        NORMAL,
        REQUIRE_BLOCK,
        EXCLUDE_BLOCK,
        REPLACE_BLOCK,
        RETRACT_BLOCK
    }

    public GoModFileContent parseGoModFile(String goModFileContent) {
        List<String> lines = Arrays.asList(goModFileContent.split("\\r?\\n"));
        return parseGoModFile(lines);
    }

    /**
     * Parses a go.mod file content and returns structured information.
     * 
     * @param lines List of lines from the go.mod file
     * @return GoModFileContent containing all parsed information
     */
    public GoModFileContent parseGoModFile(List<String> lines) {
        GoModFileContext context = new GoModFileContext();
        ParseState currentState = ParseState.NORMAL;
        
        for (String line : lines) {
            line = line.trim();
            
            if (shouldSkipLine(line)) {
                currentState = handleBlockEnd(line, currentState);
                continue;
            }
            
            currentState = processLine(line, currentState, context);
        }
        
        return context.buildGoModFileContent();
    }

    private boolean shouldSkipLine(String line) {
        return isEmptyOrComment(line) || line.equals(")");
    }

    private ParseState handleBlockEnd(String line, ParseState currentState) {
        return line.equals(")") ? ParseState.NORMAL : currentState;
    }

    private ParseState processLine(String line, ParseState currentState, GoModFileContext context) {
        if (currentState == ParseState.NORMAL) {
            return processNormalStateLine(line, context);
        }
        
        processBlockStateLine(line, currentState, context);
        return currentState;
    }

    private ParseState processNormalStateLine(String line, GoModFileContext context) {
        extractMetadataIfPresent(line, context);
        return parseNormalLine(line, 
                              context.directDependencies, context.indirectDependencies,
                              context.excludedModules, context.replaceDirectives, 
                              context.retractedVersions);
    }

    private void processBlockStateLine(String line, ParseState currentState, GoModFileContext context) {
        switch (currentState) {
            case REQUIRE_BLOCK:
                parseRequireLine(line, context.directDependencies, context.indirectDependencies);
                break;
            case EXCLUDE_BLOCK:
                parseExcludeLine(line, context.excludedModules);
                break;
            case REPLACE_BLOCK:
                parseReplaceLine(line, context.replaceDirectives);
                break;
            case RETRACT_BLOCK:
                parseRetractLine(line, context.retractedVersions);
                break;
            default:
                logger.warn("Encountered line in unknown state {}: {}", currentState, line);
                break;
        }
    }

    private void extractMetadataIfPresent(String line, GoModFileContext context) {
        if (context.moduleName == null) {
            context.moduleName = extractModuleName(line);
        }
        if (context.goVersion == null) {
            context.goVersion = extractGoVersion(line);
        }
        if (context.toolchainVersion == null) {
            context.toolchainVersion = extractToolchainVersion(line);
        }
    }

    private static class GoModFileContext {
        String moduleName = null;
        String goVersion = null;
        String toolchainVersion = null;
        List<GoModuleInfo> directDependencies = new ArrayList<>();
        List<GoModuleInfo> indirectDependencies = new ArrayList<>();
        Set<GoModuleInfo> excludedModules = new HashSet<>();
        List<GoReplaceDirective> replaceDirectives = new ArrayList<>();
        Set<GoModuleInfo> retractedVersions = new HashSet<>();
        
        GoModFileContent buildGoModFileContent() {
            return new GoModFileContent(
                moduleName, goVersion, toolchainVersion,
                directDependencies, indirectDependencies,
                excludedModules, replaceDirectives, retractedVersions
            );
        }
    }
    
    private ParseState parseNormalLine(String line,
                                     List<GoModuleInfo> directDependencies, List<GoModuleInfo> indirectDependencies,
                                     Set<GoModuleInfo> excludedModules, List<GoReplaceDirective> replaceDirectives,
                                     Set<GoModuleInfo> retractedVersions) {
        
        // Check for block starts
        if (line.startsWith("require (")) {
            return ParseState.REQUIRE_BLOCK;
        } else if (line.startsWith("exclude (")) {
            return ParseState.EXCLUDE_BLOCK;
        } else if (line.startsWith("replace (")) {
            return ParseState.REPLACE_BLOCK;
        } else if (line.startsWith("retract (")) {
            return ParseState.RETRACT_BLOCK;
        }
        
        // Handle single-line directives
        if (line.startsWith("require ")) {
            String dependencyLine = line.substring(8).trim();
            parseRequireLine(dependencyLine, directDependencies, indirectDependencies);
        } else if (line.startsWith("exclude ")) {
            String excludeLine = line.substring(8).trim();
            parseExcludeLine(excludeLine, excludedModules);
        } else if (line.startsWith("replace ")) {
            String replaceLine = line.substring(8).trim();
            parseReplaceLine(replaceLine, replaceDirectives);
        } else if (line.startsWith("retract ")) {
            String retractLine = line.substring(8).trim();
            parseRetractLine(retractLine, retractedVersions);
        }
        
        return ParseState.NORMAL;
    }
    
    private void parseRequireLine(String line, List<GoModuleInfo> directDependencies, List<GoModuleInfo> indirectDependencies) {
        GoModuleInfo moduleInfo = parseDependencyLine(line);
        if (moduleInfo != null) {
            if (moduleInfo.isIndirect()) {
                indirectDependencies.add(moduleInfo);
            } else {
                directDependencies.add(moduleInfo);
            }
        }
    }
    
    private void parseExcludeLine(String line, Set<GoModuleInfo> excludedModules) {
        GoModuleInfo moduleInfo = parseDependencyLine(line);
        if (moduleInfo != null) {
            excludedModules.add(moduleInfo);
        }
    }
    
    private void parseReplaceLine(String line, List<GoReplaceDirective> replaceDirectives) {
        // Parse replace directive: old_module [old_version] => new_module [new_version]
        Matcher matcher = REPLACE_PATTERN.matcher(line);
        if (matcher.find()) {
            String[] parts = line.split("=>");
            if (parts.length == 2) {
                GoModuleInfo oldModule = parseDependencyLine(parts[0].trim());
                GoModuleInfo newModule = parseDependencyLine(parts[1].trim());
                if (oldModule != null && newModule != null) {
                    replaceDirectives.add(new GoReplaceDirective(oldModule, newModule));
                }
            }
        }
    }
    
    private void parseRetractLine(String line, Set<GoModuleInfo> retractedVersions) {
        // Retract can be a single version or a range [v1.0.0, v1.1.0]
        if (line.startsWith("[") && line.endsWith("]")) {
            // Handle version range
            String range = line.substring(1, line.length() - 1);
            String[] versions = range.split(",");
            for (String version : versions) {
                String trimmedVersion = version.trim();
                if (StringUtils.isNotBlank(trimmedVersion)) {
                    retractedVersions.add(new GoModuleInfo("", trimmedVersion));
                }
            }
        } else {
            // Single version
            GoModuleInfo moduleInfo = parseDependencyLine(line);
            if (moduleInfo != null) {
                retractedVersions.add(moduleInfo);
            }
        }
    }
    
    private GoModuleInfo parseDependencyLine(String line) {
        if (StringUtils.isBlank(line)) {
            return null;
        }
        
        Matcher matcher = DEPENDENCY_PATTERN.matcher(line);
        if (matcher.matches()) {
            String moduleName = matcher.group(1);
            String version = matcher.group(2);
            String comment = matcher.group(3);
            
            // Clean up version (remove +incompatible, %2Bincompatible suffixes)
            version = cleanVersion(version);
            version = KBComponentHelpers.getKbCompatibleVersion(version);

            // Check if it's an indirect dependency
            boolean isIndirect = comment != null && comment.contains("indirect");
            
            return new GoModuleInfo(moduleName, version, isIndirect);
        }
        
        // Try simpler parsing for cases where version might be missing
        String[] parts = line.split("\\s+");
        if (parts.length >= 1) {
            String moduleName = parts[0];
            String version = parts.length > 1 ? cleanVersion(parts[1]) : "";
            version = KBComponentHelpers.getKbCompatibleVersion(version);
            boolean isIndirect = line.contains("// indirect");
            return new GoModuleInfo(moduleName, version, isIndirect);
        }
        
        logger.warn("Failed to parse dependency line: {}", line);
        return null;
    }
    
    private String cleanVersion(String version) {
        if (version == null) {
            return "";
        }
        
        // Remove +incompatible and %2Bincompatible suffixes
        version = version.replace("+incompatible", "").replace("%2Bincompatible", "");
        
        return version;
    }
    
    private String extractModuleName(String line) {
        Matcher matcher = MODULE_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractGoVersion(String line) {
        Matcher matcher = GO_VERSION_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private String extractToolchainVersion(String line) {
        Matcher matcher = TOOLCHAIN_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private boolean isEmptyOrComment(String line) {
        return EMPTY_LINE_PATTERN.matcher(line).matches() || 
               COMMENT_PATTERN.matcher(line).matches();
    }
}
