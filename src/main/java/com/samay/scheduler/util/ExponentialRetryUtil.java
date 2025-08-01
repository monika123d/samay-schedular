package com.samay.scheduler.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.function.Supplier;


public class ExponentialRetryUtil {

    private static final Logger log = LoggerFactory.getLogger(ExponentialRetryUtil.class);

    /**
     * Repeatedly attempts the given task using exponential backoff strategy until it succeeds,
     * the retry time window ends, or the max retry limit is reached.
     *
     * @param task         The task to be executed. Returns true if successful.
     * @param windowEnd    The time until which retries should be attempted.
     * @param initialDelay Initial delay in milliseconds.
     * @param maxDelay     Maximum delay between retries in milliseconds.
     * @param maxRetries   Maximum number of retry attempts.
     * @return true if the task succeeded within the window, false otherwise.
     */
    public static boolean retryWithExponentialBackoff(Supplier<Boolean> task, Instant windowEnd, long initialDelay, long maxDelay, int maxRetries) {
        int retryCount = 0;
        long delay = initialDelay;
        while (Instant.now().isBefore(windowEnd) && retryCount < maxRetries) {
            try {
                if (task.get()) {
                    log.info("Task succeeded on attempt #{}", retryCount + 1);
                    return true;
                }else {
                    log.info("Attempt #{} failed, retrying after {} ms...", retryCount + 1, delay);
                }
            } catch (Exception ex) {
                log.warn("Exception during task execution on attempt #{}: {}", retryCount + 1, ex.getMessage(), ex);
            }

            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error(" Retry interrupted. Exiting retry loop.");
                return false;
            }
            delay = Math.min(delay * 2, maxDelay);
            retryCount++;
        }

        log.warn("Retry stopped. Time window ended or max retries reached.");
        return false;
    }
}
