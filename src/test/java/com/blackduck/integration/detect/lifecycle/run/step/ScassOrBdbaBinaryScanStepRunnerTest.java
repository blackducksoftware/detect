package com.blackduck.integration.detect.lifecycle.run.step;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
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
import com.blackduck.integration.detect.lifecycle.run.step.binary.ScassOrBdbaBinaryScanStepRunner;
import com.blackduck.integration.detect.workflow.file.DirectoryManager;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.util.NameVersion;

public class ScassOrBdbaBinaryScanStepRunnerTest {
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
    private File binaryOutputDirectory;

    private ScassOrBdbaBinaryScanStepRunner scassOrBdbaBinaryScanStepRunner;

    @BeforeEach
    public void setUp() throws OperationException, IntegrationException {
        MockitoAnnotations.openMocks(this);
        scassOrBdbaBinaryScanStepRunner = spy(new ScassOrBdbaBinaryScanStepRunner(operationRunner));
        when(operationRunner.getDirectoryManager()).thenReturn(directoryManager);
        when(directoryManager.getBinaryOutputDirectory()).thenReturn(binaryOutputDirectory);
        when(operationRunner.initiateScan(any(), any(), any(), any(), any(), any())).thenReturn(initResult);
        when(initResult.getScanCreationResponse()).thenReturn(scanCreationResponse);
        when(initResult.getZipFile()).thenReturn(mock(File.class));
        when(scanCreationResponse.getScanId()).thenReturn(UUID.randomUUID().toString());
    }

    @Test
    public void testPerformBlackduckInteractionsScass() throws Exception {
        NameVersion projectNameVersion = new NameVersion("projectName", "version");
        File binaryScanFile = mock(File.class);
        String uploadUrl = "http://upload.url";

        when(scanCreationResponse.getUploadUrl()).thenReturn(uploadUrl);
        doNothing().when(scassScanStepRunner).runScassScan(any(), any());

        doReturn(scassScanStepRunner).when(scassOrBdbaBinaryScanStepRunner).createScassScanStepRunner(nullable(BlackDuckRunData.class));

        scassOrBdbaBinaryScanStepRunner.performBlackduckInteractions(projectNameVersion, blackDuckRunData, Optional.of(binaryScanFile));

        verify(scassScanStepRunner).runScassScan(Optional.of(initResult.getZipFile()), scanCreationResponse);
    }

    @Test
    public void testPerformBlackduckInteractionsBdba() throws OperationException, IntegrationException {
        NameVersion projectNameVersion = new NameVersion("projectName", "version");
        File binaryScanFile = mock(File.class);

        when(scanCreationResponse.getUploadUrl()).thenReturn("");
        doNothing().when(bdbaScanStepRunner).runBdbaScan(any(), any(), any(), any(), any());

        doReturn(bdbaScanStepRunner).when(scassOrBdbaBinaryScanStepRunner).createBdbaScanStepRunner(nullable(OperationRunner.class));

        scassOrBdbaBinaryScanStepRunner.performBlackduckInteractions(projectNameVersion, blackDuckRunData, Optional.of(binaryScanFile));

        verify(bdbaScanStepRunner).runBdbaScan(projectNameVersion, blackDuckRunData, Optional.of(binaryScanFile), scanCreationResponse.getScanId(), "BINARY");
    }
}