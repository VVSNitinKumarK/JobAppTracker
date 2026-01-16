package com.jobapptracker.backend.checklist.service;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.checklist.dto.ChecklistUpdateRequest;
import com.jobapptracker.backend.checklist.repository.ChecklistRepository;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.service.CompanyNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ChecklistService {

    private static final Logger log = LoggerFactory.getLogger(ChecklistService.class);

    private final ChecklistRepository checklistRepository;

    public ChecklistService(ChecklistRepository repository) {
        this.checklistRepository = repository;
    }

    public List<ChecklistCompanyDto> getChecklist(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        return checklistRepository.getChecklist(date);
    }

    @Transactional
    public void setCompleted(LocalDate date, UUID companyId, ChecklistUpdateRequest checklistUpdateRequest) {
        log.info("Setting completion status for company: companyId={}, date={}, completed={}",
                companyId, date, checklistUpdateRequest.completed());

        boolean success = checklistRepository.upsertCompletion(date, companyId, checklistUpdateRequest.completed());
        if (!success) {
            throw new CompanyNotFoundException(companyId);
        }

        log.info("Completion status updated successfully: companyId={}, date={}", companyId, date);
    }

    @Transactional
    public List<CompanyDto> submitDay(LocalDate date) {
        log.info("Submitting checklist for date={}", date);
        List<CompanyDto> updated = checklistRepository.submitDay(date);
        log.info("Checklist submitted successfully: date={}, updated {} companies", date, updated.size());
        return updated;
    }

    @Transactional
    public boolean removeFromChecklist(LocalDate date, UUID companyId) {
        log.info("Removing company from checklist: companyId={}, date={}", companyId, date);
        boolean removed = checklistRepository.deleteChecklistEntry(date, companyId);
        if (removed) {
            log.info("Company removed from checklist: companyId={}, date={}", companyId, date);
        } else {
            log.debug("No checklist entry found to remove: companyId={}, date={}", companyId, date);
        }
        return removed;
    }
}
