package com.samay.scheduler.trigger;

import com.samay.scheduler.event.DataChangeEvent;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.JobRepository;
import com.samay.scheduler.persistence.JobStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalTime;

@Slf4j
public class FileArrivalTrigger implements Trigger {

    private final String operator;
    private final String startWindow;
    private final String endWindow;
    private final String fileType;
    private final String fileCount;
    private final String filePattern;


    public FileArrivalTrigger(String operator, String startWindow, String endWindow, String fileType, String fileCount, String filePattern) {
        this.operator = operator;
        this.startWindow = startWindow;
        this.endWindow = endWindow;
        this.fileType = fileType;
        this.fileCount = fileCount;
        this.filePattern = filePattern;

        log.debug("""
            [Trigger Parameters Loaded]
            - Logical Operator     : {}
            - File Type Expected   : {}
            - Start Time Window    : {}
            - End Time Window      : {}
            - Expected File Count  : {}
            """, operator, fileType, startWindow, endWindow, fileCount);
    }

    public boolean evaluateConditions(String filename, LocalTime arrivalTime, int actualCount) {
        boolean fileTypeMatches = filename.endsWith(this.fileType);
        LocalTime start = LocalTime.parse(this.startWindow);
        LocalTime end = LocalTime.parse(this.endWindow);
        boolean timeMatches = !arrivalTime.isBefore(start) && !arrivalTime.isAfter(end);

        long startTime = System.currentTimeMillis();

        int expectedCount = 0;
        if (this.fileCount != null && !this.fileCount.isBlank()) {
            expectedCount = Integer.parseInt(this.fileCount);
        }

        boolean fileCountMatches = actualCount == expectedCount;

        log.debug("""
            [Evaluating Conditions]
            - File Name: {}
            - Arrival Time: {}
            - File Type Match (Expected: {}, Actual: {}): {}
            - Time Window Match (Expected: {} to {}, Actual:{}): {}
            - File Count Match (Expected: {}, Actual: {}): {}
            """, filename, arrivalTime,
                this.fileType, filename.substring(filename.lastIndexOf('.')), fileTypeMatches,
                startWindow, endWindow, arrivalTime, timeMatches,
                expectedCount, actualCount, fileCountMatches);

        boolean finalResult;

        switch (this.operator.toUpperCase()) {
            case "AND" -> {
                finalResult = fileTypeMatches && timeMatches && fileCountMatches;
                if (!finalResult) {
                    log.warn("[Trigger Not Matched - AND]: One or more conditions failed:");
                    if (!fileTypeMatches) log.warn("- File type '{}' did not match expected '{}'", filename, fileType);
                    if (!timeMatches) log.warn("- File arrival time '{}' not in range {} to {}", arrivalTime, startWindow, endWindow);
                    if (!fileCountMatches) log.warn("- File count mismatch (Expected: {}, Actual: {})", expectedCount, actualCount);
                }
            }
            case "OR" -> {
                finalResult = fileTypeMatches || timeMatches || fileCountMatches;
                if (!finalResult) {
                    log.warn("[Trigger Not Matched - OR]: None of the conditions were satisfied");
                }
            }
            default -> {
                log.error("[Trigger Error] Unsupported logical operator: '{}'", this.operator);
                finalResult = false;
            }
        }

        if (finalResult) {
            log.debug("[Trigger Matched] All required conditions satisfied. Proceeding with workflow...");
        }

        long endTime = System.currentTimeMillis();
        log.debug("[Performance] Trigger evaluation took {} ms", (endTime - startTime));

        return finalResult;
    }

    @Override
    public boolean isTriggered() {
        return false; // Not used in this case
    }

    @Override
    public void postExecution() {
        log.debug("[Trigger Reset] FileArrivalTrigger has completed its execution cycle.");
    }
}
