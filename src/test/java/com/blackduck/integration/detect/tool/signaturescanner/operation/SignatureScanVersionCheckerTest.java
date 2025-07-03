package com.blackduck.integration.detect.tool.signaturescanner.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.blackduck.integration.detect.tool.signaturescanner.SignatureScannerVersion;

public class SignatureScanVersionCheckerTest {

    @Mock
    private Logger mockLogger;

    @Mock
    private File mockToolsDir;

    @Mock
    private File mockInstallDir;

    @Mock
    private File mockVersionFile;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testParseSemVer_validVersion() throws Exception {
        Method method = SignatureScanVersionChecker.class.getDeclaredMethod("parseSemVer", String.class);
        method.setAccessible(true);
        SignatureScannerVersion version = (SignatureScannerVersion) method.invoke(null, "7.0.1");

        assertEquals(7, version.getMajor());
        assertEquals(0, version.getMinor());
        assertEquals(1, version.getPatch());
    }

    @Test
    public void testParseSemVer_invalidVersion() throws Exception {
        Method method = SignatureScanVersionChecker.class.getDeclaredMethod("parseSemVer", String.class);
        method.setAccessible(true);

        Exception exception = assertThrows(Exception.class, () -> {
            method.invoke(null, "invalid.version");
        });

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }
}
