package com.engine.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.engine.model.ActivityScheduledEvent;
import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowState;
import com.engine.model.WorkflowDefinition;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor  
public class WorkFlowOrchestrator {

    private final WorkFlowEventService workFlowEventService;
    private final WorkFlowDecisionEngine workflowDecisionEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    public void  processWorkFlow(String workflowId, WorkflowDefinition definition){
        WorkFlowState state;
        state = workFlowEventService.getWorkFlowState(workflowId);
        WorkFlowDecision decision = workflowDecisionEngine.evaluateDecision(definition, state);

        takeAction(workflowId, decision, definition);
    }

    private void takeAction(String workflowId, WorkFlowDecision decision, WorkflowDefinition definition){
        switch(decision) {
            case WorkFlowDecision.ScheduleActivity schedule -> {
                workFlowEventService.recordActivity(workflowId, schedule.activityType(), WorkFlowEvent.EventType.ACTIVITY_SCHEDULED, workflowId);

                eventPublisher.publishEvent(new ActivityScheduledEvent(workflowId, schedule.activityType(), definition, schedule.input()));
            }
            case WorkFlowDecision.CompleteWorkflow complete -> {
                workFlowEventService.completeWorkFlow(workflowId, objectMapper.writeValueAsString(complete.output()));
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
