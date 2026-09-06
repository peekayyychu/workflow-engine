package com.engine.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.engine.model.WorkFlowEvent;

@Repository 
public interface WorkFlowEventRepository extends JpaRepository<WorkFlowEvent, Long> {
    List<WorkFlowEvent> findByWorkflowIdOrderBySequenceNumberAsc(String workflowId);

    Optional<WorkFlowEvent> findTopByWorkflowIdOrderBySequenceNumberDesc(String workflowId);
}
