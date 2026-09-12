package com.engine.model;

import java.util.List;

public record WorkflowDefinition(String workflowType, List<WorkflowStep> steps) {
    
}
