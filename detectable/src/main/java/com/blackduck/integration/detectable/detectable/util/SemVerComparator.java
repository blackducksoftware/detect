package com.blackduck.integration.detectable.detectable.util;

import java.util.Comparator;

public class SemVerComparator implements Comparator<String> {
    @Override
    public int compare(String v1, String v2) {
        // Split each version string into parts
        String[] v1Parts = v1.split("\\.");
        String[] v2Parts = v2.split("\\.");

        // Determine the maximum length to iterate over
        int maxLength = Math.max(v1Parts.length, v2Parts.length);

        // Compare each part until we know which string is smallest
        for (int i = 0; i < maxLength; i++) {
            int part1 = parseVersionPart(v1Parts, i);
            int part2 = parseVersionPart(v2Parts, i);

            int comparison = Integer.compare(part1, part2);

            // If the parts are not equal, return the comparison result
            if (comparison != 0) {
                return comparison;
            }
        }

        // If all parts are equal, return 0
        return 0;
    }

    private int parseVersionPart(String[] parts, int index) {
        try {
            return (index < parts.length) ? Integer.parseInt(parts[index]) : 0;
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE; // Treating invalid parts as larger
        }
    }
}