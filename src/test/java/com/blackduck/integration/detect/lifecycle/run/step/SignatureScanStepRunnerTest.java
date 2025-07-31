package com.blackduck.integration.detect.lifecycle.run.step;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class SignatureScanStepRunnerTest {

    @Mock
    private OperationRunner operationRunner;

    @Mock
    private ScanBatchRunner scanBatchRunner;

    @Mock
    private BlackDuckRunData blackDuckRunData;

    private SignatureScanStepRunner signatureScanStepRunner;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, blackDuckRunData);
    }

    @Test
    public void testRunSignatureScannerOnlineRetriesOnHttpHostConnectException() throws Exception {
        String detectRunUuid = "test-uuid";
        NameVersion projectNameVersion = new NameVersion("TestProject", "1.0.0");
        DockerTargetData dockerTargetData = mock(DockerTargetData.class);
        Set<String> scanIdsToWaitFor = new HashSet<>();
        Gson gson = new Gson();

        List<SignatureScanPath> scanPaths = Collections.singletonList(mock(SignatureScanPath.class));
        ScanBatch scanBatch = mock(ScanBatch.class);

        when(operationRunner.createScanPaths(projectNameVersion, dockerTargetData)).thenReturn(scanPaths);
        when(operationRunner.createScanBatchOnline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData,
                blackDuckRunData, false)).thenReturn(scanBatch);
        when(operationRunner.createScanBatchOnline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData,
                blackDuckRunData, true)).thenReturn(scanBatch);
        when(blackDuckRunData.shouldWaitAtScanLevel()).thenReturn(true);

        SignatureScanStepRunner spyRunner = spy(signatureScanStepRunner);

        ConnectException connectException = new ConnectException("Connection failed");
        HttpHost httpHost = new HttpHost("scass.blackduck.com", 443, "https");
        HttpHostConnectException httpException = new HttpHostConnectException(httpHost, connectException);

        doThrow(httpException).doReturn(Collections.emptyList()).when(spyRunner).executeScan(any(), any(), any(), any(),
                any(), anyBoolean(), anyBoolean());

        spyRunner.runSignatureScannerOnline(detectRunUuid, projectNameVersion, dockerTargetData, scanIdsToWaitFor,
                gson);

        verify(spyRunner, times(2)).executeScan(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
    }
}
