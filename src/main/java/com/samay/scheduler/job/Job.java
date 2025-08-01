package com.samay.scheduler.job;

import com.samay.scheduler.trigger.Trigger;
import com.samay.scheduler.workflow.Workflow;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class Job {

    private final long id;
    private String name;
    private String workflow;
    private Trigger trigger;


    public Job(long id, String name, String workflow, Trigger trigger) {
        this.id = id;
        this.name = name;
        this.workflow = workflow;
        this.trigger = trigger;
    }

}