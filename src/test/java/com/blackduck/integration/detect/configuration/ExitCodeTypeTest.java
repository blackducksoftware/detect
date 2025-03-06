package com.blackduck.integration.detect.configuration;

import com.blackduck.integration.detect.configuration.enumeration.ExitCodeType;
import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class ExitCodeTypeTest {

    @Test
    void testUniquePriorities() {
        Set<Double> priorities = new HashSet<>();
        for (ExitCodeType exitCode : ExitCodeType.values()) {
            assertTrue(priorities.add(exitCode.getPriority()),
                "Duplicate priority found: " + exitCode.getPriority());
        }
    }

    @Test
    void testUniqueExitCodes() {
        Set<Integer> exitCodes = new HashSet<>();
        for (ExitCodeType exitCode : ExitCodeType.values()) {
            assertTrue(exitCodes.add(exitCode.getExitCode()),
                "Duplicate exit code found: " + exitCode.getExitCode());
        }
    }
}
