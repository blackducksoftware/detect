package com.blackduck.integration.detect.lifecycle.run.step;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.data.ScanCreationResponse;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.operation.blackduck.ScassScanInitiationResult;
import com.blackduck.integration.detect.lifecycle.run.step.utility.OperationAuditLog;
import com.blackduck.integration.detect.lifecycle.run.step.utility.OperationWrapper;
import com.blackduck.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;
import com.google.gson.Gson;

public class CommonScanStepRunnerTest {
    @Mock
    private OperationRunner operationRunner;

    @Mock
    private BlackDuckRunData blackDuckRunData;

    @Mock
    private ScassScanInitiationResult initResult;

    @Mock
    private ScanCreationResponse scanCreationResponse;

    @Mock
    private ScassScanStepRunner scassScanStepRunner;

    @Mock
    private BdbaScanStepRunner bdbaScanStepRunner;

    @Mock
    private DirectoryManager directoryManager;

    @Mock
    private File mockOutputDirectory;
    
    @Mock
    private Gson gson;

    @BeforeEach
    public void setUp() throws OperationException, IntegrationException {
        MockitoAnnotations.openMocks(this);
        when(operationRunner.getDirectoryManager()).thenReturn(directoryManager);
        when(directoryManager.getBinaryOutputDirectory()).thenReturn(mockOutputDirectory);
        when(directoryManager.getContainerOutputDirectory()).thenReturn(mockOutputDirectory);
        when(operationRunner.initiateScan(any(), any(), any(), any(), any(), any(), any())).thenReturn(initResult);
        when(initResult.getScanCreationResponse()).thenReturn(scanCreationResponse);
        when(initResult.getZipFile()).thenReturn(mock(File.class));
        when(scanCreationResponse.getScanId()).thenReturn(UUID.randomUUID().toString());
        
        CodeLocationNameManager codeLocationNameManager = mock(CodeLocationNameManager.class);
        when(operationRunner.getCodeLocationNameManager()).thenReturn(codeLocationNameManager);
        when(codeLocationNameManager.createBinaryScanCodeLocationName(any(), any(), any())).thenReturn("binary");
        when(codeLocationNameManager.createContainerScanCodeLocationName(any(), any(), any())).thenReturn("container");
    }

    @Test
    public void testPerformBlackduckInteractionsBinaryScass() throws Exception {
        NameVersion projectNameVersion = new NameVersion("projectName", "version");
        File binaryScanFile = mock(File.class);
        String uploadUrl = "http://upload.url";
        
        CommonScanStepRunner commonScanStepRunner = spy(new CommonScanStepRunner());

        when(scanCreationResponse.getUploadUrl()).thenReturn(uploadUrl);
        doNothing().when(scassScanStepRunner).runScassScan(any(), any());

        doReturn(scassScanStepRunner).when(commonScanStepRunner).createScassScanStepRunner(nullable(BlackDuckRunData.class));

        commonScanStepRunner.performCommonUpload(projectNameVersion,
                blackDuckRunData, Optional.of(binaryScanFile), operationRunner, CommonScanStepRunner.BINARY, initResult, scanCreationResponse);

        verify(scassScanStepRunner).runScassScan(Optional.of(initResult.getZipFile()), scanCreationResponse);
    }

    @Test
    public void testPerformBlackduckInteractionsBinaryBdba() throws OperationException, IntegrationException {
        NameVersion projectNameVersion = new NameVersion("projectName", "version");
        File binaryScanFile = mock(File.class);
        
        CommonScanStepRunner commonScanStepRunner = spy(new CommonScanStepRunner());

        when(scanCreationResponse.getUploadUrl()).thenReturn("");
        doNothing().when(bdbaScanStepRunner).runBdbaScan(any(), any(), any(), any(), any());

        doReturn(bdbaScanStepRunner).when(commonScanStepRunner).createBdbaScanStepRunner(nullable(OperationRunner.class));
       
        commonScanStepRunner.performCommonUpload(projectNameVersion,
                blackDuckRunData, Optional.of(binaryScanFile), operationRunner, CommonScanStepRunner.BINARY, initResult, scanCreationResponse);

        verify(bdbaScanStepRunner).runBdbaScan(projectNameVersion, blackDuckRunData, Optional.of(binaryScanFile), scanCreationResponse.getScanId(), CommonScanStepRunner.BINARY);
    }
    
    @Test
    public void testPerformBlackduckInteractionsContainerScass() throws Exception {
        NameVersion projectNameVersion = new NameVersion("projectName", "version");
        File containerScanFile = mock(File.class);
        String uploadUrl = "http://upload.url";
        
        CommonScanStepRunner commonScanStepRunner = spy(new CommonScanStepRunner());

        when(scanCreationResponse.getUploadUrl()).thenReturn(uploadUrl);
        doNothing().when(scassScanStepRunner).runScassScan(any(), any());

        doReturn(scassScanStepRunner).when(commonScanStepRunner).createScassScanStepRunner(nullable(BlackDuckRunData.class));

        commonScanStepRunner.performCommonUpload(projectNameVersion,
                blackDuckRunData, Optional.of(containerScanFile), operationRunner, CommonScanStepRunner.CONTAINER, initResult, scanCreationResponse);
        
        verify(scassScanStepRunner).runScassScan(Optional.of(initResult.getZipFile()), scanCreationResponse);
    }
    
    @Test
    public void testPerformBlackduckInteractionsContainerBdba() throws OperationException, IntegrationException {
        NameVersion projectNameVersion = new NameVersion("projectName", "version");
        File containerScanFile = mock(File.class);
        
        CommonScanStepRunner commonScanStepRunner = spy(new CommonScanStepRunner());

        when(scanCreationResponse.getUploadUrl()).thenReturn("");
        doNothing().when(bdbaScanStepRunner).runBdbaScan(any(), any(), any(), any(), any());

        doReturn(bdbaScanStepRunner).when(commonScanStepRunner).createBdbaScanStepRunner(nullable(OperationRunner.class));

        commonScanStepRunner.performCommonUpload(projectNameVersion,
                blackDuckRunData, Optional.of(containerScanFile), operationRunner, CommonScanStepRunner.CONTAINER, initResult, scanCreationResponse);
        
        verify(bdbaScanStepRunner).runBdbaScan(projectNameVersion, blackDuckRunData, Optional.of(containerScanFile), scanCreationResponse.getScanId(), CommonScanStepRunner.CONTAINER);
    }
}