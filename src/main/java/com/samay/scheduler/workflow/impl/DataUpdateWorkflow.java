package com.samay.scheduler.workflow.impl;

import com.samay.scheduler.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("dataUpdateWorkflow")
public class DataUpdateWorkflow implements Workflow {
    @Override
    public void execute() {
        log.info("WORKFLOW ==> Executing 'Data Update' workflow. Applying changes to the 'part' entity.");
    }
}