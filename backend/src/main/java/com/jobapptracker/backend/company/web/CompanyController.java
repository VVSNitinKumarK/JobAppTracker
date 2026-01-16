package com.jobapptracker.backend.company.web;

import com.jobapptracker.backend.company.dto.BatchDeleteRequest;
import com.jobapptracker.backend.company.dto.BatchDeleteResponse;
import com.jobapptracker.backend.company.dto.CompanyCreateRequest;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.dto.CompanyUpdateRequest;
import com.jobapptracker.backend.company.dto.PagedCompaniesResponse;
import com.jobapptracker.backend.company.service.CompanyService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyService companyService;

    public CompanyController(CompanyService service) {
        this.companyService = service;
    }

    @GetMapping
    public ResponseEntity<PagedCompaniesResponse> listCompanies(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String due,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String lastVisitedOn
    ) {
        log.info("GET /api/companies");
        log.debug("Listing companies: page={}, size={}, q={}, tags={}, due={}, date={}, lastVisitedOn={}",
                page, size, q, tags, due, date, lastVisitedOn);
        // Defaults are handled in service layer using PaginationConstants
        PagedCompaniesResponse response = companyService.listCompanies(page, size, q, tags, due, date, lastVisitedOn);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CompanyDto> createCompany(@Valid @RequestBody CompanyCreateRequest request) {
        log.info("POST /api/companies");
        log.debug("Creating company: name={}, url={}", request.companyName(), request.careersUrl());
        CompanyDto created = companyService.createCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{companyId}")
    public ResponseEntity<CompanyDto> updateCompany(
            @PathVariable UUID companyId,
            @Valid @RequestBody CompanyUpdateRequest request
    ) {
        log.info("PATCH /api/companies/{} - updating company", companyId);
        CompanyDto updated = companyService.updateCompany(companyId, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{companyId}")
    public ResponseEntity<Void> deleteCompany(@PathVariable UUID companyId) {
        log.info("DELETE /api/companies/{} - deleting company", companyId);
        companyService.deleteCompany(companyId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/batch")
    public ResponseEntity<BatchDeleteResponse> deleteCompanies(@Valid @RequestBody BatchDeleteRequest request) {
        log.info("DELETE /api/companies/batch - deleting {} companies", request.companyIds().size());
        int deleted = companyService.deleteCompanies(request.companyIds());
        return ResponseEntity.ok(new BatchDeleteResponse(deleted, request.companyIds().size()));
    }
}
