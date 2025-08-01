package com.samay.scheduler.service;

import com.samay.scheduler.job.Job;
import com.samay.scheduler.persistence.JobEntity;
//import com.samay.scheduler.persistence.workflows.workflowEntity;
//import com.samay.scheduler.persistence.workflows.workflowRepository;
import com.samay.scheduler.persistence.workflows.WorkflowEntity;
import com.samay.scheduler.persistence.workflows.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowExecutor {

    @Autowired
    private RestTemplate restTemplate;
    private final WorkflowRepository workflowRepository;
    private final Map<String, String> workflowQueue = new ConcurrentHashMap<>();

    public void initializer(){
        log.debug("Loading active workflows from DB into queue...");
        workflowRepository.findAll().stream()
                .filter(wf -> wf.getWorkflowName() != null)
                .forEach(wf -> {
                    workflowQueue.put(wf.getWorkflowName(), wf.getMessage());
                    log.debug("Queued workflow: {} with status {}", wf.getWorkflowName(), wf.getMessage());
                });

        log.debug("Total workflows queued: {}", workflowQueue.size());
    }

    @Async
    public void execute(JobEntity job) {
        log.debug("Entering method execute");
        try {
            //log.info("Executing workflow for job: '{}' (ID: {})", job.getName(), job.getId());

//            String endpoint = "http://localhost:8080/workflow/start?name=" + job.getWorkflow();
//            log.debug("Sending REST request to trigger workflow at: {}", endpoint);
//            String res = restTemplate.getForObject(endpoint, String.class);
//            log.debug("Workflow '{}' triggered. Server response: {}", job.getWorkflow(), res);
            WorkflowEntity workflow = new WorkflowEntity();
            workflow.setWorkflowName(job.getWorkflowName());
            workflow.setMessage("READY FOR PROCESSING"); // or any initial status you want
            workflowRepository.save(workflow);
            log.info("Persisted workflow '{}' in DB in case system restart required", job.getWorkflowName());
            log.debug("Adding Workflow : {}, to the Queue for Processing", job.getWorkflowName());
            addWorkflow(job.getWorkflowName());


        } catch (Exception e) {
            log.error("Error Adding Workflow : {} to the Queue", job.getWorkflowName());
            //log.error("Error executing workflow for job: '{}' (ID: {})", job.getName(), job.getId(), e);
        }
        log.debug("Exiting method execute");
    }

    public void addWorkflow(String workflowName) {
        log.debug("Entering method addWorkflow");
        if (workflowQueue.putIfAbsent(workflowName, "READY FOR PROCESSING") == null) {
            log.info("Workflow '{}' added to queue with status READY FOR PROCESSING.", workflowName);
        } else {
            log.warn("Workflow '{}' already exists in queue. Skipping add.", workflowName);
        }
        log.debug("Exiting method addWorkflow");
    }

    public void removeWorkflow(String workflowName) {
        if (workflowQueue.remove(workflowName) != null) {
            log.info("Workflow '{}' removed from queue after processing.", workflowName);
        } else {
            log.warn("Attempted to remove non-existing workflow '{}' from queue.", workflowName);
        }
    }

    public String getWorkflowStatus(String workflowName) {
        return workflowQueue.getOrDefault(workflowName, "NOT_FOUND");
    }

    public void printQueue() {
        log.debug("Current Workflow Queue:");
        workflowQueue.forEach((name, status) ->
                log.debug("   → {} = {}", name, status)
        );
    }
    public void clearQueue() {
        workflowQueue.clear();
        log.warn("All workflows cleared from queue.");
    }
}