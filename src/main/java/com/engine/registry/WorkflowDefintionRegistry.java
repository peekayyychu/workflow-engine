package com.engine.registry;

import com.engine.model.WorkflowDefinition;

public interface  WorkflowDefintionRegistry {
    void registerTemplate(String definitionKey, WorkflowDefinition definition);
    WorkflowDefinition getTemplate(String definitionKey);
    void associateInstance(String workflowId, WorkflowDefinition defintion);
    WorkflowDefinition getForInstance(String workflowId);
}
