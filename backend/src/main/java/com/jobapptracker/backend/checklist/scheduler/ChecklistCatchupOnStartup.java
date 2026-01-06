package com.jobapptracker.backend.checklist.scheduler;

import com.jobapptracker.backend.checklist.service.ChecklistService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
public class ChecklistCatchupOnStartup {

    @Bean
    ApplicationRunner catchupRunner(ChecklistService checklistService) {
        return args -> {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            checklistService.submitDay(yesterday);
        };
    }
}
