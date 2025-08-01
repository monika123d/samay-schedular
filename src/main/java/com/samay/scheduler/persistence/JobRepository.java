package com.samay.scheduler.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<JobEntity, Long> {
    List<JobEntity> findByActiveTrue();
    List<JobEntity> findByTriggerType(TriggerType triggerType);
    //List<JobEntity> findById(Long id);

    List<JobEntity> findByTriggerTypeAndActiveTrue(TriggerType triggerType);
}