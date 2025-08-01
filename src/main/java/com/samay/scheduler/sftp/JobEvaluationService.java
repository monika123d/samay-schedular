package com.samay.scheduler.sftp;

import com.samay.scheduler.config.SftpMonitorProperties;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.persistence.JobStatus;
import com.samay.scheduler.trigger.FileArrivalTrigger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobEvaluationService {

    private final SftpFileService fileService;
    private final JobRepository jobRepository;
    private final SftpMonitorProperties monitorProperties;
    private final RestTemplate restTemplate;

    public boolean evaluateJobForFile(SftpClient client, SftpClient.DirEntry file, JobEntity job) {
        if (JobStatus.PROCESSED.equals(job.getJobStatus()) && LocalDate.now().equals(job.getLastTriggeredDate())) {
            log.debug("Job '{}' already processed today. Skipping re-processing.", job.getName());
            return false;
        }

        Map<String, String> params = job.getTriggerParameters();
        String filename = file.getFilename();
        LocalTime now = LocalTime.now();
        LocalTime start = LocalTime.parse(params.get("start_window"));
        LocalTime end = LocalTime.parse(params.get("end_window"));

        if (now.isBefore(start) || now.isAfter(end)) {
            log.debug("File '{}' is outside the allowed time window: {} - {}", filename, start, end);
            return false;
        }

        String jobDir = monitorProperties.getProcessingDir();
        fileService.ensureDir(client, jobDir);

        String pollingDir = monitorProperties.getPollingDir();


        String filePattern = params.get("file_pattern");
        int current = fileService.countFiles(client, pollingDir, filePattern).size();
        int expected = Integer.parseInt(params.get("file_count"));

        log.debug("File count for job '{}': current = {}, expected = {}", job.getName(), current, expected);

        if (current == expected) {

            List<SftpClient.DirEntry> files = fileService.countFiles(client, pollingDir, filePattern);

            for (SftpClient.DirEntry sftpFile : files) {
                String src = pollingDir + "/" + sftpFile.getFilename();
                String dst = jobDir + "/" + sftpFile.getFilename();
                log.debug("Moving file '{}' to processing directory '{}'", filename, jobDir);
                fileService.ensureDir(client, jobDir);
                fileService.moveFile(client, src, dst, "Moved to Processing");
            }
            log.debug("Required file count met for job '{}'. Preparing to trigger workflow.", job.getName());
            triggerWorkflowIfValid(client, job, filename, now, jobDir);
        }

        return true;
    }

    private void triggerWorkflowIfValid(SftpClient client, JobEntity job, String file, LocalTime now, String jobDir) {

        Map<String, String> params = job.getTriggerParameters();
        String filePattern = params.get("file_pattern");

        log.debug("Creating FileArrivalTrigger for job '{}'.", job.getName());
        FileArrivalTrigger trigger = new FileArrivalTrigger(
                job.getTriggerParameters().get("operator"),
                job.getTriggerParameters().get("start_window"),
                job.getTriggerParameters().get("end_window"),
                job.getTriggerParameters().get("file_type"),
                job.getTriggerParameters().get("file_count"),
                job.getTriggerParameters().get("file_pattern")
        );

        log.debug("Evaluating trigger conditions for file '{}'.", file);
        if (!trigger.evaluateConditions(file, now, fileService.countFiles(client, jobDir, filePattern).size())) {
            log.debug("Conditions not met for triggering job '{}'.", job.getName());
            return;
        }

        try {
            job.setJobStatus(JobStatus.PROCESSED);
            job.setLastTriggeredDate(LocalDate.now());
            jobRepository.save(job);
            log.debug("Job {} marked as PROCESSED and saved to DB with date {}", job.getName(), job.getLastTriggeredDate());
        } catch (Exception e) {
            log.error("Failed to update job status to PROCESSED: {}", e.getMessage(), e);
        }

        try {
            String endpoint = "http://localhost:8080/workflow/start?name=" + job.getWorkflowName();
            log.debug("Sending REST request to trigger workflow at: {}", endpoint);
            String res = restTemplate.getForObject(endpoint, String.class);
            log.debug("Workflow '{}' triggered. Server response: {}", job.getWorkflowName(), res);

            String doneDir = monitorProperties.getProcessedDir();
            fileService.ensureDir(client, doneDir);
            fileService.moveAllFiles(client, jobDir, doneDir, "Processed");
        } catch (Exception e) {
            log.error("Error triggering workflow '{}': {}", job.getWorkflowName(), e.getMessage(), e);
        }
    }
}
