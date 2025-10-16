package com.blackduck.integration.detect.lifecycle.run.step.utility;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackduck.integration.detect.configuration.enumeration.DetectTool;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.util.filter.DetectToolFilter;
import com.blackduck.integration.detect.workflow.report.util.ReportConstants;
import com.blackduck.integration.detect.workflow.status.Operation;
import com.blackduck.integration.detect.workflow.status.OperationSystem;
import com.blackduck.integration.detect.workflow.status.OperationType;

public class StepHelper {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final OperationSystem operationSystem;
    private final OperationWrapper operationWrapper;
    private final DetectToolFilter detectToolFilter;

    public StepHelper(OperationSystem operationSystem, OperationWrapper operationWrapper, DetectToolFilter detectToolFilter) {
        this.operationSystem = operationSystem;
        this.operationWrapper = operationWrapper;
        this.detectToolFilter = detectToolFilter;
    }

    public void runToolIfIncluded(DetectTool detectTool, String name, OperationWrapper.OperationFunction supplier) throws OperationException {
        runToolIfIncluded(detectTool, name, () -> {
            supplier.execute();
            return true;
        }, () -> {}, (e) -> {});
    }

    public void runToolIfIncluded(DetectTool detectTool, String name, OperationWrapper.OperationFunction supplier, List<CompletableFuture<Void>> scanFutures, ExecutorService executorService) {
        scanFutures.add(CompletableFuture.runAsync(() -> {
            Thread.currentThread().setName(name + " Thread");
            try {
                runToolIfIncluded(detectTool, name, () -> {
                    supplier.execute();
                    return true;
                }, () -> {}, (e) -> {});
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService));

    }

    public <T> Optional<T> runToolIfIncluded(DetectTool detectTool, String name, OperationWrapper.OperationSupplier<T> supplier) throws OperationException {
        return runToolIfIncluded(detectTool, name, supplier, () -> {}, (e) -> {});
    }

    public void runToolIfIncludedWithCallbacks(
        DetectTool detectTool,
        String name,
        OperationWrapper.OperationFunction supplier,
        Runnable successConsumer,
        Consumer<Exception> errorConsumer
    )
        throws OperationException {
        runToolIfIncluded(detectTool, name, () -> {
            supplier.execute();
            return true;
        }, successConsumer, errorConsumer);
    }

    private <T> Optional<T> runToolIfIncluded(
        DetectTool detectTool,
        String name,
        OperationWrapper.OperationSupplier<T> supplier,
        Runnable successConsumer,
        Consumer<Exception> errorConsumer
    )
        throws OperationException {
        logger.info(ReportConstants.RUN_SEPARATOR);
        if (detectToolFilter.shouldInclude(detectTool)) {
            logger.info("Will include the " + name + " tool.");
            Operation operation = operationSystem.startOperation(name + " Tool", OperationType.INTERNAL);
            T value = operationWrapper.wrappedWithCallbacks(operation, supplier, successConsumer, errorConsumer);
            logger.info(name + " actions finished.");
            return Optional.ofNullable(value);
        } else {
            logger.info(name + " tool will not be run.");
            return Optional.empty();
        }
    }

    public void runAsGroup(String name, OperationType type, OperationWrapper.OperationFunction supplier) throws OperationException {
        runAsGroup(name, type, () -> {
            supplier.execute();
            return true;
        });
    }

    public <T> T runAsGroup(String name, OperationType type, OperationWrapper.OperationSupplier<T> supplier) throws OperationException {
        Operation operation = operationSystem.startOperation(name, type);
        return operationWrapper.wrapped(operation, supplier);
    }
}
