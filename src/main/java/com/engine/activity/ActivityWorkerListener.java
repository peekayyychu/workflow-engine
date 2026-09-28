package com.engine.activity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.engine.model.ActivityScheduledEvent;
import com.engine.model.WorkFlowEvent.EventType;
import com.engine.service.WorkFlowEventService;
import com.engine.service.WorkFlowOrchestrator;

import tools.jackson.databind.ObjectMapper;

@Component
public class ActivityWorkerListener {    
    private final Map<String, ActivityHandler> handlerRegistry;

    private final WorkFlowEventService workFlowEventService;
 
    private final WorkFlowOrchestrator workFlowOrchestrator;

    private final ObjectMapper objectMapper;

    public ActivityWorkerListener(
        List<ActivityHandler> handlers,
        WorkFlowEventService workFlowEventService,
        @Lazy WorkFlowOrchestrator workFlowOrchestrator,
        ObjectMapper objectMapper
    ){
        this.workFlowEventService = workFlowEventService;
        this.workFlowOrchestrator = workFlowOrchestrator;
        this.objectMapper = objectMapper;

        this.handlerRegistry = handlers.stream()
            .collect(Collectors.toMap(
                ActivityHandler::getActivityName,
                handler -> handler,
                (existing, replacement) -> existing // Prevent duplicate key crashes
            ));
    }

    @Async 
    @EventListener 
    public void handleActivityScheduled(ActivityScheduledEvent event){
        ActivityHandler handler = handlerRegistry.get(event.activityName());

        if(Objects.isNull(handler)){
            workFlowEventService.failActivity(event.workflowId(), event.activityName(), "No handler registered for: " + event.activityName());
            workFlowOrchestrator.processWorkFlow(event.workflowId(), event.workflowDefinition());
            return;
        }

        try{
            Map<String, Object> output = handler.execute(event.inputPayload());
            workFlowEventService.recordActivity(event.workflowId(), event.activityName(), EventType.ACTIVITY_COMPLETED, objectMapper.writeValueAsString(output));

        }catch(Exception e){
            workFlowEventService.failActivity(event.workflowId(), event.activityName(), e.getMessage());
        }

        workFlowOrchestrator.processWorkFlow(event.workflowId(), event.workflowDefinition());
    }

}
