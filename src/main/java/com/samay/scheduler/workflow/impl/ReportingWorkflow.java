package com.samay.scheduler.workflow.impl;

import com.samay.scheduler.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("reportingWorkflow")
public class ReportingWorkflow implements Workflow {
    @Override
    public void execute() {
        log.info("WORKFLOW ==> Executing 'Daily Report' workflow. Generating sales and inventory reports.");
    }
}