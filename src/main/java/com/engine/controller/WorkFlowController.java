package com.engine.controller;

import java.util.List;
import java.util.Objects;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowState;
import com.engine.model.request.WorkFlowStartRequest;
import com.engine.service.WorkFlowEventService;
import com.engine.service.WorkFlowOrchestrator;

@RestController 
@RequestMapping ("/api/v1/workflows/{workflowId}")
public class WorkFlowController {
    private final WorkFlowEventService workFlowEventService;

    private final WorkFlowOrchestrator workFlowOrchestrator;

    WorkFlowController(WorkFlowOrchestrator workFlowOrchestrator, WorkFlowEventService workFlowEventService) {
        this.workFlowOrchestrator = workFlowOrchestrator;
        this.workFlowEventService = workFlowEventService;
    }

    @PostMapping("/start")
    public ResponseEntity<WorkFlowEvent> startWorkflow(
        @PathVariable String workflowId,
        @RequestBody(required = false) WorkFlowStartRequest request
    ){
        String eventPayload = StringUtils.hasText(request.payload()) ? request.payload() : null;
        WorkFlowEvent event = workFlowEventService.appendEvent(workflowId, null, WorkFlowEvent.EventType.WORKFLOW_STARTED, eventPayload);

        if(Objects.nonNull(request.definition())){
            Thread.ofVirtual().start(()->{
                workFlowOrchestrator.processWorkFlow(workflowId, request.definition());
            });
        }
        
        return ResponseEntity.ok(event);
    }

    @GetMapping ("/history")
    public ResponseEntity<List<WorkFlowEvent>> getWorkflowHistory(
        @PathVariable String workflowId
    ){  
        return ResponseEntity.ok(workFlowEventService.getEventsByWorkflowId(workflowId));
    }

    @PostMapping ("/activity/{activityName}/complete")
    public ResponseEntity<WorkFlowEvent> completeActivity(
        @PathVariable String workflowId,
        @PathVariable String activityName,
        @RequestBody(required = false) String payload
    ){
        String eventPayload = StringUtils.hasText(payload) ? payload : null;
        WorkFlowEvent event = workFlowEventService.recordActivity(workflowId, activityName, WorkFlowEvent.EventType.ACTIVITY_COMPLETED, eventPayload);
        return ResponseEntity.ok(event);
    }

    @PostMapping ("/complete")
    public ResponseEntity<WorkFlowEvent> completeWorkflow(
        @PathVariable String workflowId,
        @RequestBody String payload
    ){
        return ResponseEntity.ok(workFlowEventService.completeWorkFlow(workflowId, payload));
    }

    @GetMapping("/state")
    public ResponseEntity<WorkFlowState> getWorkflowState(@PathVariable String workflowId) {
        return ResponseEntity.ok(workFlowEventService.getWorkFlowState(workflowId));
    }
    
    @PostMapping("/activity/{activityName}/fail")
    public ResponseEntity<WorkFlowEvent> failActivity(
            @PathVariable String workflowId,
            @PathVariable String activityName,
            @RequestBody String payload) {
        return ResponseEntity.ok(workFlowEventService.failActivity(workflowId, activityName, payload));
    }

    @PostMapping("/fail")
    public ResponseEntity<WorkFlowEvent> failWorkflow(
            @PathVariable String workflowId,
            @RequestBody String payload) {
        return ResponseEntity.ok(workFlowEventService.failWorkflow(workflowId, payload));
    }
}
