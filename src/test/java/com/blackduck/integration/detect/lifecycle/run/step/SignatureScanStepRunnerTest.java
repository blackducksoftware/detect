package com.blackduck.integration.detect.lifecycle.run.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpHost;
import org.apache.http.conn.HttpHostConnectException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import com.blackduck.integration.blackduck.codelocation.Result;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatch;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchOutput;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.blackduck.integration.blackduck.codelocation.signaturescanner.command.ScanCommandOutput;
import com.blackduck.integration.blackduck.service.model.NotificationTaskRange;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.DockerTargetData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.tool.signaturescanner.SignatureScanPath;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanOuputResult;
import com.blackduck.integration.detect.tool.signaturescanner.operation.SignatureScanResult;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class SignatureScanStepRunnerTest {
    
    @Mock
    private OperationRunner operationRunner;
    
    @Mock
    private ScanBatchRunner scanBatchRunner;
    
    @Mock
    private BlackDuckRunData blackDuckRunData;
    
//    @TempDir
//    Path tempDir;
    
    private SignatureScanStepRunner signatureScanStepRunner;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, blackDuckRunData);
    }

    @Test
    public void testRunSignatureScannerOnlineRetriesOnHttpHostConnectException() throws Exception {
        // Arrange
        String detectRunUuid = "test-uuid";
        NameVersion projectNameVersion = new NameVersion("TestProject", "1.0.0");
        DockerTargetData dockerTargetData = mock(DockerTargetData.class);
        Set<String> scanIdsToWaitFor = new HashSet<>();
        Gson gson = new Gson();

        List<SignatureScanPath> scanPaths = Collections.singletonList(mock(SignatureScanPath.class));
        ScanBatch scanBatch = mock(ScanBatch.class);

        when(operationRunner.createScanPaths(projectNameVersion, dockerTargetData)).thenReturn(scanPaths);
        when(operationRunner.createScanBatchOnline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData, blackDuckRunData, false)).thenReturn(scanBatch);
        when(operationRunner.createScanBatchOnline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData, blackDuckRunData, true)).thenReturn(scanBatch);

        // Ensure blackDuckRunData is mocked and stub the method
        when(blackDuckRunData.shouldWaitAtScanLevel()).thenReturn(true);

        SignatureScanStepRunner spyRunner = spy(signatureScanStepRunner);

        // Mock both calls to executeScan
        doThrow(HttpHostConnectException.class)
            .doReturn(Collections.emptyList())
            .when(spyRunner)
            .executeScan(scanBatch, scanBatchRunner, scanPaths, scanIdsToWaitFor, gson, true, true);

        // Act
        spyRunner.runSignatureScannerOnline(detectRunUuid, projectNameVersion, dockerTargetData, scanIdsToWaitFor, gson);

        // Assert
        verify(spyRunner, times(2)).executeScan(scanBatch, scanBatchRunner, scanPaths, scanIdsToWaitFor, gson, true, true);
    }
    
//    @Test
//    public void testRunSignatureScannerOnlineRetriesAndExecutesScanTwice() throws IOException, OperationException, IntegrationException {
//        List<SignatureScanPath> scanPaths = Collections.singletonList(mock(SignatureScanPath.class));
//        ScanBatch scanBatch = mock(ScanBatch.class);
//        NotificationTaskRange notificationTaskRange = mock(NotificationTaskRange.class);
//        
//        ScanCommandOutput scanCommandOutput = createMockScanCommandOutputWithScanResult();
//        ScanBatchOutput scanBatchOutput = mock(ScanBatchOutput.class);
//        when(scanBatchOutput.getOutputs()).thenReturn(Collections.singletonList(scanCommandOutput));
//        
//        SignatureScanOuputResult scanOutputResult = mock(SignatureScanOuputResult.class);
//        when(scanOutputResult.getScanBatchOutput()).thenReturn(scanBatchOutput);
//        
//        NameVersion projectNameVersion = new NameVersion("TestProject", "1.0.0");
//        DockerTargetData dockerTargetData = mock(DockerTargetData.class);
//        String detectRunUuid = "test-uuid-123";
//        
//        when(operationRunner.createScanPaths(projectNameVersion, dockerTargetData)).thenReturn(scanPaths);
//        when(operationRunner.createScanBatchOnline(detectRunUuid, scanPaths, projectNameVersion, dockerTargetData, blackDuckRunData, false)).thenReturn(scanBatch);
//        when(operationRunner.createCodeLocationRange(blackDuckRunData)).thenReturn(notificationTaskRange);
//        when(operationRunner.signatureScan(scanBatch, scanBatchRunner)).thenReturn(scanOutputResult);
//        when(blackDuckRunData.shouldWaitAtScanLevel()).thenReturn(true);
//        when(scanBatch.isScassScan()).thenReturn(true);
//        when(scanBatch.isCsvArchive()).thenReturn(false);
//        
//        ConnectException connectException = new ConnectException("Connection failed");
//        HttpHost httpHost = new HttpHost("scass.blackduck.com", 443, "https");
//        HttpHostConnectException httpException = new HttpHostConnectException(httpHost, connectException);
//        IntegrationException integrationException = new IntegrationException("SCASS upload failed");
//        
//        SignatureScanStepRunner signatureScanStepRunner = new SignatureScanStepRunner(operationRunner, blackDuckRunData);
//        SignatureScanStepRunner spyRunner = spy(signatureScanStepRunner);
//        
//        try (MockedStatic<ScassScanStepRunner> mockedScassRunner = mockStatic(ScassScanStepRunner.class)) {
//            ScassScanStepRunner mockScassRunner = mock(ScassScanStepRunner.class);
//            mockedScassRunner.when(() -> new ScassScanStepRunner(blackDuckRunData)).thenReturn(mockScassRunner);
//            
//            doThrow(integrationException).when(mockScassRunner).runScassScan(any(), any());
//            
//            HttpHostConnectException thrownException = assertThrows(HttpHostConnectException.class, () -> {
//                Gson gson = new Gson();
//                Set<String> scanIdsToWaitFor = new HashSet<>();
//                
//                spyRunner.executeScan(scanBatch, scanBatchRunner, scanPaths, scanIdsToWaitFor, gson, true, true);
//            });
//            
//            assertEquals(httpException, thrownException);
//        }
//    }
//
//    private ScanCommandOutput createMockScanCommandOutputWithScanResult() throws IOException {
//        ScanCommandOutput output = createMockScanCommandOutput();
//        
//        File outputDir = output.getSpecificRunOutputDirectory();
//        File bdioDir = new File(outputDir, "bdio");
//        bdioDir.mkdirs();
//        
//        File scanResultFile = new File(outputDir, SignatureScanResult.OUTPUT_FILE_PATH);
//        scanResultFile.getParentFile().mkdirs();
//        
//        String jsonContent = "{\n" +
//                "  \"uploadUrl\": \"https://scass.blackduck.com/upload\",\n" +
//                "  \"scanId\": \"scan-123\",\n" +
//                "  \"scanIds\": [\"scan-123\"]\n" +
//                "}";
//        
//        Files.write(scanResultFile.toPath(), jsonContent.getBytes());
//        
//        File bdioFile = new File(bdioDir, "scan-123.bdio");
//        Files.write(bdioFile.toPath(), "content".getBytes());
//        
//        return output;
//    }
//
//    private ScanCommandOutput createMockScanCommandOutput() {
//      ScanCommandOutput output = mock(ScanCommandOutput.class);
//      when(output.getResult()).thenReturn(Result.SUCCESS);
//      when(output.getCodeLocationName()).thenReturn("test-location");
//      
//      File outputDir = tempDir.resolve("scan-output").toFile();
//      outputDir.mkdirs();
//      when(output.getSpecificRunOutputDirectory()).thenReturn(outputDir);
//      
//      return output;
//    }
}

//@Test
//public void testRunSignatureScannerOnlineRetriesAndExecutesScanTwice() throws DetectUserFriendlyException, OperationException, IOException, BlackDuckIntegrationException {
//  // Arrange
//  OperationRunner operationRunner = mock(OperationRunner.class);
//  BlackDuckRunData blackDuckRunData = mock(BlackDuckRunData.class);
//  ScanBatchRunner scanBatchRunner = mock(ScanBatchRunner.class);
//  ScanBatch scanBatch = mock(ScanBatch.class);
//  Gson gson = mock(Gson.class);
//
//  when(operationRunner.createScanPaths(any(), any())).thenReturn(Collections.emptyList());
//  when(operationRunner.createScanBatchOnline(any(), any(), any(), any(), any(), eq(false))).thenReturn(scanBatch);
//  when(operationRunner.createScanBatchOnline(any(), any(), any(), any(), any(), eq(true))).thenReturn(scanBatch);
//  when(operationRunner.createCodeLocationRange(any())).thenReturn(null);
//  when(scanBatchRunner.executeScans(any())).thenThrow(HttpHostConnectException.class).thenReturn(null);
//
//  SignatureScanStepRunner signatureScanStepRunner = spy(new SignatureScanStepRunner(operationRunner, blackDuckRunData));
//
//  // Act
//  signatureScanStepRunner.runSignatureScannerOnline("testUuid", mock(NameVersion.class), mock(DockerTargetData.class), mock(Set.class), gson);
//
//  // Assert
//  //verify(signatureScanStepRunner, atLeast(2)).executeScan(any(), any(), any(), any(), any(), anyBoolean(), anyBoolean());
//}
