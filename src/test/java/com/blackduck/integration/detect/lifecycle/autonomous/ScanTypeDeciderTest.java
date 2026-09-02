package com.blackduck.integration.detect.lifecycle.autonomous;

import com.blackduck.integration.configuration.property.types.enumallnone.list.AllNoneEnumCollection;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.DetectPropertyConfiguration;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ScanTypeDeciderTest {
    private static final Path TEXT_ONLY_PROJECT = Paths.get("src/test/resources/lifecycle/autonomous/sample-project/only-text");
    private static final Path BINARY_PROJECT = Paths.get("src/test/resources/lifecycle/autonomous/sample-project/only-binary");
    private static final Path MIXED_PROJECT = Paths.get("src/test/resources/lifecycle/autonomous/sample-project/text-and-binary");

    private final DetectPropertyConfiguration detectConfiguration;
    private final AllNoneEnumCollection<DetectTool> includedTools, excludedTools;
    private final ScanTypeDecider scanTypeDecider = new ScanTypeDecider();
    private final List<String> fileInclusionPatterns;

    public ScanTypeDeciderTest() {
        detectConfiguration = Mockito.mock(DetectPropertyConfiguration.class);
        includedTools = Mockito.mock(AllNoneEnumCollection.class);
        excludedTools = Mockito.mock(AllNoneEnumCollection.class);
        fileInclusionPatterns = Mockito.mock(List.class);
    }
    
    @BeforeEach
    public void setup() {
        Mockito.doReturn(true).when(detectConfiguration).getValue(DetectProperties.DETECT_AUTONOMOUS_SCAN_ENABLED);
        Mockito.doReturn(includedTools).when(detectConfiguration).getValue(DetectProperties.DETECT_TOOLS);
        Mockito.doReturn(excludedTools).when(detectConfiguration).getValue(DetectProperties.DETECT_TOOLS_EXCLUDED);
        Mockito.doReturn(false).when(excludedTools).containsValue(DetectTool.DETECTOR);
        Mockito.doReturn(false).when(excludedTools).containsValue(DetectTool.SIGNATURE_SCAN);
        Mockito.doReturn(false).when(excludedTools).containsValue(DetectTool.BINARY_SCAN);
        Mockito.doReturn(false).when(includedTools).containsValue(DetectTool.DETECTOR);
        Mockito.doReturn(false).when(includedTools).containsValue(DetectTool.SIGNATURE_SCAN);
        Mockito.doReturn(false).when(includedTools).containsValue(DetectTool.BINARY_SCAN);
        
        Mockito.doReturn(true).when(excludedTools).isEmpty();
        Mockito.doReturn(true).when(includedTools).isEmpty();
        
        Mockito.doReturn(false).when(excludedTools).containsAll();
        Mockito.doReturn(false).when(includedTools).containsAll();
        
        Mockito.doReturn(fileInclusionPatterns).when(detectConfiguration).getValue(DetectProperties.DETECT_BINARY_SCAN_FILE_NAME_PATTERNS);
        Mockito.doReturn(true).when(fileInclusionPatterns).isEmpty();
    }
    
    @Test
    public void testTextOnlyProjectBinarySearch() {
        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decide(false, detectConfiguration, TEXT_ONLY_PROJECT);
        Assertions.assertFalse(scanTypeMap.containsKey(DetectTool.BINARY_SCAN));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR));
        Assertions.assertEquals(Collections.singleton(TEXT_ONLY_PROJECT.toAbsolutePath().toString()), scanTypeMap.get(DetectTool.DETECTOR));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
        Assertions.assertEquals(Collections.singleton(TEXT_ONLY_PROJECT.toAbsolutePath().toString()), scanTypeMap.get(DetectTool.SIGNATURE_SCAN));
    }
    
    @Test
    public void testBinaryProjectBinarySearch() {
        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decide(false, detectConfiguration, BINARY_PROJECT);
        Assertions.assertEquals(
            Collections.singleton(BINARY_PROJECT.resolve("7z2406-x64.exe").toAbsolutePath().toString()),
            scanTypeMap.get(DetectTool.BINARY_SCAN)
        );
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
    }
    
    @Test
    public void testMixProjectBinarySearch() {
        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decide(false, detectConfiguration, MIXED_PROJECT);
        Assertions.assertEquals(
            Collections.singleton(MIXED_PROJECT.resolve("deploy/7z2406-x64.exe").toAbsolutePath().toString()),
            scanTypeMap.get(DetectTool.BINARY_SCAN)
        );
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
    }

    @Test
    public void testDiscoveryUsesSharedLogicWithoutAutonomousMode() {
        Map<DetectTool, Set<String>> autonomousResult = scanTypeDecider.decide(false, detectConfiguration, BINARY_PROJECT);
        Mockito.doReturn(false).when(detectConfiguration).getValue(DetectProperties.DETECT_AUTONOMOUS_SCAN_ENABLED);

        Assertions.assertTrue(scanTypeDecider.decide(false, detectConfiguration, BINARY_PROJECT).isEmpty());
        Assertions.assertEquals(
            autonomousResult,
            scanTypeDecider.decideForDiscovery(false, detectConfiguration, BINARY_PROJECT)
        );
    }

    @Test
    public void testDiscoverySkipsSourceScanTypesForImageOrTar() {
        Assertions.assertTrue(scanTypeDecider.decideForDiscovery(true, detectConfiguration, BINARY_PROJECT).isEmpty());
    }

    @Test
    public void testDiscoveryHonorsExplicitlyIncludedTools() {
        Mockito.doReturn(false).when(includedTools).isEmpty();
        Mockito.doReturn(true).when(includedTools).containsValue(DetectTool.DETECTOR);

        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decideForDiscovery(false, detectConfiguration, BINARY_PROJECT);

        Assertions.assertEquals(Collections.singleton(DetectTool.DETECTOR), scanTypeMap.keySet());
    }

    @Test
    public void testDiscoveryExclusionOverridesInclusion() {
        Mockito.doReturn(false).when(includedTools).isEmpty();
        Mockito.doReturn(true).when(includedTools).containsValue(DetectTool.SIGNATURE_SCAN);
        Mockito.doReturn(true).when(excludedTools).containsValue(DetectTool.SIGNATURE_SCAN);

        Assertions.assertTrue(scanTypeDecider.decideForDiscovery(false, detectConfiguration, TEXT_ONLY_PROJECT).isEmpty());
    }

    @Test
    public void testDiscoveryHonorsBinaryExclusion() {
        Mockito.doReturn(true).when(excludedTools).containsValue(DetectTool.BINARY_SCAN);

        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decideForDiscovery(false, detectConfiguration, BINARY_PROJECT);

        Assertions.assertFalse(scanTypeMap.containsKey(DetectTool.BINARY_SCAN));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
    }

    @Test
    public void testConfiguredBinaryPatternsSkipAutomaticBinaryDetection() {
        Mockito.doReturn(false).when(fileInclusionPatterns).isEmpty();

        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decideForDiscovery(false, detectConfiguration, BINARY_PROJECT);

        Assertions.assertFalse(scanTypeMap.containsKey(DetectTool.BINARY_SCAN));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
    }
}
