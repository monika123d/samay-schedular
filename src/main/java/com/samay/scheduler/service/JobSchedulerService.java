package com.samay.scheduler.service;

import com.samay.scheduler.dto.CreateJobRequest;
import com.samay.scheduler.event.DataChangeEvent;
import com.samay.scheduler.job.Job;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.trigger.EventBasedTrigger;
import com.samay.scheduler.trigger.TimeBasedTrigger;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final List<Job> runningJobs = new CopyOnWriteArrayList<>();
    private final JobRepository jobRepository;
    private final JobFactory jobFactory;
    private final WorkflowExecutor workflowExecutor;
    private final DynamicThreadSchedulerService JobSchedulerService;

    @PostConstruct
    @Transactional(readOnly = true)
    public void initialize() {
        log.debug("Entering method initialize");
        log.info("Loading active jobs from the database...");
        List<JobEntity> activeJobs = jobRepository.findByActiveTrue();
//        runningJobs.addAll(activeJobs.stream()
//                .map(jobFactory::createJobFromEntity)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList()));
        activeJobs.forEach(JobSchedulerService::scheduleThread);
        if(activeJobs.size() == 0){
            log.debug("No Active Jobs found In DB");
        }else{
            log.info("Successfully loaded and scheduled {} jobs.", activeJobs.size());
        }
        log.debug("Exiting method initialize");
    }

    @Scheduled(fixedRateString = "${samay.scheduler.check-frequency-ms}")
    public void checkAndExecuteJobs() {
        //log.info("running jobs is empty...........");
        if (runningJobs.isEmpty()) return;

        log.trace("--- Scheduler checking {} running jobs ---", runningJobs.size());
        for (Job job : runningJobs) {
            if (job.getTrigger().isTriggered()) {
                //workflowExecutor.execute(job);
            }
        }
    }

    @EventListener
    public void handleDataChangeEvent(DataChangeEvent event) {
        log.info("[Event Listener] Received DataChangeEvent for entity: {}", event.getEntityName());
        for (Job job : runningJobs) {
            if (job.getTrigger() instanceof EventBasedTrigger eventTrigger) {
                eventTrigger.handleEvent(event);
            }

            //workflowExecutor.execute(job);
        }
    }

    @Transactional
    public JobEntity createJob(CreateJobRequest request) {
        log.debug("Entering method createJob");
        log.info("Creating new job: {}", request.getName());
        JobEntity entity = new JobEntity();
        entity.setName(request.getName());
        entity.setWorkflowName(request.getWorkflowName());
        entity.setTriggerType(request.getTriggerType());
        entity.setTriggerParameters(request.getTriggerParameters());
        entity.setActive(true);

        JobEntity savedEntity = jobRepository.save(entity);

//        Job runningJob = jobFactory.createJobFromEntity(savedEntity);
//        if (runningJob != null) {
//            runningJobs.add(runningJob);
//            log.info("Successfully created and scheduled new job '{}' (ID: {})", runningJob.getName(), runningJob.getId());
//        }

        JobSchedulerService.scheduleThread(entity);
        log.info("Successfully created and scheduled new job '{}' (ID: {})", entity.getName(), entity.getId());
        log.debug("Exiting method createJob");
        return savedEntity;
    }

    @Transactional
    public void deleteJob(Long jobId) {
        log.info("Attempting to delete job with ID: {}", jobId);
        if (!jobRepository.existsById(jobId)) {
            throw new IllegalArgumentException("Job with ID " + jobId + " not found.");
        }

        jobRepository.deleteById(jobId);
        runningJobs.removeIf(job -> Long.valueOf(job.getId()).equals(jobId));
        log.info("Successfully deleted and unscheduled job with ID: {}", jobId);
    }
}