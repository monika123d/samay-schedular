package com.samay.scheduler.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode; // Import this
import org.hibernate.type.SqlTypes;           // Import this

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Entity
@Table(name = "jobs")
@Data
public class JobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String workflowName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TriggerType triggerType;

    @Convert(converter = JsonToMapConverter.class)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> triggerParameters;

    private boolean active = true;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "last_triggered_date")
    private LocalDate lastTriggeredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus jobStatus;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

//    @Column(name = "freq_interval")
//    private Long freqInterval;
//    private String scheduleType;
//
//    private Long frequencyValue;
//    private String frequencyUnit;
//
//    private Integer dateOfMonth;
//    @Enumerated(EnumType.STRING)
//    private DayOfWeek dayOfWeek;
//    private LocalTime timeOfDay;
//
//    private String message;
//
//    @Column(name = "is_locked")
//    private boolean isLocked;
}