package com.engine.exception;

import com.engine.exception.model.ExceptionMetaData;

public class UnresolvedVariableException extends RuntimeException {

    private final ExceptionMetaData metadata;

    public UnresolvedVariableException(String workflowId, String stepName, String expression, String reason) {
        super(String.format("Failed to resolve expression '%s' for step '%s' in workflow '%s': %s",
                expression, stepName, workflowId, reason));
        this.metadata = new ExceptionMetaData(workflowId, stepName, expression, reason);
    }

    public UnresolvedVariableException(String workflowId, String stepName, String expression, String reason, Throwable cause) {
        super(String.format("Failed to resolve expression '%s' for step '%s' in workflow '%s': %s",
                expression, stepName, workflowId, reason), cause);
        this.metadata = new ExceptionMetaData(workflowId, stepName, expression, reason);
    }

    public ExceptionMetaData getMetadata() {
        return metadata;
    }
}
