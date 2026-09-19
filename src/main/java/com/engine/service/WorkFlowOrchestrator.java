package com.engine.service;

import org.springframework.stereotype.Service;

import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowEvent.EventType;
import com.engine.model.WorkFlowState;
import com.engine.model.WorkflowDefinition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor  
public class WorkFlowOrchestrator {

    private final WorkFlowEventService workFlowEventService;
    private final WorkFlowDecisionEngine workflowDecisionEngine;
    
    public void  processWorkFlow(String workflowId, WorkflowDefinition definition){
        WorkFlowState state = workFlowEventService.getWorkFlowState(workflowId);
        WorkFlowDecision decision = workflowDecisionEngine.evaluateDecision(definition, state);

        takeAction(workflowId, decision);
    }

    private void takeAction(String workflowId, WorkFlowDecision decision){
        switch(decision) {
            case WorkFlowDecision.ScheduleActivity schedule -> {
                workFlowEventService.recordActivity(workflowId, schedule.activityType(), WorkFlowEvent.EventType.ACTIVITY_SCHEDULED, workflowId);

                //TODO: add activity dispatcher for the scheduled activity
            }
            case WorkFlowDecision.CompleteWorkflow complete -> {
                workFlowEventService.completeWorkFlow(workflowId, complete.output().toString());
            }
            case WorkFlowDecision.FailWorkflow fail -> {
                workFlowEventService.failWorkflow(workflowId, fail.reason());
            }
            case WorkFlowDecision.Wait wait -> {
                //wait
            }
                
            default -> throw new IllegalStateException("Unexpected value: " + decision);
        }
    }
}
