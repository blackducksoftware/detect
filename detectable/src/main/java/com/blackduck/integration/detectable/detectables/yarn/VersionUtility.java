package com.blackduck.integration.detectable.detectables.yarn;

import com.blackduck.integration.bdio.graph.builder.LazyIdSource;
import com.blackduck.integration.util.NameVersion;
import java.util.*;
import java.util.function.Function;
import java.util.regex.*;

public class VersionUtility {
    
    private static final String ESCAPED_BACKSLASH = "\",\"";
    
    Version buildVersion(String version) {
        String cleanVersion = version.trim();
        StringBuilder sb = new StringBuilder(cleanVersion.length());
        for (int i=0; i< cleanVersion.length(); i++) {
            if (cleanVersion.charAt(i) == ' ') {
                sb.append('.');
            } else {
                sb.append(cleanVersion.charAt(i));
            }
        }
        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)+)(?!.*\\d+\\.\\d+)").matcher(sb.toString());
        List<String> parts = new ArrayList<>();
        if (matcher.find()) {
            Matcher versionMatcher = Pattern.compile("(\\d+)").matcher(matcher.group(1));
            while (parts.size() < 3 && versionMatcher.find()) {
                parts.add(versionMatcher.group());
            }
        }
        return new Version(parts.toArray(new String[0]));
    }
    
    Optional<String> resolveYarnVersion(List<Version> versionList, String version) {
        versionList.sort(Comparator.reverseOrder());
        if (version.contains(" || ")) {
            List<String> possibleVersions = Arrays.asList(version.split(" "));
            return parseOrVersions(versionList, possibleVersions);
        } else if (version.startsWith("^")) {
            return minorOrPatchUpgrade(versionList, version);
        } else if (version.startsWith("~")) {
            return onlyPatchUpgrade(versionList, version);
        } else if (version.startsWith("*")) {
            return mustUpgrade(versionList, version);
        } else if (version.startsWith(">") && !version.startsWith(">=")) {
            return mustUpgradeGreater(versionList, version);
        } else if (version.startsWith(">=")) {
            return mustUpgradeGreaterOrEqual(versionList, version);
        } else if (version.startsWith("<=")) {
            return mustUpgradeLesser(versionList, version);
        } else if (version.startsWith("<") && !version.startsWith("<=")) {
            return mustUpgradeLesserOrEqual(versionList, version);
        } else {
            return mustUpgradeEqual(versionList, version);
        }
    }

    private Optional<String> parseOrVersions(List<Version> versionList, List<String> possibleVersions) {
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        for (Version left : versionList) {
            for (String version : possibleVersions) {
                if (!version.equals("||")) {
                    version = getVersionSubstring(version);
                    Version right = buildVersion(version);
                    if(left.nearestVersion(right) < currentNearestVersion) {
                        currentNearestVersion = left.nearestVersion(right);
                        nearestVersion = left.toString();
                    }
                }
            }
        }

        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> minorOrPatchUpgrade(List<Version> versionList, String version) {
        Version right = buildVersion(version.substring(1).trim());
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        for (Version left : versionList) {
            if (left.major == right.major) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> onlyPatchUpgrade(List<Version> versionList, String version) {
        Version right = buildVersion(version.substring(1).trim());
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        for (Version left : versionList) {
            if (left.major == right.major && left.minor == right.minor) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> mustUpgrade(List<Version> versionList, String version) {
        Version right = buildVersion(version.substring(1).trim());
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        for (Version left : versionList) {
            if (left.compareTo(right) == 1) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> mustUpgradeGreater(List<Version> versionList, String version) {
        Version right;
        Optional<Version> ceiling;
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        if (version.contains(" <=")) {
            right = buildVersion(version.substring(1, version.indexOf(" <=")).trim());
            ceiling = Optional.of(buildVersion(version.substring(version.indexOf(" <=") + " <=".length()).trim()));
        } else {
            right = buildVersion(version.substring(1).trim());
            ceiling = Optional.empty();
        }
        for (Version left : versionList) {
            if (ceiling.isPresent() && left.compareTo(ceiling.get()) < 1 && (left.compareTo(right) == 1)) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> mustUpgradeGreaterOrEqual(List<Version> versionList, String version) {
        Version right;
        Optional<Version> ceiling;
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        if (version.contains(" <")) {
            right = buildVersion(version.substring(2, version.indexOf(" <")).trim());
            ceiling = Optional.of(buildVersion(version.substring(version.indexOf(" <") + " <".length()).trim()));
        } else {
            right = buildVersion(version.substring(2).trim());
            ceiling = Optional.empty();
        }

        for (Version left : versionList) {
            if (ceiling.isPresent() && left.compareTo(ceiling.get()) == -1 && (left.compareTo(right) >= 0)) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> mustUpgradeLesser(List<Version> versionList, String version) {
        Version right = buildVersion(version.substring(2).trim());
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        for (Version left : versionList) {
            if (left.compareTo(right) <= 0) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> mustUpgradeLesserOrEqual(List<Version> versionList, String version) {
        Version right = buildVersion(version.substring(1).trim());
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        for (Version left : versionList) {
            if (left.compareTo(right) == -1) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }
    
    private Optional<String> mustUpgradeEqual(List<Version> versionList, String version) {
        Version right = buildVersion(version.substring(1).trim());
        String nearestVersion = null;
        int currentNearestVersion = Integer.MAX_VALUE;
        for (Version left : versionList) {
            if (left.compareTo(right) == 0) {
                return Optional.of(left.toString());
            }
            if(left.nearestVersion(right) < currentNearestVersion) {
                currentNearestVersion = left.nearestVersion(right);
                nearestVersion = left.toString();
            }
        }
        return Optional.ofNullable(nearestVersion);
    }

    private String getVersionSubstring(String version) {
        if (version.startsWith("<") || version.startsWith(">") || version.startsWith("^") || version.startsWith("~")) {
            if (version.startsWith("<=") || version.startsWith(">=")) {
                return version.substring(2).trim();
            } else {
               return version.substring(1).trim();
            }
        }
        return version;
    }
    
    public Optional<NameVersion> getNameVersion(String dependencyIdString) {
        Optional<NameVersion> result = tryParsePattern(dependencyIdString,
                LazyIdSource.STRING + ESCAPED_BACKSLASH,
                "@npm:",
                true, // use lastIndexOf for mid
                0
        );
        if (result.isPresent()) return result;

        result = tryParsePattern(dependencyIdString,
                LazyIdSource.NAME_VERSION + ESCAPED_BACKSLASH,
                ESCAPED_BACKSLASH + "npm:",
                false, // use indexOf for mid
                1
        );
        if (result.isPresent()) return result;

        return tryParsePattern(dependencyIdString,
                LazyIdSource.NAME_VERSION + ESCAPED_BACKSLASH,
                ESCAPED_BACKSLASH,
                true, // use lastIndexOf for mid
                1
        );
    }


    private Optional<NameVersion> tryParsePattern(String dependencyIdString,
                                                  String startPrefix,
                                                  String midDelimiter,
                                                  boolean useLastIndexOf,
                                                  int addValue) {
        int start = dependencyIdString.indexOf(startPrefix);
        if (start == -1) return Optional.empty();

        int mid = useLastIndexOf ?
                dependencyIdString.lastIndexOf(midDelimiter) :
                dependencyIdString.indexOf(midDelimiter, start);
        if (mid == -1) return Optional.empty();

        int end = dependencyIdString.indexOf("\"]}", mid);
        if (end == -1) return Optional.empty();

        String name = dependencyIdString.substring(start + startPrefix.length(), mid);
        String version = dependencyIdString.substring(mid + midDelimiter.length(), end);

        int atIndex = version.indexOf('@');
        if (atIndex > -1) {
            version = version.substring(atIndex + addValue);
        }

        return Optional.of(new NameVersion(name, version));
    }
}
