package com.jobapptracker.backend.checklist.scheduler;

import com.jobapptracker.backend.checklist.service.ChecklistService;
import com.jobapptracker.backend.company.dto.CompanyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class ChecklistCatchupOnStartup {

    private static final Logger log = LoggerFactory.getLogger(ChecklistCatchupOnStartup.class);

    @Bean
    ApplicationRunner catchupRunner(ChecklistService checklistService) {
        return args -> {
            log.info("Starting checklist catchup on application startup");
            try {
                LocalDate yesterday = LocalDate.now().minusDays(1);
                List<CompanyDto> updated = checklistService.submitDay(yesterday);
                log.info("Checklist catchup completed successfully: updated {} companies for date={}",
                        updated.size(), yesterday);
            } catch (Exception e) {
                log.error("Checklist catchup failed", e);
                throw e;
            }
        };
    }
}
