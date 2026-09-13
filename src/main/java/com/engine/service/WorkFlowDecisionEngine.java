package com.engine.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowState;
import com.engine.model.WorkflowDefinition;
import com.engine.model.WorkflowStep;
import com.engine.model.enums.ActivityStatus;


@Service 
public class WorkFlowDecisionEngine {

    public WorkFlowDecision evaluateDecision(WorkflowDefinition definition, WorkFlowState state){
        List<WorkFlowEvent> events = state.getEvents();

        boolean hasWorkflowFailed = events.stream()
            .anyMatch(event -> event.getEventType() == WorkFlowEvent.EventType.WORKFLOW_FAILED);

        if(hasWorkflowFailed){
            return new WorkFlowDecision.FailWorkflow("Workflow marked as failed in the event history");
        }

        Map<String, ActivityStatus> activityStatuses = aggregateActivityStatus(state);

        for(WorkflowStep step: definition.steps()){
            ActivityStatus status = activityStatuses.get(step.activityName());

            switch (status){
                case FAILED -> {
                    return new WorkFlowDecision.FailWorkflow("Activity Failed: " + step.activityName());
                }

                case PENDING -> {
                    return new WorkFlowDecision.ScheduleActivity(step.activityName(), step.defaultInput());
                }

                case SCHEDULED -> {
                    return new WorkFlowDecision.Wait();
                }

                case COMPLETED -> {}
            }
        }

        return new WorkFlowDecision.CompleteWorkflow(extractActivityOutputs(events));
    }

    @SuppressWarnings ("null")
    private Map<String, Object> extractActivityOutputs(List<WorkFlowEvent> events){
        return events.stream()
            .filter(event -> event.getEventType().equals(WorkFlowEvent.EventType.ACTIVITY_COMPLETED))
            .filter(event -> Objects.nonNull(event.getActivityName()))
            .collect(Collectors.toMap(
                WorkFlowEvent::getActivityName,
                WorkFlowEvent::getPayload,
                (existingPayload, newPayload) -> newPayload
            ));
    }

    private Map<String, ActivityStatus> aggregateActivityStatus(WorkFlowState state) {
        Map<String, ActivityStatus> statuses = new HashMap<>();

        for(WorkFlowEvent event : state.getEvents()){
            if(!ObjectUtils.isEmpty(event.getActivityName())){
                statuses.put(event.getActivityName(), toActivityStatus(event));
            }
        }

        return statuses;
    }

    private ActivityStatus toActivityStatus(WorkFlowEvent event){
        return switch (event.getEventType()){
            case ACTIVITY_SCHEDULED -> ActivityStatus.SCHEDULED;
            case ACTIVITY_COMPLETED -> ActivityStatus.COMPLETED;
            case ACTIVITY_FAILED -> ActivityStatus.FAILED;
            default -> ActivityStatus.PENDING;
        };
    }

}
