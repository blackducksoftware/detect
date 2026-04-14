package com.blackduck.integration.detect.workflow.blackduck.codelocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;

class CodeLocationAccumulatorTest {

    private CodeLocationAccumulator accumulator;

    @BeforeEach
    void setUp() {
        accumulator = new CodeLocationAccumulator();
    }

    @Test
    void testAddNonWaitableCodeLocationString() {
        String name = "location1";
        
        accumulator.addNonWaitableCodeLocation(name);

        assertEquals(1, accumulator.getNonWaitableCodeLocations().size());
        assertTrue(accumulator.getNonWaitableCodeLocations().contains(name));
    }

    @Test
    void testAddMultipleNonWaitableCodeLocations() {
        Set<String> names1 = new HashSet<>(Arrays.asList("location1", "location2"));
        Set<String> names2 = new HashSet<>(Arrays.asList("location3", "location4"));
        String name3 = "location5";
        
        accumulator.addNonWaitableCodeLocation(names1);
        accumulator.addNonWaitableCodeLocation(names2);
        accumulator.addNonWaitableCodeLocation(name3);

        assertEquals(5, accumulator.getNonWaitableCodeLocations().size());
        assertTrue(accumulator.getNonWaitableCodeLocations().containsAll(names1));
        assertTrue(accumulator.getNonWaitableCodeLocations().containsAll(names2));
        assertTrue(accumulator.getNonWaitableCodeLocations().contains(name3));
    }

    @Test
    void testIncrementAdditionalCounts() {
        accumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 5);

        assertEquals(1, accumulator.getAdditionalCountsByTool().size());
        assertEquals(5, accumulator.getAdditionalCountsByTool().get(DetectTool.DETECTOR));
    }

    @Test
    void testIncrementAdditionalCountsMultipleTimes() {
        accumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 3);
        accumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 7);

        assertEquals(1, accumulator.getAdditionalCountsByTool().size());
        assertEquals(10, accumulator.getAdditionalCountsByTool().get(DetectTool.DETECTOR));
    }

    @Test
    void testIncrementAdditionalCountsMultipleTools() {
        accumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 3);
        accumulator.incrementAdditionalCounts(DetectTool.SIGNATURE_SCAN, 5);
        accumulator.incrementAdditionalCounts(DetectTool.BINARY_SCAN, 2);

        assertEquals(3, accumulator.getAdditionalCountsByTool().size());
        assertEquals(3, accumulator.getAdditionalCountsByTool().get(DetectTool.DETECTOR));
        assertEquals(5, accumulator.getAdditionalCountsByTool().get(DetectTool.SIGNATURE_SCAN));
        assertEquals(2, accumulator.getAdditionalCountsByTool().get(DetectTool.BINARY_SCAN));
    }

    @Test
    void testIncrementAdditionalCountsZero() {
        accumulator.incrementAdditionalCounts(DetectTool.DETECTOR, 0);

        assertEquals(1, accumulator.getAdditionalCountsByTool().size());
        assertEquals(0, accumulator.getAdditionalCountsByTool().get(DetectTool.DETECTOR));
    }

    @Test
    void testEmptyAccumulator() {
        assertTrue(accumulator.getWaitableCodeLocations().isEmpty());
        assertTrue(accumulator.getNonWaitableCodeLocations().isEmpty());
        assertTrue(accumulator.getAdditionalCountsByTool().isEmpty());
    }
}
