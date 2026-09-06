package com.engine.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.engine.model.WorkFlowEvent;
import com.engine.service.WorkFlowEventService;

@RestController 
@RequestMapping ("/api/v1/workflows")
public class WorkFlowController {
    @Autowired 
    private WorkFlowEventService workFlowEventService;

    @PostMapping("/{workflowId}/start")
    public ResponseEntity<WorkFlowEvent> startWorkflow(
        @PathVariable String workflowId,
        @RequestBody(required = false) String payload
    ){
        String eventPayload = StringUtils.hasText(payload) ? payload : null;
        WorkFlowEvent event = workFlowEventService.appendEvent(workflowId, WorkFlowEvent.EventType.WORKFLOW_STARTED, eventPayload);
        return ResponseEntity.ok(event);
    }

    @GetMapping ("/{workflowId}/history")
    public ResponseEntity<List<WorkFlowEvent>> getWorkflowHistory(
        @PathVariable String workflowId
    ){  
        return ResponseEntity.ok(workFlowEventService.getEventsByWorkflowId(workflowId));
    }

    @PostMapping ("/{workflowId}/activity/{activityName}/complete")
    public ResponseEntity<WorkFlowEvent> completeActivity(
        @PathVariable String workflowId,
        @PathVariable String activityName,
        @RequestBody(required = false) String payload
    ){
        String eventPayload = StringUtils.hasText(payload) ? payload : null;
        WorkFlowEvent event = workFlowEventService.recordActivity(workflowId, activityName, WorkFlowEvent.EventType.ACTIVITY_COMPLETED, eventPayload);
        return ResponseEntity.ok(event);
    }

    @PostMapping ("/{workflowId}/complete")
    public ResponseEntity<WorkFlowEvent> completeWorkflow(
        @PathVariable String workflowId,
        @RequestBody String payload
    ){
        return ResponseEntity.ok(workFlowEventService.completeWorkFlow(workflowId, payload));
    }
}
