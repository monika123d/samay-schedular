package com.samay.scheduler.controller;

import com.samay.scheduler.dto.CreateJobRequest;
import com.samay.scheduler.job.Job;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.persistence.scheduled.ScheduledJob;
import com.samay.scheduler.service.JobFactory;
import com.samay.scheduler.service.JobSchedulerService;
import com.samay.scheduler.service.WorkflowExecutor;
import com.samay.scheduler.service.WorkflowRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class JobController {

    private final JobSchedulerService jobSchedulerService;
    private final JobRepository jobRepository;
    private final WorkflowRegistry workflowRegistry;
    private final WorkflowExecutor workflowExecutor;
    private final JobFactory jobFactory;

    @GetMapping("/workflows")
    public ResponseEntity<Set<String>> getAvailableWorkflows() {
        return ResponseEntity.ok(workflowRegistry.getAvailableWorkflowNames());
    }

    @PostMapping("/jobs")
    public ResponseEntity<JobEntity> createJob(@Valid @RequestBody CreateJobRequest request) {
        try{
            JobEntity createdJob = jobSchedulerService.createJob(request);
            return new ResponseEntity<>(createdJob, HttpStatus.CREATED);
        }catch (Exception e) {
            log.error("Failed to fetch jobs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/run-now/{jobId}")
    public ResponseEntity<?> runJobNow(@PathVariable Long jobId) {
        //to be executed.............
        return ResponseEntity.ok("Job execution started.");
    }


    @GetMapping("/jobs")
    public ResponseEntity<List<JobEntity>> getAllJobs() {
        return ResponseEntity.ok(jobRepository.findAll());
    }

    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Void> deleteJob(@PathVariable Long id) {
        jobSchedulerService.deleteJob(id);
        return ResponseEntity.noContent().build();
    }
}