package com.blackduck.integration.detect.workflow.blackduck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import com.blackduck.integration.blackduck.api.generated.enumeration.BomStatusScanStatusType;
import com.blackduck.integration.blackduck.api.generated.view.BomStatusScanView;
import com.blackduck.integration.blackduck.service.BlackDuckApiClient;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.exception.IntegrationTimeoutException;
import com.blackduck.integration.rest.HttpUrl;

import java.lang.reflect.Field;

class DetectBomScanWaitJobTest {

    @Test
    void testAttemptJobWithBuildingStatus() throws Exception {
        BlackDuckApiClient blackDuckApiClient = mock(BlackDuckApiClient.class);
        HttpUrl scanUrl = new HttpUrl("https://blackduck.com/scan");
        BomStatusScanView mockResponse = mock(BomStatusScanView.class);
        
        when(blackDuckApiClient.getResponse(scanUrl, BomStatusScanView.class)).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(BomStatusScanStatusType.BUILDING);
        
        DetectBomScanWaitJob job = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        job.attemptJob();
        
        assertFalse(job.wasJobCompleted());
    }

    @Test
    void testAttemptJobWithNotIncludedStatusLogsWarning() throws Exception {
        BlackDuckApiClient blackDuckApiClient = mock(BlackDuckApiClient.class);
        HttpUrl scanUrl = new HttpUrl("https://blackduck.com/scan");
        BomStatusScanView mockResponse = mock(BomStatusScanView.class);
        Logger mockLogger = mock(Logger.class);
        
        when(blackDuckApiClient.getResponse(scanUrl, BomStatusScanView.class)).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(BomStatusScanStatusType.NOT_INCLUDED);
        
        DetectBomScanWaitJob job = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        // Replace logger using reflection
        Field loggerField = DetectBomScanWaitJob.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(job, mockLogger);
        
        job.attemptJob();
        
        assertTrue(job.wasJobCompleted());
        verify(mockLogger).warn("Encountered unexpected scan status: {}", BomStatusScanStatusType.NOT_INCLUDED);
    }

    @Test
    void testAttemptJobWithFailureStatusNoWarning() throws Exception {
        BlackDuckApiClient blackDuckApiClient = mock(BlackDuckApiClient.class);
        HttpUrl scanUrl = new HttpUrl("https://blackduck.com/scan");
        BomStatusScanView mockResponse = mock(BomStatusScanView.class);
        Logger mockLogger = mock(Logger.class);
        
        when(blackDuckApiClient.getResponse(scanUrl, BomStatusScanView.class)).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(BomStatusScanStatusType.FAILURE);
        
        DetectBomScanWaitJob job = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        // Replace logger using reflection
        Field loggerField = DetectBomScanWaitJob.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(job, mockLogger);
        
        job.attemptJob();
        
        assertTrue(job.wasJobCompleted());
        // Verify no warning was logged for FAILURE status
        verify(mockLogger, org.mockito.Mockito.never()).warn(org.mockito.Mockito.anyString(), (Object) org.mockito.Mockito.any());
    }

    @Test
    void testAttemptJobWithSuccessfulStatusNoWarning() throws Exception {
        BlackDuckApiClient blackDuckApiClient = mock(BlackDuckApiClient.class);
        HttpUrl scanUrl = new HttpUrl("https://blackduck.com/scan");
        BomStatusScanView mockResponse = mock(BomStatusScanView.class);
        Logger mockLogger = mock(Logger.class);
        
        when(blackDuckApiClient.getResponse(scanUrl, BomStatusScanView.class)).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(BomStatusScanStatusType.SUCCESS);
        
        DetectBomScanWaitJob job = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        // Replace logger using reflection
        Field loggerField = DetectBomScanWaitJob.class.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(job, mockLogger);
        
        job.attemptJob();
        
        assertTrue(job.wasJobCompleted());
        // Verify no warning was logged for SUCCESS status
        verify(mockLogger, org.mockito.Mockito.never()).warn(org.mockito.Mockito.anyString(), (Object) org.mockito.Mockito.any());
    }

    @Test
    void testOnCompletionReturnsScanResponse() throws Exception {
        BlackDuckApiClient blackDuckApiClient = mock(BlackDuckApiClient.class);
        HttpUrl scanUrl = new HttpUrl("https://blackduck.com/scan");
        BomStatusScanView mockResponse = mock(BomStatusScanView.class);
        
        when(blackDuckApiClient.getResponse(scanUrl, BomStatusScanView.class)).thenReturn(mockResponse);
        when(mockResponse.getStatus()).thenReturn(BomStatusScanStatusType.FAILURE);
        
        DetectBomScanWaitJob job = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        job.attemptJob();
        
        assertEquals(mockResponse, job.onCompletion());
    }

    @Test
    void testGetNameIncludesScanUrl() throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = mock(BlackDuckApiClient.class);
        HttpUrl scanUrl = new HttpUrl("https://blackduck.com/scan");
        
        DetectBomScanWaitJob job = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        String name = job.getName();
        
        assertTrue(name.contains("BOM Scan Wait Job"));
        assertTrue(name.contains("https://blackduck.com/scan"));
    }

    @Test
    void testOnTimeoutThrowsIntegrationTimeoutException() throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = mock(BlackDuckApiClient.class);
        HttpUrl scanUrl = new HttpUrl("https://blackduck.com/scan");
        
        DetectBomScanWaitJob job = new DetectBomScanWaitJob(blackDuckApiClient, scanUrl);
        
        IntegrationTimeoutException exception = assertThrows(IntegrationTimeoutException.class, job::onTimeout);
        
        assertEquals("Error waiting for scan to be considered for including in BOM. Timeout may have occurred.", exception.getMessage());
    }
}