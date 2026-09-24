package com.engine.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor 
public class WorkFlowState {
    private String workflowId;
    private String status;
    private Integer currentSequenceNumber;
    private Instant lastUpdated;
    private int totalEvents;
    private List<WorkFlowEvent> events;
    private Set<String> completedList;
    private Set<String> pendingTimers;
}
