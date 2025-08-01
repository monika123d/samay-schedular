package com.samay.scheduler.condition;

import com.samay.scheduler.model.ConditionConfig;
import com.samay.scheduler.model.ConditionDetail;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ConditionEvaluator {

    public static boolean evaluateConditions(ConditionConfig config, String filename, LocalTime fileArrivalTime) {
        List<Boolean> results = new ArrayList<>();

        for (ConditionDetail condition : config.getConditions()) {
            switch (condition.getType()) {
                case FILE_EXTENSION:
                    boolean hasExtension = filename.endsWith(condition.getValue());
                    log.info("[Condition Check] FILE_EXTENSION '{}' match result: {}", condition.getValue(), hasExtension);
                    results.add(hasExtension);
                    break;

                case TIME_WINDOW:
                    LocalTime start = LocalTime.parse(condition.getStartTime());
                    LocalTime end = LocalTime.parse(condition.getEndTime());
                    boolean inWindow = !fileArrivalTime.isBefore(start) && !fileArrivalTime.isAfter(end);
                    log.info("[Condition Check] TIME_WINDOW from {} to {} result: {}", start, end, inWindow);
                    results.add(inWindow);
                    break;

                case FILE_COUNT:

            }
        }

        boolean finalResult = "AND".equalsIgnoreCase(config.getOperator())
                ? results.stream().allMatch(Boolean::booleanValue)
                : results.stream().anyMatch(Boolean::booleanValue);

        log.info("[Condition Evaluation] Operator: {}, Final Result: {}", config.getOperator(), finalResult);
        return finalResult;
    }
}

