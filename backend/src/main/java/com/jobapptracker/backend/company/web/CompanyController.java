package com.jobapptracker.backend.company.web;

import com.jobapptracker.backend.company.dto.CompanyCreateRequest;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.dto.CompanyUpdateRequest;
import com.jobapptracker.backend.company.service.CompanyService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService service) {
        this.companyService = service;
    }

    @GetMapping
    public ResponseEntity<List<CompanyDto>> listCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String due,
            @RequestParam(required = false) String date
    ) {
        List<CompanyDto> out = companyService.listCompanies(page, size, q, tags, due, date);
        return ResponseEntity.ok(out);
    }

    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(@RequestBody CompanyCreateRequest request) {
        CompanyDto created = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{companyId}")
    public ResponseEntity<CompanyDto> updateCompany(
            @PathVariable UUID companyId,
            @RequestBody CompanyUpdateRequest request
    ) {
        CompanyDto updated = companyService.updateCompany(companyId, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{companyId}/mark-visited-today")
    public ResponseEntity<CompanyDto> markVisitedToday(@PathVariable UUID companyId) {
        CompanyDto updated = companyService.markVisitedToday(companyId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID companyId) {
        companyService.deleteCompany(companyId);
        return ResponseEntity.noContent().build();
    }
}
