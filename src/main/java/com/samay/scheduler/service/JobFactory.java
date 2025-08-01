package com.samay.scheduler.service;

import com.samay.scheduler.job.Job;
import com.samay.scheduler.persistence.JobEntity;
import com.samay.scheduler.persistence.TriggerType;
import com.samay.scheduler.trigger.EventBasedTrigger;
import com.samay.scheduler.trigger.FileArrivalTrigger;
import com.samay.scheduler.trigger.TimeBasedTrigger;
import com.samay.scheduler.trigger.Trigger;
import com.samay.scheduler.workflow.Workflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class JobFactory {

    //private final WorkflowRegistry workflowRegistry;
    private final DynamicThreadSchedulerService dynamicThreadSchedulerService;

    public Job createJobFromEntity(JobEntity entity) {
        Objects.requireNonNull(entity, "JobEntity cannot be null");
        try {
            //Workflow workflow = workflowRegistry.getWorkflow(entity.getWorkflowName());
            String workflowName = entity.getWorkflowName();
            Trigger trigger = createTriggerFromEntity(entity);

             return new Job(entity.getId(), entity.getName(), workflowName, trigger);


        } catch (Exception e) {
            log.error("Failed to create job from entity with id {}: {}", entity.getId(), e.getMessage(), e);
            return null;
        }
    }

    private Trigger createTriggerFromEntity(JobEntity entity) {
        Map<String, String> params = entity.getTriggerParameters();
        return switch (entity.getTriggerType()) {
            case TIME_BASED -> {
//                LocalTime time = LocalTime.parse(params.get("triggerTime"));
//                ZoneId zoneId = ZoneId.of(params.get("zoneId"));
//                yield new TimeBasedTrigger(time, zoneId);
                String scheduleType = params.get("scheduleType");
                switch (scheduleType) {
                    case "INTERVAL" -> {
                        dynamicThreadSchedulerService.scheduleThread(entity);
                    }
                    default ->{
                        log.error("Unsupported scheduleType [{}] for TIME_BASED trigger", scheduleType);
                        throw new IllegalArgumentException("Unsupported scheduleType: " + scheduleType);
                    }
                }
                yield new TimeBasedTrigger();
            }
            case EVENT_BASED -> {
                String entityName = params.get("entityName");
                String fieldName = params.get("fieldName");
                String newValue = params.get("newValue");
                yield new EventBasedTrigger(entityName, fieldName, newValue);
            }
            case FILE_ARRIVAL_EVENT -> {
                String trigger_type = params.get("trigger_type");
                String operator = params.get("operator");
                String startWindow = params.get("start_window");
                String endWindow = params.get("end_window");
                String fileType = params.get("file_type");
                String filPattern = params.get("file_pattern");
                String fileCount = params.get("file_count");
                yield new FileArrivalTrigger(operator, startWindow, endWindow, fileType, fileCount, filPattern);
            }
        };
    }
}