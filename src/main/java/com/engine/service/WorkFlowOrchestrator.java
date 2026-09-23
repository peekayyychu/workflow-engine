package com.engine.service;

import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.engine.exception.UnresolvedVariableException;
import com.engine.model.ActivityScheduledEvent;
import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowState;
import com.engine.model.WorkflowDefinition;
import com.engine.resolver.VariableResolver;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor  
public class WorkFlowOrchestrator {

    private final WorkFlowEventService workFlowEventService;
    private final WorkFlowDecisionEngine workflowDecisionEngine;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final VariableResolver variableResolver;
    
    public void  processWorkFlow(String workflowId, WorkflowDefinition definition){
        WorkFlowState state;
        state = workFlowEventService.getWorkFlowState(workflowId);
        WorkFlowDecision decision = workflowDecisionEngine.evaluateDecision(definition, state);

        takeAction(workflowId, decision, definition, state);
    }

    private void takeAction(String workflowId, WorkFlowDecision decision, WorkflowDefinition definition, WorkFlowState state){
        switch(decision) {
            case WorkFlowDecision.ScheduleActivity schedule -> {
                try {
                    Map<String, Object> resolvedInputs = variableResolver.resolve(schedule.input(), state, workflowId, schedule.activityType());
                    String jsonPayload = objectMapper.writeValueAsString(resolvedInputs);
                    workFlowEventService.recordActivity(workflowId, schedule.activityType(), WorkFlowEvent.EventType.ACTIVITY_SCHEDULED, jsonPayload);
                    eventPublisher.publishEvent(new ActivityScheduledEvent(workflowId, schedule.activityType(), definition, resolvedInputs));
                } catch (UnresolvedVariableException e) {
                    workFlowEventService.failActivity(workflowId, schedule.activityType(), e.getMessage());

                    this.processWorkFlow(workflowId, definition);
                }catch (JacksonException e) {
                    throw new RuntimeException("Failed to serialize resolved payload for workflow " + workflowId, e);
                }
            }
            case WorkFlowDecision.CompleteWorkflow complete -> {
                String outputJson =  objectMapper.writeValueAsString(complete.output());
                workFlowEventService.completeWorkFlow(workflowId, outputJson);
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
