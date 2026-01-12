package com.jobapptracker.backend.checklist.scheduler;

import com.jobapptracker.backend.checklist.service.ChecklistService;
import com.jobapptracker.backend.company.dto.CompanyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class ChecklistAutoSubmitJob {

    private static final Logger log = LoggerFactory.getLogger(ChecklistAutoSubmitJob.class);

    private final ChecklistService checklistService;

    public ChecklistAutoSubmitJob(ChecklistService service) {
        this.checklistService = service;
    }

    @Scheduled(cron = "0 59 23 * * *")
    public void autoSubmit() {
        LocalDate today = LocalDate.now();
        log.info("Starting checklist auto-submit job for date={}", today);
        try {
            List<CompanyDto> updated = checklistService.submitDay(today);
            log.info("Checklist auto-submit completed successfully: updated {} companies", updated.size());
        } catch (Exception e) {
            log.error("Checklist auto-submit failed for date={}", today, e);
            throw e;
        }
    }
}
