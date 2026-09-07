package com.engine.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity 
@Table (
    name = "workflow_event",
    uniqueConstraints = {
        @UniqueConstraint (name = "uc_wf_seq", columnNames = {"workflow_id", "sequence_number"})
    }
)
@Data
@AllArgsConstructor 
@NoArgsConstructor 
public class WorkFlowEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", nullable = false)
    private String workflowId;

    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant timestamp = Instant.now();

    public enum EventType {
        WORKFLOW_STARTED,
        ACTIVITY_SCHEDULED,
        ACTIVITY_COMPLETED,
        ACTIVITY_FAILED,
        WORKFLOW_COMPLETED,
        WORKFLOW_FAILED
    }
}
