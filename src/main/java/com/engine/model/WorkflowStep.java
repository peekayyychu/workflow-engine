package com.engine.model;

import java.util.Map;

public record WorkflowStep(String activityName, Map<String, Object> defaultInput, int retryCount) {
    
}
