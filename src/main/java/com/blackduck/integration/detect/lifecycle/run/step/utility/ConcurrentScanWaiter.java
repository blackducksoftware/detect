package com.blackduck.integration.detect.lifecycle.run.step.utility;

import com.blackduck.integration.blackduck.api.generated.view.ProjectVersionView;
import com.blackduck.integration.blackduck.service.model.ProjectVersionWrapper;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.lifecycle.run.data.BlackDuckRunData;
import com.blackduck.integration.detect.lifecycle.run.operation.OperationRunner;
import com.blackduck.integration.rest.HttpUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;

public class ConcurrentScanWaiter {

    Logger logger = LoggerFactory.getLogger(ConcurrentScanWaiter.class);

    private final OperationRunner operationRunner;
    private final ExecutorService waitingPool;
    private final List<CompletableFuture<Void>> activWaits = Collections.synchronizedList(new ArrayList<>());

    public ConcurrentScanWaiter(ExecutorService waitingPool, OperationRunner operationRunner) {
        this.waitingPool = waitingPool;
        this.operationRunner = operationRunner;
    }

    public void startWaitingForScan(String scanId, BlackDuckRunData blackDuckRunData, ProjectVersionWrapper projectVersion, String threadName) {
        if (scanId == null && !blackDuckRunData.shouldWaitAtScanLevel()) return;

        CompletableFuture<Void> waitFuture = CompletableFuture.runAsync(() -> {
            Thread.currentThread().setName(threadName + " Wait For BOM Thread");
            try {

                if (scanId == null) {
                    logger.debug("Unexpected null scanID for project version" + projectVersion.getProjectVersionView().getVersionName()
                            + " skipping waiting for this scan.");
                    return;
                }

                HttpUrl bomUrl = projectVersion.getProjectVersionView().getFirstLink(ProjectVersionView.BOM_STATUS_LINK);
                HttpUrl scanUrl = new HttpUrl(bomUrl.toString() + "/" + scanId);
                operationRunner.waitForBomCompletion(blackDuckRunData, scanUrl);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, waitingPool);

        activWaits.add(waitFuture);
    }

    public void waitForAllScansToComplete() throws OperationException {
        try {
            CompletableFuture.allOf(activWaits.toArray(new CompletableFuture[0])).get();
        } catch (ExecutionException | InterruptedException e) {
            throw new OperationException(e);
        }
    }
}
