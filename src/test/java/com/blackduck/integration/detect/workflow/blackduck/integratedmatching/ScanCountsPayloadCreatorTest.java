package com.blackduck.integration.detect.workflow.blackduck.integratedmatching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Queue;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.workflow.blackduck.codelocation.WaitableCodeLocationData;
import com.blackduck.integration.detect.workflow.blackduck.integratedmatching.model.ScanCountsPayload;

class ScanCountsPayloadCreatorTest {

    @Test
    void testMultSigScansPlusBinary() {
        WaitableCodeLocationData signatureScanWaitableCodeLocationData = Mockito.mock(WaitableCodeLocationData.class);
        Mockito.when(signatureScanWaitableCodeLocationData.getDetectTool()).thenReturn(DetectTool.SIGNATURE_SCAN);
        Set<String> successfulSigScanCodeLocationNames = new HashSet<>();
        successfulSigScanCodeLocationNames.add("sigScan1");
        successfulSigScanCodeLocationNames.add("sigScan2");
        Mockito.when(signatureScanWaitableCodeLocationData.getSuccessfulCodeLocationNames()).thenReturn(successfulSigScanCodeLocationNames);

        WaitableCodeLocationData binaryWaitableCodeLocationData = Mockito.mock(WaitableCodeLocationData.class);
        Mockito.when(binaryWaitableCodeLocationData.getDetectTool()).thenReturn(DetectTool.BINARY_SCAN);
        Set<String> successfulBinaryCodeLocationNames = new HashSet<>();
        successfulBinaryCodeLocationNames.add("binaryScan1");
        successfulBinaryCodeLocationNames.add("binaryScan2");
        successfulBinaryCodeLocationNames.add("binaryScan3");
        Mockito.when(binaryWaitableCodeLocationData.getSuccessfulCodeLocationNames()).thenReturn(successfulBinaryCodeLocationNames);

        Queue<WaitableCodeLocationData> waitableCodeLocationDataList = new ConcurrentLinkedQueue<>();
        Collections.addAll(waitableCodeLocationDataList, signatureScanWaitableCodeLocationData, binaryWaitableCodeLocationData);

        ScanCountsPayloadCreator creator = new ScanCountsPayloadCreator();

        Map<DetectTool, Integer> additionalCounts = new HashMap<>();
        additionalCounts.put(DetectTool.DETECTOR, 17);

        ScanCountsPayload payload = creator.create(waitableCodeLocationDataList, additionalCounts);

        assertEquals(17, payload.getScanCounts().getPackageManager());
        assertEquals(2, payload.getScanCounts().getSignature());
        assertEquals(3, payload.getScanCounts().getBinary());
    }

    @Test
    void testAllPkgMgrTypesPlusIgnored() {
        WaitableCodeLocationData bazelWaitableCodeLocationData = Mockito.mock(WaitableCodeLocationData.class);
        Mockito.when(bazelWaitableCodeLocationData.getDetectTool()).thenReturn(DetectTool.BAZEL);
        Set<String> successfulBazelCodeLocationNames = new HashSet<>();
        successfulBazelCodeLocationNames.add("bazelScan1");
        successfulBazelCodeLocationNames.add("bazelScan2");
        Mockito.when(bazelWaitableCodeLocationData.getSuccessfulCodeLocationNames()).thenReturn(successfulBazelCodeLocationNames);

        WaitableCodeLocationData dockerWaitableCodeLocationData = Mockito.mock(WaitableCodeLocationData.class);
        Mockito.when(dockerWaitableCodeLocationData.getDetectTool()).thenReturn(DetectTool.DOCKER);
        Set<String> successfulDockerCodeLocationNames = new HashSet<>();
        successfulDockerCodeLocationNames.add("dockerScan1");
        successfulDockerCodeLocationNames.add("dockerScan2");
        Mockito.when(dockerWaitableCodeLocationData.getSuccessfulCodeLocationNames()).thenReturn(successfulDockerCodeLocationNames);

        WaitableCodeLocationData detectorWaitableCodeLocationData = Mockito.mock(WaitableCodeLocationData.class);
        Mockito.when(detectorWaitableCodeLocationData.getDetectTool()).thenReturn(DetectTool.DETECTOR);
        Set<String> successfulDetectorCodeLocationNames = new HashSet<>();
        successfulDetectorCodeLocationNames.add("detectorScan1");
        successfulDetectorCodeLocationNames.add("detectorScan2");
        Mockito.when(detectorWaitableCodeLocationData.getSuccessfulCodeLocationNames()).thenReturn(successfulDetectorCodeLocationNames);


        WaitableCodeLocationData iacWaitableCodeLocationData = Mockito.mock(WaitableCodeLocationData.class);
        Mockito.when(iacWaitableCodeLocationData.getDetectTool()).thenReturn(DetectTool.IAC_SCAN);
        Set<String> successfulIacCodeLocationNames = new HashSet<>();
        successfulIacCodeLocationNames.add("iacScan1");
        successfulIacCodeLocationNames.add("iacScan2");
        Mockito.when(iacWaitableCodeLocationData.getSuccessfulCodeLocationNames()).thenReturn(successfulIacCodeLocationNames);

        Queue<WaitableCodeLocationData> waitableCodeLocationDataList = new ConcurrentLinkedQueue<>();
        Collections.addAll(waitableCodeLocationDataList,bazelWaitableCodeLocationData, detectorWaitableCodeLocationData, iacWaitableCodeLocationData, dockerWaitableCodeLocationData);

        ScanCountsPayloadCreator creator = new ScanCountsPayloadCreator();
        ScanCountsPayload payload = creator.create(waitableCodeLocationDataList, new HashMap<>());

        assertEquals(6, payload.getScanCounts().getPackageManager());
        assertEquals(0, payload.getScanCounts().getSignature());
        assertEquals(0, payload.getScanCounts().getBinary());
    }
}
