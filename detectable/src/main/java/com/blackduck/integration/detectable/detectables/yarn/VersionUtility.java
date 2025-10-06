package com.blackduck.integration.detectable.detectables.yarn;

import com.blackduck.integration.bdio.graph.builder.LazyIdSource;
import com.blackduck.integration.util.NameVersion;
import java.util.*;
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
                    if (version.startsWith("<=") || version.startsWith(">=") || version.startsWith("<") || version.startsWith(">") || version.startsWith("^") || version.startsWith("~")) {
                        version = version.substring(1).trim();
                    }
                    Version right = buildVersion(version);
                    if(left.compareTo(right) == 0) {
                        return Optional.of(left.toString());
                    }
                    if(left.nearestVersion(right) < currentNearestVersion) {
                        currentNearestVersion = left.nearestVersion(right);
                        nearestVersion = left.toString();
                    }
                }
            }
        }
        if(nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
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
        if (nearestVersion != null) {
            return Optional.of(nearestVersion);
        }
        return Optional.empty();
    }
    
    Optional<NameVersion> getNameVersion(String dependencyIdString) {
        int start, mid, end;
        String name;
        if ((start = dependencyIdString.indexOf(LazyIdSource.STRING + ESCAPED_BACKSLASH)) > -1
                &&  (mid = dependencyIdString.lastIndexOf("@npm:")) > -1
                && (end = dependencyIdString.indexOf("\"]}", mid)) > -1) {
            name = dependencyIdString.substring(start + (LazyIdSource.STRING + ESCAPED_BACKSLASH).length(), mid);
            String version = dependencyIdString.substring(mid + "@npm:".length(), end);
            if ((start = version.indexOf("@")) > -1) {
                version = version.substring(start);
            }
            return Optional.of(new NameVersion(name, version));
        } else if ((start = dependencyIdString.indexOf(LazyIdSource.NAME_VERSION + ESCAPED_BACKSLASH)) > -1
                && (mid = dependencyIdString.indexOf(ESCAPED_BACKSLASH + "npm:", start)) > -1
                && (end = dependencyIdString.indexOf("\"]}", mid)) > -1) {
            name = dependencyIdString.substring(start + (LazyIdSource.NAME_VERSION + ESCAPED_BACKSLASH).length(), mid);
            String version = dependencyIdString.substring(mid + (ESCAPED_BACKSLASH + "npm:").length(), end);
            if ((start = version.indexOf("@")) > -1) {
                version = version.substring(start + 1);
            }
            return Optional.of(new NameVersion(name, version));
        } else if ((start = dependencyIdString.indexOf(LazyIdSource.NAME_VERSION + ESCAPED_BACKSLASH)) > -1
                && (mid = dependencyIdString.lastIndexOf(ESCAPED_BACKSLASH)) > -1
                && (end = dependencyIdString.indexOf("\"]}", mid)) > -1) {
            name = dependencyIdString.substring(start + (LazyIdSource.NAME_VERSION + ESCAPED_BACKSLASH).length(), mid);
            String version = dependencyIdString.substring(mid + (ESCAPED_BACKSLASH).length(), end);
            if ((start = version.indexOf("@")) > -1) {
                version = version.substring(start + 1);
            }
            return Optional.of(new NameVersion(name, version));
        }
        return Optional.empty();
    }
}
