package com.samay.scheduler.sftp;

import com.samay.scheduler.config.SftpMonitorProperties;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.persistence.JobStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobExpirationHandler {

    private final JobRepository jobRepository;
    private final SftpFileService fileService;
    private final SftpMonitorProperties monitorProperties;

    public void checkAndMarkExpiredJobs(SftpClient client, List<JobEntity> jobs) {
        LocalTime now = LocalTime.now();
        for (JobEntity job : jobs) {
            Map<String, String> params = job.getTriggerParameters();
            LocalTime endWindow = LocalTime.parse(params.get("end_window"));

            if (now.isAfter(endWindow) &&
                    (job.getJobStatus() == JobStatus.IN_PROGRESS || job.getJobStatus() == JobStatus.PENDING || job.getJobStatus() == null)) {
                job.setJobStatus(JobStatus.FAILED);
                job.setLastTriggeredDate(LocalDate.now());
                jobRepository.save(job);

                String jobDir = monitorProperties.getProcessingDir() + "/" + job.getName();
                String filePattern = params.get("file_pattern");
                int current = fileService.countFiles(client, jobDir, filePattern).size();
                int expected = Integer.parseInt(params.get("file_count"));

                log.debug("File count mismatch for job '{}': current = {}, expected = {}", job.getName(), current, expected);
                log.warn("Time window expired for job '{}'. '{}'-'{}' Required files not received. Marked as FAILED.", job.getName(), expected, current);

                try {
//                    String jobDir = monitorProperties.getProcessingDir() + "/" + job.getName();
                    String failedDir = monitorProperties.getDeletedDir();
                    fileService.ensureDir(client, failedDir);
                    fileService.moveAllFiles(client, jobDir, failedDir, "Job expired - Incomplete files");
                } catch (Exception e) {
                    log.error("Failed to move/delete files for expired job '{}': {}", job.getName(), e.getMessage(), e);
                }
            }
        }
    }

    public void failAllPendingJobs(List<JobEntity> jobs, String reason) {
        for (JobEntity job : jobs) {
            if (job.getJobStatus() == JobStatus.PENDING || job.getJobStatus() == JobStatus.IN_PROGRESS) {
                job.setJobStatus(JobStatus.FAILED);
                job.setLastTriggeredDate(LocalDate.now());
                jobRepository.save(job);
                log.warn("Job '{}' marked as FAILED due to {}.", job.getName(), reason);
            }
        }
    }
}
