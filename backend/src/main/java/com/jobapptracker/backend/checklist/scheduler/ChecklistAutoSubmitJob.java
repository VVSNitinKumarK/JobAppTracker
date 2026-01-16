package com.jobapptracker.backend.checklist.scheduler;

import com.jobapptracker.backend.checklist.service.ChecklistService;
import com.jobapptracker.backend.company.dto.CompanyDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class ChecklistAutoSubmitJob {

    private static final Logger log = LoggerFactory.getLogger(ChecklistAutoSubmitJob.class);

    private final ChecklistService checklistService;
    private final ZoneId schedulerZone;

    public ChecklistAutoSubmitJob(
            ChecklistService service,
            @Value("${scheduler.timezone}") String timezone
    ) {
        this.checklistService = service;
        this.schedulerZone = ZoneId.of(timezone);
    }

    @Scheduled(cron = "0 59 23 * * *", zone = "${scheduler.timezone}")
    public void autoSubmit() {
        LocalDate today = LocalDate.now(schedulerZone);
        log.info("Starting checklist auto-submit job for date={} (zone={})", today, schedulerZone);
        try {
            List<CompanyDto> updated = checklistService.submitDay(today);
            log.info("Checklist auto-submit completed successfully: updated {} companies", updated.size());
        } catch (Exception exception) {
            log.error("Checklist auto-submit failed for date={}", today, exception);
        }
    }
}
