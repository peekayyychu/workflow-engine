package com.engine.exception.model;

public record ExceptionMetaData(
    String workflowId,
    String stepName,
    String expression,
    String reason
) {}
