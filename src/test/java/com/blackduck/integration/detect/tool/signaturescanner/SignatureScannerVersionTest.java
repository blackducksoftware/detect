package com.blackduck.integration.detect.tool.signaturescanner;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SignatureScannerVersionTest {
    @Test
    void testIsAtLeastSameFormatOld() {
        SignatureScannerVersion version1 = new SignatureScannerVersion(2000, 1, 0);
        SignatureScannerVersion version2 = new SignatureScannerVersion(2000, 2, 0);

        assertFalse(version1.isAtLeast(version2));
        assertTrue(version2.isAtLeast(version1));
    }

    @Test
    void testIsAtLeastSameFormatNew() {
        SignatureScannerVersion version1 = new SignatureScannerVersion(1, 0, 1);
        SignatureScannerVersion version2 = new SignatureScannerVersion(1, 0, 2);

        assertFalse(version1.isAtLeast(version2));
        assertTrue(version2.isAtLeast(version1));
    }

    @Test
    void testIsAtLeastDifferentFormatOldToNew() {
        SignatureScannerVersion oldFormatVersion = new SignatureScannerVersion(2000, 1, 0);
        SignatureScannerVersion newFormatVersion = new SignatureScannerVersion(1, 0, 0);

        assertTrue(newFormatVersion.isAtLeast(oldFormatVersion));
        assertFalse(oldFormatVersion.isAtLeast(newFormatVersion));
    }

    @Test
    void testIsAtLeastDifferentFormatNewToOld() {
        SignatureScannerVersion oldFormatVersion = new SignatureScannerVersion(1, 0, 0);
        SignatureScannerVersion newFormatVersion = new SignatureScannerVersion(1999, 12, 31);

        assertTrue(newFormatVersion.isAtLeast(oldFormatVersion));
        assertFalse(oldFormatVersion.isAtLeast(newFormatVersion));
    }

    @Test
    void testIsAtLeastBoundaryCases() {
        SignatureScannerVersion version1 = new SignatureScannerVersion(1, 0, 0);
        SignatureScannerVersion version2 = new SignatureScannerVersion(1, 0, 1);
        SignatureScannerVersion version3 = new SignatureScannerVersion(1, 1, 0);
        SignatureScannerVersion version4 = new SignatureScannerVersion(2, 0, 0);

        assertFalse(version1.isAtLeast(version2));
        assertTrue(version2.isAtLeast(version1));

        assertFalse(version1.isAtLeast(version3));
        assertTrue(version3.isAtLeast(version1));

        assertFalse(version1.isAtLeast(version4));
        assertTrue(version4.isAtLeast(version1));
    }
}
