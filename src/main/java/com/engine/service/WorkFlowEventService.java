package com.engine.service;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.engine.model.WorkFlowEvent;
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
}
