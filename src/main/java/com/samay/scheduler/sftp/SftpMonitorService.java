package com.samay.scheduler.sftp;

import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.persistence.TriggerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SftpMonitorService {

    private final SftpFileService fileService;
    private final FilePollProcessor pollProcessor;
    private final JobExpirationHandler expirationHandler;
    private final JobRepository jobRepository;
    private final SessionFactory<SftpClient.DirEntry> sftpSessionFactory;

    //@Scheduled(fixedDelayString = "${sftp.monitor.pollInterval}")
    public void monitorFolder() {
        LocalTime pollTime = LocalTime.now();
        log.debug("\n\n ======== [Poll Start] SFTP Poll at {} ========", pollTime);

        List<JobEntity> jobs = jobRepository.findByTriggerType(TriggerType.FILE_ARRIVAL_EVENT);
        log.debug("Retrieved {} jobs with FILE_ARRIVAL_EVENT trigger type", jobs.size());

        try (Session<SftpClient.DirEntry> session = sftpSessionFactory.getSession()) {
            SftpClient sftpClient = (SftpClient) session.getClientInstance();

            expirationHandler.checkAndMarkExpiredJobs(sftpClient, jobs);

            List<SftpClient.DirEntry> files = fileService.readFiles(sftpClient);
            if (files.isEmpty()) {
                log.warn("No eligible files found. Ending poll cycle.");
                return;
            }

            for (SftpClient.DirEntry file : files) {
                if (fileService.isSkippableFile(file.getFilename())) continue;
                log.debug("Processing file: '{}'", file.getFilename());
                pollProcessor.processFile(sftpClient, file, jobs);
            }
        } catch (Exception e) {
            log.error("SFTP system failure detected during polling. All active jobs will be marked as FAILED. Reason: {}", e.getMessage(), e);
            expirationHandler.failAllPendingJobs(jobs, "SFTP system failure");
        }

        log.debug(" ======== [Poll End] Completed SFTP Poll at {} ========\n", pollTime);
    }

}
