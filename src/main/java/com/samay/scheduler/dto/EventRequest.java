package com.samay.scheduler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EventRequest {
    @NotBlank
    private String entityName;
    @NotBlank
    private String fieldName;
    @NotBlank
    private String newValue;
}