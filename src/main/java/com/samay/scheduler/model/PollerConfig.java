package com.samay.scheduler.model;

import lombok.Data;

@Data
public class PollerConfig {

    private String scheduleType;
    private String frequencyValue;
    private String frequencyUnit;

}
