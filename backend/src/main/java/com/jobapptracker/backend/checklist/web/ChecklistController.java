package com.jobapptracker.backend.checklist.web;

import com.jobapptracker.backend.checklist.dto.ChecklistCompanyDto;
import com.jobapptracker.backend.checklist.dto.ChecklistUpdateRequest;
import com.jobapptracker.backend.checklist.service.ChecklistService;
import com.jobapptracker.backend.company.dto.CompanyDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/checklist")
public class ChecklistController {

    private final ChecklistService checklistService;

    public ChecklistController(ChecklistService service) {
        this.checklistService = service;
    }

    @GetMapping
    public ResponseEntity<List<ChecklistCompanyDto>> getChecklist(@RequestParam(required = false) String date) {
        LocalDate d = (date == null || date.isBlank()) ? LocalDate.now() : parseDate(date);
        return ResponseEntity.ok(checklistService.getChecklist(d));
    }

    @PutMapping("/{date}/companies/{companyId}")
    public ResponseEntity<Void> setCompleted(
            @PathVariable String date,
            @PathVariable UUID companyId,
            @RequestBody ChecklistUpdateRequest request
            ) {
        checklistService.setCompleted(parseDate(date), companyId, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{date}/submit")
    public ResponseEntity<List<CompanyDto>> submitDay(
            @PathVariable String date
    ) {
        LocalDate d = parseDate(date);
        var updatedCompanies = checklistService.submitDay(d);
        return ResponseEntity.ok(updatedCompanies);
    }

    private static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + raw + " (expected YYYY-MM-DD)");
        }
    }
}
