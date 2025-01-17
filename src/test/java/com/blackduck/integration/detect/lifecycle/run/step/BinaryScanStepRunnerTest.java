package com.blackduck.integration.detect.lifecycle.run.step;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.detect.lifecycle.run.step.binary.PreScassBinaryScanStepRunner;
import com.blackduck.integration.sca.upload.rest.model.response.BinaryFinishResponseContent;
import com.blackduck.integration.sca.upload.rest.status.BinaryUploadStatus;

public class BinaryScanStepRunnerTest {

    @Test
    public void testExtractBinaryScanId() {
        OperationRunner operationRunner = mock(OperationRunner.class);
        PreScassBinaryScanStepRunner binaryScanStepRunner = new PreScassBinaryScanStepRunner(operationRunner);
        
        String expectedScanId = "93420e34-348c-440a-9911-198a65ed6f00";
        String location = "https://localhost/api/intelligent-persistence-scans/93420e34-348c-440a-9911-198a65ed6f00";

        BinaryFinishResponseContent mockResponse = mock(BinaryFinishResponseContent.class);
        when(mockResponse.getLocation()).thenReturn(location);
        
        BinaryUploadStatus mockStatus = mock(BinaryUploadStatus.class);
        when(mockStatus.getResponseContent()).thenReturn(Optional.of(mockResponse));

        UUID result = binaryScanStepRunner.extractBinaryScanId(mockStatus);

        assertTrue(result != null);
        assertEquals(expectedScanId, result.toString());
    }
    
}
