package com.samay.scheduler.persistence.thread;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduledThreadConfigRepository extends JpaRepository<ScheduledThreadConfig, String> {
    //List<ScheduledThreadConfig> findByActiveTrue();
}
