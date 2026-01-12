package com.jobapptracker.backend.checklist.service;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.checklist.dto.ChecklistUpdateRequest;
import com.jobapptracker.backend.checklist.repository.ChecklistRepository;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.service.CompanyNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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

        return checklistRepository.getCheckList(date);
    }

    public void setCompleted(LocalDate date, UUID companyId, ChecklistUpdateRequest checklistUpdateRequest) {
        log.info("Setting completion status for company: companyId={}, date={}, completed={}",
                companyId, date, checklistUpdateRequest.completed());

        if (!checklistRepository.companyExists(companyId)) {
            throw new CompanyNotFoundException();
        }

        checklistRepository.upsertCompletion(date, companyId, checklistUpdateRequest.completed());
        log.info("Completion status updated successfully: companyId={}, date={}", companyId, date);
    }

    public List<CompanyDto> submitDay(LocalDate date) {
        log.info("Submitting checklist for date={}", date);
        List<CompanyDto> updated = checklistRepository.submitDay(date);
        log.info("Checklist submitted successfully: date={}, updated {} companies", date, updated.size());
        return updated;
    }
}
