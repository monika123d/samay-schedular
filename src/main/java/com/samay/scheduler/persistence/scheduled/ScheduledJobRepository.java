package com.samay.scheduler.persistence.scheduled;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, Long> {
}
