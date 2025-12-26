package com.jobapptracker.backend.checklist.service;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.checklist.dto.ChecklistUpdateRequest;
import com.jobapptracker.backend.checklist.repository.ChecklistRepository;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.service.CompanyNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ChecklistService {

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
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }

        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }

        if (checklistUpdateRequest == null || checklistUpdateRequest.completed() == null) {
            throw new IllegalArgumentException("completed is required");
        }

        if (!checklistRepository.companyExists(companyId)) {
            throw new CompanyNotFoundException();
        }

        checklistRepository.upsertCompletion(date, companyId, checklistUpdateRequest.completed());
    }

    public List<CompanyDto> submitDay(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }

        return checklistRepository.submitDay(date);
    }
}
