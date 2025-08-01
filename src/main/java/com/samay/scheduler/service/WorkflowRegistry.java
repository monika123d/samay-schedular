package com.samay.scheduler.service;

import com.samay.scheduler.workflow.Workflow;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class WorkflowRegistry {

    private final Map<String, Workflow> workflows;

    public WorkflowRegistry(ApplicationContext context) {
        this.workflows = context.getBeansOfType(Workflow.class);
    }

    public Workflow getWorkflow(String name) {
        Workflow workflow = workflows.get(name);
        if (workflow == null) {
            throw new IllegalArgumentException("No workflow found with name: " + name);
        }
        return workflow;
    }

    public Set<String> getAvailableWorkflowNames() {
        return workflows.keySet();
    }
}