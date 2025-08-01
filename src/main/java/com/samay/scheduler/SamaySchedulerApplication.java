package com.samay.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.sql.Connection;
import java.sql.DriverManager;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class SamaySchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SamaySchedulerApplication.class, args);
		log.info("SAMAY Scheduler with API and DB Persistence is now running.");
	}

}