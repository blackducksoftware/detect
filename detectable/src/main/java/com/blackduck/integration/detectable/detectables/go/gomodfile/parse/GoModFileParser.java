package com.blackduck.integration.detectable.detectables.go.gomodfile.parse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModFileContent;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoModuleInfo;
import com.blackduck.integration.detectable.detectables.go.gomodfile.parse.model.GoReplaceDirective;

import java.util.*;
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

    /**
     * Parses a go.mod file content and returns structured information.
     * 
     * @param lines List of lines from the go.mod file
     * @return GoModFileContent containing all parsed information
     */
    public GoModFileContent parseGoModFile(List<String> lines) {
        String moduleName = null;
        String goVersion = null;
        String toolchainVersion = null;
        List<GoModuleInfo> directDependencies = new ArrayList<>();
        List<GoModuleInfo> indirectDependencies = new ArrayList<>();
        Set<GoModuleInfo> excludedModules = new HashSet<>();
        List<GoReplaceDirective> replaceDirectives = new ArrayList<>();
        Set<GoModuleInfo> retractedVersions = new HashSet<>();
        
        ParseState currentState = ParseState.NORMAL;
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines and comments
            if (isEmptyOrComment(line)) {
                continue;
            }
            
            // Check for end of blocks
            if (line.equals(")")) {
                currentState = ParseState.NORMAL;
                continue;
            }
            
            // Parse based on current state
            switch (currentState) {
                case NORMAL:
                    currentState = parseNormalLine(line, moduleName, goVersion, toolchainVersion, 
                                                 directDependencies, indirectDependencies, 
                                                 excludedModules, replaceDirectives, retractedVersions);
                    // Extract module, go version, toolchain if found
                    if (moduleName == null) {
                        moduleName = extractModuleName(line);
                    }
                    if (goVersion == null) {
                        goVersion = extractGoVersion(line);
                    }
                    if (toolchainVersion == null) {
                        toolchainVersion = extractToolchainVersion(line);
                    }
                    break;
                case REQUIRE_BLOCK:
                    parseRequireLine(line, directDependencies, indirectDependencies);
                    break;
                case EXCLUDE_BLOCK:
                    parseExcludeLine(line, excludedModules);
                    break;
                case REPLACE_BLOCK:
                    parseReplaceLine(line, replaceDirectives);
                    break;
                case RETRACT_BLOCK:
                    parseRetractLine(line, retractedVersions);
                    break;
            }
        }
        
        return new GoModFileContent(
            moduleName,
            goVersion,
            toolchainVersion,
            directDependencies,
            indirectDependencies,
            excludedModules,
            replaceDirectives,
            retractedVersions
        );
    }
    
    private ParseState parseNormalLine(String line, String moduleName, String goVersion, String toolchainVersion,
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
            
            // Check if it's an indirect dependency
            boolean isIndirect = comment != null && comment.contains("indirect");
            
            return new GoModuleInfo(moduleName, version, isIndirect);
        }
        
        // Try simpler parsing for cases where version might be missing
        String[] parts = line.split("\\s+");
        if (parts.length >= 1) {
            String moduleName = parts[0];
            String version = parts.length > 1 ? cleanVersion(parts[1]) : "";
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
