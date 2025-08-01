package com.samay.scheduler.trigger;

import com.samay.scheduler.event.DataChangeEvent;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class EventBasedTrigger implements Trigger {
    private final String expectedEntity;
    private final String expectedField;
    private final String expectedValue;
    private final AtomicBoolean hasFired = new AtomicBoolean(false);

    public EventBasedTrigger(String entity, String field, String value) {
        this.expectedEntity = entity;
        this.expectedField = field;
        this.expectedValue = value;
    }

    public void handleEvent(DataChangeEvent event) {
        if (expectedEntity.equals(event.getEntityName()) &&
                expectedField.equals(event.getFieldName()) &&
                expectedValue.equals(event.getNewValue())) {
            log.info("[Trigger Match] Event matched for entity '{}'. Flagging for execution.", expectedEntity);
            hasFired.set(true);
        }
    }

    @Override
    public boolean isTriggered() {
        return hasFired.getAndSet(false);
    }

    @Override
    public void postExecution() {
        log.info("[Trigger Reset] EventBasedTrigger for entity '{}' has completed its execution cycle.", expectedEntity);
    }
}