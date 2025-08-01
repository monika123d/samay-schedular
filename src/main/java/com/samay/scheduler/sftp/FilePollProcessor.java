package com.samay.scheduler.sftp;

import com.samay.scheduler.config.SftpMonitorProperties;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.persistence.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilePollProcessor {

    private final JobEvaluationService evaluationService;
    private final SftpFileService fileService;
    private final JobRepository jobRepository;
    private final SftpMonitorProperties monitorProperties;

    public void processFile(SftpClient client, SftpClient.DirEntry file, List<JobEntity> jobs) {
        String filename = file.getFilename();
        if (fileService.isSkippableFile(filename)) {
            log.debug("Skipping special file: '{}'", filename);
            return;
        }

        if (!filename.endsWith(monitorProperties.getExtension())) {
            log.warn("File '{}' has invalid extension. Moving to error folder.", filename);
            fileService.moveFile(client, monitorProperties.getPollingDir() + "/" + filename, monitorProperties.getErroredDir() + "/" + filename, "Unsupported File");
            return;
        }

        if (fileService.fileExists(client, monitorProperties.getProcessedDir(), filename)) {
            log.debug("File '{}' already exists in processed directory. Skipping.", filename);
            return;
        }

        log.debug("Evaluating file '{}' against configured jobs...", filename);
        boolean matchedAnyJob = false;
        for (JobEntity job : jobs) {
            log.debug("Checking conditions for job '{}'.", job.getName());
//            if (evaluationService.evaluateJobForFile(client, file, job)) {
//                matchedAnyJob = true;
//                if (job.getJobStatus() != JobStatus.PROCESSED) {
//                    job.setJobStatus(JobStatus.IN_PROGRESS);
//                    jobRepository.save(job);
//                    log.debug("Job '{}' marked as IN_PROGRESS and saved to DB.", job.getName());
//                }
//            }
            try {
                if (evaluationService.evaluateJobForFile(client, file, job)) {
                    matchedAnyJob = true;
                    if (job.getJobStatus() != JobStatus.PROCESSED) {
                        job.setJobStatus(JobStatus.IN_PROGRESS);
                        jobRepository.save(job);
                        log.debug("Job '{}' marked as IN_PROGRESS and saved to DB.", job.getName());
                    }
                } else {
                    // Move to backlog if condition is partially matched (customize based on condition check)
                    fileService.ensureDir(client, monitorProperties.getIgnoredDir());
                    fileService.moveFile(client,
                            monitorProperties.getPollingDir() + "/" + filename,
                            monitorProperties.getIgnoredDir() + "/" + filename,
                            "No Job Window matches for current file - moved to Ignored");
                }
            } catch (Exception e) {
                log.error("Job '{}' failed during SFTP poll: {}", job.getName(), e.getMessage());
            }
        }
        if (!matchedAnyJob) {
            log.warn("File '{}' did not match any active job window. Moving to IGNORED folder.", filename);
            fileService.moveFile(client, monitorProperties.getPollingDir() + "/" + filename, monitorProperties.getIgnoredDir() + "/" + filename, "No Matching Job Window");
        }
    }

}
