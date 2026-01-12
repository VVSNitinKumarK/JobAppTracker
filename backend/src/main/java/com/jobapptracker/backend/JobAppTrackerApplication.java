package com.jobapptracker.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobAppTrackerApplication {

    private static final Logger log = LoggerFactory.getLogger(JobAppTrackerApplication.class);

    public static void main(String[] args) {
        log.info("Starting Job Application Tracker backend application");
        SpringApplication.run(JobAppTrackerApplication.class, args);
        log.info("Job Application Tracker backend application started successfully");
    }
}
