package com.jobapptracker.backend.checklist.scheduler;

import com.jobapptracker.backend.checklist.service.ChecklistService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ChecklistAutoSubmitJob {

    private final ChecklistService checklistService;

    public ChecklistAutoSubmitJob(ChecklistService service) {
        this.checklistService = service;
    }

    @Scheduled(cron = "0 59 23 * * *")
    public void autoSubmit() {
        LocalDate today = LocalDate.now();
        checklistService.submitDay(today);
    }
}
