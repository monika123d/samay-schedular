package com.samay.scheduler.persistence.workflows;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {
    // Optional: if needed
    WorkflowEntity findByWorkflowName(String workflowName);
}
