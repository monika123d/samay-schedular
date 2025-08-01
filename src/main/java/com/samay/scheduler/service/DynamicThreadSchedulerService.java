package com.samay.scheduler.service;

import com.samay.scheduler.config.SftpMonitorProperties;
import com.samay.scheduler.model.PollerConfig;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.persistence.JobStatus;
import com.samay.scheduler.persistence.thread.ScheduledThreadConfig;
import com.samay.scheduler.persistence.thread.ScheduledThreadConfigRepository;
import com.samay.scheduler.sftp.SftpFileService;
import com.samay.scheduler.util.TimeUnitConverter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.keyverifier.StaticServerKeyVerifier;
import org.apache.sshd.sftp.client.SftpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Service;

import java.sql.Time;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class DynamicThreadSchedulerService{

    private final WorkflowExecutor workflowExecutor;
    private final SessionFactory<SftpClient.DirEntry> sftpSessionFactory;
    private final SftpFileService fileService;
    private final SftpMonitorProperties sftpMonitorProperties;
    private final JobRepository jobRepository;
    private final PollerConfigLoader pollerConfigLoader;
    private final TaskScheduler scheduler = new ConcurrentTaskScheduler();
    private final Map<String, JobHolder> jobMap = new ConcurrentHashMap<>();
    private volatile boolean failureOccurred = false;




    public void scheduleThread(JobEntity config) {
        log.debug("Entering method scheduleThread");
//        if(config.isLocked()){
//            log.debug("Skipping job {} as it is already Locked..", config.getName());
//            return;
//        }
        Map<String, String> params = config.getTriggerParameters();
        Runnable task = () -> {
            ZonedDateTime now = ZonedDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z VV");
            String formatted = now.format(formatter);
            if (failureOccurred) {
                log.warn("Skipping job {} as a failure has occurred", config.getName());
                stopAllThreads();
                return;
            }
            try {
                String message = "Running job: " + config.getName() + " at " + formatted;
                log.debug("\n\n ========  {} ========", message);
                Map<String, String> triggerParams = config.getTriggerParameters();
                String end = triggerParams.get("end_window");
                LocalTime endWindow = LocalTime.parse(end);
                LocalTime nowTime = LocalTime.now();
                if (nowTime.isAfter(endWindow)) {
                    log.warn("Retry window has passed for job '{}'. No more retries.", config.getName());
                    jobMap.get(config.getName()).future.cancel(false);
                    jobMap.remove(config.getName());
                    JobEntity latest = jobRepository.findById(config.getId()).orElse(null);
                    if (latest != null) {
                        latest.setActive(false);
                        latest.setJobStatus(JobStatus.FAILED);
                        jobRepository.save(latest);
                    }
                    log.debug("Exiting method scheduleThread");
                    return;
                }
                evaluateAndTrigger(config);

//                if ("minuteJob".equals(config.getName())) {
//                    throw new RuntimeException("Simulated failure in " + config.getName());
//                }
            } catch (Exception e) {
                log.error("Job {} failed with error: {}", config.getName(), e.getMessage(), e);
                failureOccurred = true;
                stopAllThreads();
            }
        };
        PollerConfig conf =  pollerConfigLoader.getConfig("INTERVAL");
            ScheduledFuture<?> future;
            switch (conf.getScheduleType()) {
                case "INTERVAL" -> {
                    Duration interval = TimeUnitConverter.convertToDuration(Long.parseLong(conf.getFrequencyValue()), conf.getFrequencyUnit());
                    future = scheduler.scheduleAtFixedRate(task, interval);
                }

//                case "DAILY" -> {
//                    ZonedDateTime nextRun = TimeUnitConverter.getNextRunTimeDaily(params.get("timeOfDay"));
//                    future = scheduler.schedule(task, Date.from(nextRun.toInstant()));
//                }
//
//                case "WEEKLY" -> {
//                    ZonedDateTime nextRun = TimeUnitConverter.getNextRunTimeWeekly(config.getDayOfWeek(), config.getTimeOfDay());
//                    future = scheduler.schedule(task, Date.from(nextRun.toInstant()));
//                }
//
//                case "MONTHLY" -> {
//                    ZonedDateTime nextRun = TimeUnitConverter.getNextRunTimeMonthly(config.getDateOfMonth(), config.getTimeOfDay());
//                    future = scheduler.schedule(task, Date.from(nextRun.toInstant()));
//                }

                default -> throw new IllegalArgumentException("Unsupported schedule type");
            }

        jobMap.put(config.getName(), new JobHolder(future, config));
        log.debug("Sheduled Job: {} | ScheduleType: {}",config.getName(), conf.getScheduleType());
        log.debug("Exiting method scheduleThread");

    }

    public void stopAllThreads() {
        for (Map.Entry<String, JobHolder> entry : jobMap.entrySet()) {
            entry.getValue().future.cancel(false);
            //entry.getValue().getConfig().setMessage("Cancelled due to failure");
            log.warn("Job {} stopped due to failure", entry.getKey());
        }
        jobMap.clear();
    }

    public void evaluateAndTrigger(JobEntity config){
        log.debug("Entering method evaluateAndTrigger");
        Map<String, String> params = config.getTriggerParameters();

        String startWindow = params.get("start_window");
        String endWindow = params.get("end_window");

        LocalTime start = LocalTime.parse(startWindow);
        LocalTime end = LocalTime.parse(endWindow);
        LocalTime now = LocalTime.now();
        log.debug("Start Window: {}, End Window: {}, Current Time: {}", start, end, now);

        if (now.isAfter(start) && now.isBefore(end)) {

            log.debug("Current time is within the allowed job window. checking for files in sftp....");
            // Mock SFTP file arrival logic — replace with real check
            boolean filesArrived = SftpFileCheck(params);

            if (filesArrived) {
                // Send to queue
                workflowExecutor.execute(config);

                try (Session<SftpClient.DirEntry> session = sftpSessionFactory.getSession()){
                    SftpClient sftpClient = (SftpClient) session.getClientInstance();
                    String src = sftpMonitorProperties.getProcessingDir();
                    String dst = sftpMonitorProperties.getProcessedDir();
                    try {
                        Thread.sleep(120000);
                    } catch (InterruptedException e) {
                        log.warn("Sleep interrupted before rechecking processing folder: {}", e.getMessage());
                        Thread.currentThread().interrupt(); // Best practice
                    }
                    fileService.moveAllFiles(sftpClient, src, dst, "moving files to processed");
                    log.info("Files for job '{}' moved from PROCESSING to PROCESSED directory.", config.getName());
                } catch (Exception e) {
                    log.error("Failed to move files from PROCESSING to PROCESSED for job '{}'. Reason: {}", config.getName(), e.getMessage(), e);
                }
                config.setActive(false);
                jobRepository.save(config);
                log.info("Job {} successfully processed and marked inactive.", config.getName());
                jobMap.get(config.getName()).future.cancel(false);
                jobMap.remove(config.getName());
            } else {
                log.info("Job {} skipped — SFTP conditions not met.", config.getName());
            }

                config.setRetryCount(config.getRetryCount()+1);
                jobRepository.save(config);
                log.debug("Updated and saved RetryCount : {} in DB", config.getRetryCount());


        } else {
            log.info("Job {} not in valid time window ({} - {})", config.getName(), startWindow, endWindow);
        }
        log.debug("Exiting method evaluateAndTrigger");
    }

    //    public List<JobEntity> getJobsByFrequencyUnit(String unit) {
//        return jobMap.values().stream()
//                .map(JobHolder::getConfig)
//                .filter(cfg -> cfg.getFrequencyUnit().equalsIgnoreCase(unit))
//                .toList();
//    }


//    public String getStatus(String schedulerName) {
//        JobHolder holder = jobMap.get(schedulerName);
//        return holder != null ? holder.getConfig().getMessage() : null;
//    }

    static class JobHolder {
        private final ScheduledFuture<?> future;
        JobEntity config;

        public JobHolder(ScheduledFuture<?> future, JobEntity config) {
            this.future = future;
            this.config = config;

        }

        public JobEntity getConfig() {
            return config;
        }

        public void setMessage(JobEntity config) {
            this.config = config;
        }
    }

    private boolean SftpFileCheck(Map<String, String> params) {
        log.debug("Entering method SftpFileCheck");
        String expectedPattern = params.get("file_pattern");
        String expectedType = params.get("file_type");
        String fileCountStr = params.get("file_count");
        String startWindowStr = params.get("start_window");
        String endWindowStr = params.get("end_window");
        int requiredCount;
        try {
            requiredCount = Integer.parseInt(fileCountStr);
        } catch (NumberFormatException e) {
            log.error("Invalid file_count value: '{}'. It must be a number.", fileCountStr);
            log.debug("Exiting method SftpFileCheck");
            return false;
        }
        LocalTime startWindow, endWindow;
        try {
            startWindow = LocalTime.parse(startWindowStr);
            endWindow = LocalTime.parse(endWindowStr);
        } catch (Exception e) {
            log.error("Failed to parse time windows: start='{}', end='{}'. Reason: {}", startWindowStr, endWindowStr, e.getMessage());
            log.debug("Exiting method SftpFileCheck");
            return false;
        }
        LocalTime now = LocalTime.now();
        if (now.isBefore(startWindow) || now.isAfter(endWindow)) {
            log.warn("Current time '{}' is outside configured window ({} - {}). Skipping file check.", now, startWindow, endWindow);
            log.debug("Exiting method SftpFileCheck");
            return false;
        }
        List<String> matchedFiles = new ArrayList<>();
        try (Session<SftpClient.DirEntry> session = sftpSessionFactory.getSession()) {
            SftpClient sftpClient = (SftpClient) session.getClientInstance();
            List<SftpClient.DirEntry> files = fileService.readFiles(sftpClient);
            if (files.isEmpty()) {
                log.warn("No files found in polling directory.");
                log.debug("Exiting method SftpFileCheck");
                return false;
            }
            log.info("{} file(s) found in polling directory. Checking for matches...", files.size());
            Pattern pattern = expectedPattern != null ? Pattern.compile(expectedPattern.replace("*", ".*")) : null;
            for (SftpClient.DirEntry file : files) {
                String fileName = file.getFilename();
                if (fileService.isSkippableFile(fileName)) {
                    log.debug("Skipping system file: '{}'", fileName);
                    continue;
                }
                boolean matchesType = expectedType == null || fileName.endsWith(expectedType);
                boolean matchesPattern = pattern == null || pattern.matcher(fileName).matches();
                String fileType = fileName.substring(fileName.lastIndexOf("."));;
                log.debug("File: '{}' | Type Match: {}/{} | Pattern Match: {}/{}", fileName, expectedType, fileType, expectedPattern, fileName);
                log.debug("File: '{}' | Type Match: {} | Pattern Match: {}", fileName, matchesType, matchesPattern);
                if (matchesType && matchesPattern) {
                    matchedFiles.add(fileName);
                    log.info("Match found: '{}'", fileName);
                }else{
                    try {
                        String erroredDir = sftpMonitorProperties.getErroredDir() +"/"+ fileName;
                        String pollingDir = sftpMonitorProperties.getPollingDir() +"/"+ fileName;
                        fileService.moveFile(sftpClient, pollingDir, erroredDir, "moving non-matching file to ERRORED folder");
                        log.info("File '{}' moved to ERRORED folder at '{}'", fileName, erroredDir);
                    } catch (Exception e) {
                        log.error("Failed to move non-matching file '{}' to ERRORED folder: {}", fileName, e.getMessage(), e);
                    }
                }
                if (matchedFiles.size() >= requiredCount) {
                    log.info("Required file count matched: {}/{}. Moving files to PROCESSING folder...", matchedFiles.size(), requiredCount);
                    for (String entry : matchedFiles) {
                        String src = sftpMonitorProperties.getPollingDir() + "/" + entry;
                        String dst = sftpMonitorProperties.getProcessingDir() + "/" + entry;
                        try {
                            fileService.moveFile(sftpClient, src, dst, "moving for proccesing");
                            log.info("Moved '{}' -> '{}'", src, dst);
                        } catch (Exception ex) {
                            log.error("Failed to move file '{}' to processing dir. Reason: {}", entry, ex.getMessage());
                            log.debug("Exiting method SftpFileCheck");
                            return false;
                        }
                    }
                    log.debug("Exiting method SftpFileCheck");
                    return true;
                }
            }
            log.warn("Matched file count ({}) less than required ({}) within polling window.", matchedFiles.size(), requiredCount);
            log.debug("Exiting method SftpFileCheck");
            return false;
        } catch (Exception e) {
            log.error("SFTP polling failure: {}", e.getMessage(), e);
            log.debug("Exiting method SftpFileCheck");
            return false;
        }
    }


}
