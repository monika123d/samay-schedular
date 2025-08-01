package com.samay.scheduler.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DataChangeEvent extends ApplicationEvent {
    private final String entityName;
    private final String fieldName;
    private final String newValue;

    public DataChangeEvent(Object source, String entityName, String fieldName, String newValue) {
        super(source);
        this.entityName = entityName;
        this.fieldName = fieldName;
        this.newValue = newValue;
    }
}