package com.engine.model;

import java.time.Instant;

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
}
