package com.samay.scheduler.dto;

import com.samay.scheduler.persistence.JobStatus;
import com.samay.scheduler.persistence.TriggerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class CreateJobRequest {
    @NotBlank(message = "Job name is required")
    private String name;

    @NotBlank(message = "Workflow name is required")
    private String workflowName;

    @NotNull(message = "Trigger type is required")
    private TriggerType triggerType;

    @NotEmpty(message = "Trigger parameters are required")
    private Map<String, String> triggerParameters;

    private Long freqInterval;

    @NotNull(message = "Job Status is required")
    private JobStatus jobStatus;

    @NotNull(message = "Retry Count is required")
    private int retryCount;
}