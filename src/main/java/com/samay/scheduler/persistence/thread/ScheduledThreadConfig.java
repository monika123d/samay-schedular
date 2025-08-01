package com.samay.scheduler.persistence.thread;

import com.samay.scheduler.persistence.JsonToMapConverter;
import com.samay.scheduler.persistence.TriggerType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Map;

@Entity
@Table(name = "scheduled_thread_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class ScheduledThreadConfig {

    @Id
    private String jobName; // primary key

    @Column(nullable = false)
    private String workflowName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerType triggerType;

    @Convert(converter = JsonToMapConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> triggerParameters;

    private String scheduleType;

    private long frequencyValue;
    private String frequencyUnit;

    private Integer dateOfMonth;
    private DayOfWeek dayOfWeek;
    private LocalTime timeOfDay;

    private String message;

    @Column(name = "is_locked")
    private boolean isLocked;
}
