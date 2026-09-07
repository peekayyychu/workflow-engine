package com.engine.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Propagation;

import com.engine.model.WorkFlowEvent;
import com.engine.model.WorkFlowState;
import com.engine.repository.WorkFlowEventRepository;


@Service 
public class WorkFlowEventService {
    @Autowired 
    private WorkFlowEventRepository workFlowEventRepository;

    @Retryable (
        retryFor = { DataIntegrityViolationException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 50)
    )
    @Transactional (propagation = Propagation.REQUIRES_NEW)
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
                case ACTIVITY_FAILED -> state.setStatus("FAILED");
                case WORKFLOW_STARTED, ACTIVITY_SCHEDULED, ACTIVITY_COMPLETED -> state.setStatus("RUNNING");
                default -> {
                }
            }
        }

        return state;
    }

    public WorkFlowEvent failActivity(String workflowId, String activityName, String failureDetails){
        List<WorkFlowEvent> history = workFlowEventRepository.findByWorkflowIdOrderBySequenceNumberAsc(workflowId);

        if(history.isEmpty()){
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Workflow " + workflowId + " not found"
            );
        }
        int nextSequence = history.size() + 1;

        WorkFlowEvent event = new WorkFlowEvent(null, workflowId, nextSequence, WorkFlowEvent.EventType.ACTIVITY_FAILED, failureDetails, Instant.now());
        return workFlowEventRepository.save(event);
    }

    public WorkFlowEvent failWorkflow(String workflowId, String failureDetails){
        List<WorkFlowEvent> history = workFlowEventRepository.findByWorkflowIdOrderBySequenceNumberAsc(workflowId);

        if(history.isEmpty()){
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Workflow " + workflowId + " not found"
            );
        }
        int nextSequence = history.size() + 1;

        WorkFlowEvent event = new WorkFlowEvent(null, workflowId, nextSequence, WorkFlowEvent.EventType.WORKFLOW_FAILED, failureDetails, Instant.now());
        return workFlowEventRepository.save(event);
    }
}
