package com.blackduck.integration.detect.battery.docker.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class DockerAssertionsTest {
    
    @ParameterizedTest
    @CsvSource({
        "'00h 02m 30s', 151, true",
        "'00h 02m 30s', 150, false",
        "'23h 59m 59s', 86400, true",
        "'23h 59m 59s', 86399, false"
    })
    public void testDurationLessThan(String durationString, int thresholdSeconds, boolean shouldPass) {
        // Create mock Docker result with duration log
        DockerDetectResult mockResult = Mockito.mock(DockerDetectResult.class);
        String logWithDuration = "2025-09-03 19:20:26 MDT INFO  [main] --- Some log output here\n" +
                                 "2025-09-03 19:20:26 MDT INFO  [main] --- Detect duration: " + durationString + "\n" +
                                 "2025-09-03 19:20:26 MDT INFO  [main] --- More log output";

        when(mockResult.getDetectLogs()).thenReturn(logWithDuration);
        
        // Create mock test directories
        DockerTestDirectories mockDirectories = Mockito.mock(DockerTestDirectories.class);
        
        // Create DockerAssertions instance
        DockerAssertions assertions = new DockerAssertions(mockDirectories, mockResult);
        
        if (shouldPass) {
            // Should not throw any exception
            assertDoesNotThrow(() -> assertions.durationLessThan(thresholdSeconds));
        } else {
            // Should throw a wrapped AssertionError
            Throwable throwable = assertThrows(AssertionError.class, () -> {
                assertions.durationLessThan(thresholdSeconds);
            }).getCause();
            
            // Verify error message contains expected duration information
            assertTrue(throwable.getMessage().contains("Expected duration"));
            assertTrue(throwable.getMessage().contains("seconds to be less than"));
        }
    }
    
    @Test
    public void testDurationLessThanWithMissingDurationLog() {
        // Create mock Docker result without duration log
        DockerDetectResult mockResult = Mockito.mock(DockerDetectResult.class);
        String logWithoutDuration = "Some log output here\nNo duration logged\nMore log output";
        when(mockResult.getDetectLogs()).thenReturn(logWithoutDuration);
        
        // Create mock test directories
        DockerTestDirectories mockDirectories = Mockito.mock(DockerTestDirectories.class);
        
        // Create DockerAssertions instance
        DockerAssertions assertions = new DockerAssertions(mockDirectories, mockResult);
        
        // Should fail because no duration pattern found
        Throwable throwable = assertThrows(AssertionError.class, () -> {
            assertions.durationLessThan(100);
        }).getCause();
        
        assertTrue(throwable.getMessage().contains("Expected Detect to log duration"));
    }
}