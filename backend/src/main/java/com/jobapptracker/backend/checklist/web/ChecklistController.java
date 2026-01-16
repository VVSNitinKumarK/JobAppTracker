package com.jobapptracker.backend.checklist.web;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.checklist.dto.ChecklistUpdateRequest;
import com.jobapptracker.backend.checklist.service.ChecklistService;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.config.DateUtils;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/checklist")
public class ChecklistController {

    private static final Logger log = LoggerFactory.getLogger(ChecklistController.class);

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService service) {
        this.checklistService = service;
    }

    @GetMapping
    public ResponseEntity<List<ChecklistCompanyDto>> getChecklist(@RequestParam(required = false) String date) {
        log.info("GET /api/checklist - date={}", date);
        LocalDate parsedDate = DateUtils.parseDateOrNull(date);
        LocalDate effectiveDate = (parsedDate != null) ? parsedDate : LocalDate.now();
        return ResponseEntity.ok(checklistService.getChecklist(effectiveDate));
    }

    @PutMapping("/{date}/companies/{companyId}")
    public ResponseEntity<Void> setCompleted(
            @PathVariable String date,
            @PathVariable UUID companyId,
            @Valid @RequestBody ChecklistUpdateRequest request
            ) {
        log.info("PUT /api/checklist/{}/companies/{} - completed={}",
                date, companyId, request.completed());
        checklistService.setCompleted(DateUtils.parseDate(date), companyId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{date}/submit")
    public ResponseEntity<List<CompanyDto>> submitDay(
            @PathVariable String date
    ) {
        log.info("POST /api/checklist/{}/submit - submitting checklist for date", date);
        LocalDate parsedDate = DateUtils.parseDate(date);
        var updatedCompanies = checklistService.submitDay(parsedDate);
        return ResponseEntity.ok(updatedCompanies);
    }

    @DeleteMapping("/{date}/companies/{companyId}")
    public ResponseEntity<Void> removeFromChecklist(
            @PathVariable String date,
            @PathVariable UUID companyId
    ) {
        log.info("DELETE /api/checklist/{}/companies/{}", date, companyId);
        LocalDate parsedDate = DateUtils.parseDate(date);
        boolean removed = checklistService.removeFromChecklist(parsedDate, companyId);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }
}
