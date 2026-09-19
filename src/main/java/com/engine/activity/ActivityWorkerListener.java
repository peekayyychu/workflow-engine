package com.engine.activity;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import tools.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.engine.model.ActivityScheduledEvent;
import com.engine.model.WorkFlowEvent.EventType;
import com.engine.service.WorkFlowEventService;
import com.engine.service.WorkFlowOrchestrator;

@Component
public class ActivityWorkerListener {
    @Autowired 
    List<ActivityHandler> handlers;
    
    private Map<String, ActivityHandler> handlerRegistry;

    @Autowired 
    private WorkFlowEventService workFlowEventService;

    @Autowired 
    @Lazy 
    private WorkFlowOrchestrator workFlowOrchestrator;

    @Autowired 
    private ObjectMapper objectMapper;

    @PostConstruct 
    public void init(){
        this.handlerRegistry = handlers.stream()
            .collect(Collectors.toMap(ActivityHandler::getActivityName, h->h));
    }

    @Async 
    @EventListener 
    public void handleActivityScheduled(ActivityScheduledEvent event){
        ActivityHandler handler = handlerRegistry.get(event.activityName());

        if(Objects.isNull(handler)){
            workFlowEventService.failActivity(event.workflowId(), event.activityName(), "No handler registered for: " + event.activityName());
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
