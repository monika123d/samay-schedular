package com.samay.scheduler.persistence.scheduled;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "samay_scheduled_jobs")
public class ScheduledJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long jobId;

    private LocalDateTime scheduledTime;

    private String triggerType; // e.g. "RUN_NOW", "SFTP", "TIME_BASED"
    private String status;      // "PENDING", "TRIGGERED", "FAILED"
    private String remarks;

    private LocalDateTime createdAt = LocalDateTime.now();
}
