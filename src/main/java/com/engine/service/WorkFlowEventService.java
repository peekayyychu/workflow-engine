package com.engine.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowState;
import com.engine.repository.WorkFlowEventRepository;

import jakarta.transaction.Transactional;

@Service 
public class WorkFlowEventService {
    @Autowired 
    private WorkFlowEventRepository workFlowEventRepository;

    @Transactional 
    public WorkFlowEvent appendEvent(String workflowId, WorkFlowEvent.EventType eventType, String payload) {
        int sequenceNumber = workFlowEventRepository.findTopByWorkflowIdOrderBySequenceNumberDesc(workflowId)
                .map(event -> event.getSequenceNumber() + 1)
                .orElse(1);

        WorkFlowEvent event = new WorkFlowEvent(null, workflowId, sequenceNumber, eventType, payload, Instant.now());
        return workFlowEventRepository.save(event);
    }

    public List<WorkFlowEvent> getEventsByWorkflowId(String workflowId) {
        return workFlowEventRepository.findByWorkflowIdOrderBySequenceNumberAsc(workflowId);
    }

    public WorkFlowEvent recordActivity(String workflowId, String activityName, WorkFlowEvent.EventType eventType, String payload ) {
        List<WorkFlowEvent> history = workFlowEventRepository.findByWorkflowIdOrderBySequenceNumberAsc(workflowId);
        int nextSequence = history.size() + 1;

        WorkFlowEvent event = new WorkFlowEvent(null, workflowId, nextSequence, eventType, payload, Instant.now());
        return workFlowEventRepository.save(event);
    }

    public WorkFlowEvent completeWorkFlow(String workflowId, String payload){
        List<WorkFlowEvent> history = workFlowEventRepository.findByWorkflowIdOrderBySequenceNumberAsc(workflowId);
        int nextSequence = history.size() + 1;

        WorkFlowEvent event = new WorkFlowEvent(null, workflowId, nextSequence, WorkFlowEvent.EventType.WORKFLOW_COMPLETED, payload, Instant.now());
        return workFlowEventRepository.save(event);
    }

    public WorkFlowState getWorkFlowState(String workflowId){
        List<WorkFlowEvent> events = workFlowEventRepository.findByWorkflowIdOrderBySequenceNumberAsc(workflowId);

        if(events.isEmpty()){
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Workflow " + workflowId + " not found"
            );
        }

        WorkFlowState state = new WorkFlowState();

        state.setWorkflowId(workflowId);
        state.setTotalEvents(events.size());
        state.setStatus("RUNNING");

        for(WorkFlowEvent event: events){
            state.setCurrentSequenceNumber(event.getSequenceNumber());
            state.setLastUpdated(event.getTimestamp());

            switch(event.getEventType()){
                case WORKFLOW_COMPLETED -> state.setStatus("COMPLETED");
                case WORKFLOW_FAILED -> state.setStatus("FAILED");
                case WORKFLOW_STARTED, ACTIVITY_SCHEDULED, ACTIVITY_COMPLETED, ACTIVITY_FAILED -> state.setStatus("RUNNING");
                default -> {
                }
            }
        }

        return state;
    }
}
