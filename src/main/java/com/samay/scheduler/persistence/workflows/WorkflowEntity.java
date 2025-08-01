package com.samay.scheduler.persistence.workflows;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "workflows")
@Data
public class WorkflowEntity {

    @Id
    @Column(nullable = false)
    private String workflowName;

    @Column(nullable = false)
    private String message;
}
