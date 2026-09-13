package com.engine.service;

import org.springframework.stereotype.Service;

import com.engine.model.WorkFlowState;
import com.engine.model.WorkflowDefinition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor  
public class WorkFlowOrchestrator {

    private WorkFlowEventService workFlowEventService;
    private WorkFlowDecisionEngine workflowDecisionEngine;
    
    public WorkFlowDecision processWorkFlow(String workflowId, WorkflowDefinition definition){
        WorkFlowState state = workFlowEventService.getWorkFlowState(workflowId);
        WorkFlowDecision decision = workflowDecisionEngine.evaluateDecision(definition, state);

        return decision;
    }
    
}
