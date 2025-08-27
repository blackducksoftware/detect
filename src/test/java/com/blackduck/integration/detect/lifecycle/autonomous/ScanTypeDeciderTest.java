package com.blackduck.integration.detect.lifecycle.autonomous;

import com.blackduck.integration.configuration.property.types.enumallnone.list.AllNoneEnumCollection;
import com.blackduck.integration.detect.configuration.DetectProperties;
import com.blackduck.integration.detect.configuration.DetectPropertyConfiguration;
import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.autonomous.ScanTypeDecider;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ScanTypeDeciderTest {
    
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
        Path dir = Paths.get("src/test/resources/lifecycle/autonomous/sample-project/only-text");
        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decide(false, detectConfiguration, dir);
        Assertions.assertFalse(scanTypeMap.containsKey(DetectTool.BINARY_SCAN));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR));
        Assertions.assertTrue(scanTypeMap.get(DetectTool.DETECTOR).size()==1);
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
        Assertions.assertTrue(scanTypeMap.get(DetectTool.SIGNATURE_SCAN).size()==1);
    }
    
    @Test
    public void testBinaryProjectBinarySearch() {
        Path dir = Paths.get("src/test/resources/lifecycle/autonomous/sample-project/only-binary");
        Map<DetectTool, Set<String>> scanTypeMap = scanTypeDecider.decide(false, detectConfiguration, dir);
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.BINARY_SCAN));
        Assertions.assertFalse(scanTypeMap.get(DetectTool.BINARY_SCAN).isEmpty());
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR));
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
    }
    
    @Test
    public void testMixProjectBinarySearch() {
        Path dir = Paths.get("src/test/resources/lifecycle/autonomous/sample-project/text-and-binary");
        Map<DetectTool, Set<String>>  scanTypeMap = scanTypeDecider.decide(false, detectConfiguration, dir);
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.BINARY_SCAN));
        Assertions.assertTrue(!scanTypeMap.get(DetectTool.BINARY_SCAN).isEmpty());
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.DETECTOR)); 
        Assertions.assertTrue(!scanTypeMap.get(DetectTool.DETECTOR).isEmpty());
        Assertions.assertTrue(scanTypeMap.containsKey(DetectTool.SIGNATURE_SCAN));
        Assertions.assertTrue(!scanTypeMap.get(DetectTool.SIGNATURE_SCAN).isEmpty());
    }
}