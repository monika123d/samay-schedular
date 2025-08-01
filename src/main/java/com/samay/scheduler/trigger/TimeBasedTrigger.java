package com.samay.scheduler.trigger;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

@Slf4j
public class TimeBasedTrigger implements Trigger {
    ////private final LocalTime triggerTime;
    //private final ZoneId zoneId;
    private LocalDate lastTriggerDate;
    private LocalDate currentActivationDate;

    public TimeBasedTrigger() {
//        this.triggerTime = triggerTime;
//        this.zoneId = zoneId;
//        this.currentActivationDate = null;
//
//        LocalDate todayAtInitialization = LocalDate.now(this.zoneId);
//        LocalTime nowAtInitialization = LocalTime.now(this.zoneId);
//
//        if (nowAtInitialization.isAfter(this.triggerTime)) {
//            this.lastTriggerDate = todayAtInitialization;
//            log.info("[Trigger Init] TimeBasedTrigger for {} at {}: Trigger time has passed for today ({}). LastTriggerDate set to today to prevent immediate re-run on restart.",
//                    this.triggerTime, this.zoneId, todayAtInitialization);
//        } else {
//
//            this.lastTriggerDate = todayAtInitialization.minusDays(1);
//            log.info("[Trigger Init] TimeBasedTrigger for {} at {}: Trigger time for today ({}) has not passed. LastTriggerDate set to yesterday to allow run today.",
//                    this.triggerTime, this.zoneId, todayAtInitialization);
//        }
        log.debug("Mocked TimeBaseTrigger");

    }

    @Override
    public boolean isTriggered() {
//        if (this.currentActivationDate != null) {
//            // Already decided to trigger in this cycle, waiting for postExecution
//            return false;
//        }
//
//        LocalDate today = LocalDate.now(zoneId);
//        boolean timeConditionMet = LocalTime.now(zoneId).isAfter(triggerTime);
//
//
//        boolean dateConditionMet = !today.equals(lastTriggerDate);
//
//        if (timeConditionMet && dateConditionMet) {
//            this.currentActivationDate = today;
//            log.trace("[Trigger Check] TimeBasedTrigger for {} at {} on {} met conditions. Activation date set to {}.",
//                    this.triggerTime, this.zoneId, today, this.currentActivationDate);
//            return true;
//        }
        return false;
    }

    @Override
    public void postExecution() {
//        if (this.currentActivationDate != null) {
//            this.lastTriggerDate = this.currentActivationDate;
//            log.info("[Trigger Reset] TimeBasedTrigger for {} at {} successfully executed for {}. Reset for the next eligible day.",
//                    this.triggerTime, this.zoneId, this.lastTriggerDate);
//        } else {
//            this.lastTriggerDate = LocalDate.now(zoneId);
//            log.warn("[Trigger Reset] TimeBasedTrigger for {} at {} reset. Activation date was unexpectedly null. Using current date {}.",
//                    this.triggerTime, this.zoneId, this.lastTriggerDate);
//        }
//        this.currentActivationDate = null; // Reset for the next cycle
    }
}