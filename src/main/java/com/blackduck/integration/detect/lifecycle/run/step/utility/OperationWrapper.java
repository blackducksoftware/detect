package com.blackduck.integration.detect.lifecycle.run.step.utility;

import java.io.IOException;
import java.util.function.Consumer;

import com.blackduck.integration.detect.lifecycle.BlackDuckDuplicateProjectException;
import com.blackduck.integration.detect.workflow.componentlocationanalysis.ComponentLocatorException;
import org.apache.commons.lang3.StringUtils;

import com.blackduck.integration.blackduck.exception.BlackDuckApiException;
import com.blackduck.integration.detect.configuration.DetectUserFriendlyException;
import com.blackduck.integration.detect.lifecycle.OperationException;
import com.blackduck.integration.detect.workflow.status.Operation;
import com.blackduck.integration.exception.IntegrationException;

public class OperationWrapper {

    private static final int MESSAGE_LENGTH_LIMIT = 600;

    public void wrapped(Operation operation, OperationFunction supplier) throws OperationException {
        wrapped(operation, () -> { //To reduce duplication, calling the supplier with a return type but throwing away the returned result.
            supplier.execute();
            return true;
        });
    }

    public <T> T wrapped(Operation operation, OperationSupplier<T> supplier) throws OperationException {
        return wrapped(operation, supplier, () -> {}, (e) -> {});
    }

    public <T> T wrappedWithCallbacks(Operation operation, OperationSupplier<T> supplier, Runnable successConsumer, Consumer<Exception> errorConsumer) throws OperationException {
        return wrapped(operation, supplier, successConsumer, errorConsumer);
    }

    public <T> T wrapped(Operation operation, OperationSupplier<T> supplier, Runnable successConsumer, Consumer<Exception> errorConsumer) throws OperationException {
        try {
            T value = supplier.execute();
            operation.success();
            successConsumer.run();
            return value;
        } catch (ComponentLocatorException e) {
            // Set operation status to failure but let Detect exit successfully
            operation.error(e);
            errorConsumer.accept(e);
            return null;
        } catch (InterruptedException e) {
            operation.error(e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            errorConsumer.accept(e);
            throw new OperationException(e);
        } catch (OperationException e) {
            operation.error(e);
            errorConsumer.accept(e);
            throw e;
        } catch (BlackDuckApiException e) {
            String contentDetails = "Black Duck response body: " + e.getOriginalIntegrationRestException().getHttpResponseContent();
            if (StringUtils.isNotBlank(contentDetails)) {
                if (contentDetails.length() > MESSAGE_LENGTH_LIMIT) {
                    contentDetails = contentDetails.substring(0, MESSAGE_LENGTH_LIMIT) + "...";
                }
                operation.error(e, contentDetails);
            } else {
                operation.error(e);
            }
            errorConsumer.accept(e);
            if (e.getBlackDuckErrorCode().contains("central.constraint_violation.project_name_duplicate_not_allowed")) {
                throw new BlackDuckDuplicateProjectException(e);
            }
            throw new OperationException(e);
        } catch (Exception e) {
            // in some cases, the problem is buried in a nested exception 
            // (i.e. a "caused by" exception.  This will drill into that 
            // hierarchy and get the real error message.
            if (null != e.getCause()) {
                String rootMessage = rootCauseMessage(e);
                operation.error(e, rootMessage);
            } else {
                operation.error(e);
            }
            errorConsumer.accept(e);
            throw new OperationException(e);
        } finally {
            operation.finish();
        }
    }
    
    private String rootCauseMessage(Exception e) {
        String msg = "";
        Throwable t = e.getCause();
        if (null == t) {
            return e.getMessage();
        } else if (t instanceof Exception) {
            msg = this.rootCauseMessage((Exception)t);
        } else {
            msg = e.getMessage();
        }
        return msg;
    }

    @FunctionalInterface
    public interface OperationSupplier<T> {
        T execute() throws OperationException, DetectUserFriendlyException, IntegrationException, InterruptedException, IOException, ComponentLocatorException; //basically all known detect exceptions.
    }

    @FunctionalInterface
    public interface OperationFunction {
        void execute() throws OperationException, DetectUserFriendlyException, IntegrationException, InterruptedException, IOException, ComponentLocatorException; //basically all known detect exceptions.
    }
}
