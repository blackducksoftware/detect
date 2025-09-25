package com.blackduck.integration.detect.lifecycle.run.step;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.net.ConnectException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

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

    private static final String TEST_UUID = "test-uuid";
    private static final String PROJECT_NAME = "TestProject";
    private static final String PROJECT_VERSION = "1.0.0";

    @Mock
    private OperationRunner operationRunner;

    @Mock
    private ScanBatchRunner scanBatchRunner;

    @Mock
    private BlackDuckRunData blackDuckRunData;

    private SignatureScanStepRunner signatureScanStepRunner;
    private NameVersion projectNameVersion;
    private Gson gson;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, blackDuckRunData);
        projectNameVersion = new NameVersion(PROJECT_NAME, PROJECT_VERSION);
        gson = new Gson();
    }

    @Test
    public void testRunSignatureScannerOnlineRetriesOnHttpHostConnectException() throws Exception {
        DockerTargetData dockerTargetData = mock(DockerTargetData.class);
        Queue<String> scanIdsToWaitFor = new ConcurrentLinkedQueue<>();
        List<SignatureScanPath> scanPaths = Collections.singletonList(mock(SignatureScanPath.class));
        ScanBatch scanBatch = mock(ScanBatch.class);

        when(operationRunner.createScanPaths(projectNameVersion, dockerTargetData)).thenReturn(scanPaths);
        when(operationRunner.createScanBatchOnline(TEST_UUID, scanPaths, projectNameVersion, dockerTargetData,
                blackDuckRunData, false)).thenReturn(scanBatch);
        when(operationRunner.createScanBatchOnline(TEST_UUID, scanPaths, projectNameVersion, dockerTargetData,
                blackDuckRunData, true)).thenReturn(scanBatch);
        when(blackDuckRunData.shouldWaitAtScanLevel()).thenReturn(true);

        SignatureScanStepRunner spyRunner = spy(signatureScanStepRunner);

        HttpHostConnectException httpException = createHttpHostConnectException();

        doThrow(httpException).doReturn(Collections.emptyList()).when(spyRunner).executeScan(any(), any(), any(), any(),
                any(), anyBoolean(), anyBoolean(), any());

        spyRunner.runSignatureScannerOnline(TEST_UUID, projectNameVersion, dockerTargetData, scanIdsToWaitFor, gson, any());

        verify(spyRunner, times(2)).executeScan(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    public void testRunSignatureScannerOnlineExecutesOnceWithoutException() throws Exception {
        DockerTargetData dockerTargetData = mock(DockerTargetData.class);
        Queue<String> scanIdsToWaitFor = new ConcurrentLinkedQueue<>();
        List<SignatureScanPath> scanPaths = Collections.singletonList(mock(SignatureScanPath.class));
        ScanBatch scanBatch = mock(ScanBatch.class);

        when(operationRunner.createScanPaths(projectNameVersion, dockerTargetData)).thenReturn(scanPaths);
        when(operationRunner.createScanBatchOnline(TEST_UUID, scanPaths, projectNameVersion, dockerTargetData,
                blackDuckRunData, false)).thenReturn(scanBatch);
        when(blackDuckRunData.shouldWaitAtScanLevel()).thenReturn(true);

        SignatureScanStepRunner spyRunner = spy(signatureScanStepRunner);

        doReturn(Collections.emptyList()).when(spyRunner).executeScan(any(), any(), any(), any(), any(), anyBoolean(),
                anyBoolean(), any());

        spyRunner.runSignatureScannerOnline(TEST_UUID, projectNameVersion, dockerTargetData, scanIdsToWaitFor, gson, any());

        verify(spyRunner, times(1)).executeScan(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean(), any());
    }

    private HttpHostConnectException createHttpHostConnectException() {
        ConnectException connectException = new ConnectException("Connection failed");
        HttpHost httpHost = new HttpHost("scass.blackduck.com", 443, "https");
        return new HttpHostConnectException(httpHost, connectException);
    }
}