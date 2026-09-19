package com.engine.model;

import java.util.Map;

public record ActivityScheduledEvent(
    String workflowId,
    String activityName,
    WorkflowDefinition workflowDefinition,
    Map<String, Object> inputPayload
) {
    
}
