package com.jobapptracker.backend.company.service;

import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.dto.CompanyCreateRequest;
import com.jobapptracker.backend.company.dto.CompanyUpdateRequest;
import com.jobapptracker.backend.company.repository.CompanyRepository;
import com.jobapptracker.backend.company.web.DueFilter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Stream;
import java.util.UUID;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository repository) {
        this.companyRepository = repository;
    }

    public List<CompanyDto> listCompanies(
            Integer page,
            Integer size,
            String q,
            String tagsCsv,
            String dueRaw,
            String dateRaw
    ) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;

        if (p < 0) {
            throw new IllegalStateException("page must be >= 0");
        }
        if (s <= 0) {
            throw new IllegalStateException("size must be > 0");
        }

        // simple guardrail
        if (s > 200) {
            s = 200;
        }

        List<String> tags = parseTags(tagsCsv);
        DueFilter due = DueFilter.fromStringOrNull(dueRaw);
        LocalDate date = parseDateOrNull(dateRaw);

        return companyRepository.findCompanies(p, s, q, tags, due, date);
    }

    private static List<String> parseTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return List.of();
        }

        return Stream.of(tagsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    private static LocalDate parseDateOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(raw.trim());
        } catch(DateTimeParseException e) {
            throw new IllegalStateException("Invalid date: " + raw + " expected YYYY-MM-DD)");
        }
    }

    public CompanyDto createCompany(CompanyCreateRequest request) {
        if (request == null) {
            throw new IllegalStateException("Request body is required");
        }

        String name = (request.companyName() == null) ? null : request.companyName().trim();
        String url = (request.careersUrl() == null) ? null : request.careersUrl().trim();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("companyName is required");
        }

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("careersUrl is required");
        }

        int revisit = (request.revisitAfterDays() == null) ? 7 : request.revisitAfterDays();
        if (revisit <= 0) {
            throw new IllegalArgumentException("revisitAfterDays must be > 0");
        }

        var tags = (request.tags() == null) ? java.util.List.<String>of() : request.tags();

        try {
            return companyRepository.insertCompany(name, url, request.lastVisitedOn(), revisit, tags);
        } catch (DuplicateKeyException e) {
            throw new DuplicateCareersUrlException(url);
        }
    }

    public CompanyDto updateCompany(UUID companyId, CompanyUpdateRequest request) {
        if (companyId == null) {
            throw new BadUpdateRequestException("companyId is required");
        }

        if (request == null) {
            throw new BadUpdateRequestException("RequestBode is required");
        }

        String companyName = (request.companyName() == null) ? null : request.companyName().trim();
        String careersUrl = (request.careersUrl() == null) ? null : request.careersUrl().trim();

        if (request.companyName() != null && companyName.isBlank()) {
            throw new BadUpdateRequestException("companyName must not be blank");
        }

        if (request.careersUrl() != null && careersUrl.isBlank()) {
            throw new BadUpdateRequestException("CareersUrl must not be blank");
        }

        Integer revisit = request.revisitAfterDays();
        if (revisit != null && revisit <= 0) {
            throw new BadUpdateRequestException("revisitAfterDays must be > 0");
        }

        List<String> tags = null;
        if (request.tags() != null) {
            tags = request.tags().stream()
                    .filter(s -> s != null)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }

        boolean anyFieldProvided =
                request.companyName() != null ||
                request.careersUrl() != null ||
                request.lastVisitedOn() != null ||
                request.revisitAfterDays() != null ||
                request.tags() != null;

        if (!anyFieldProvided) {
            throw new BadUpdateRequestException("No fields provided to update");
        }

        try {
            CompanyDto updated = companyRepository.updateCompany(
                    companyId,
                    companyName,
                    careersUrl,
                    request.lastVisitedOn(),
                    revisit,
                    tags
            );

            if (updated == null) {
                throw new CompanyNotFoundException();
            }

            return updated;
        } catch (DuplicateKeyException e) {
            throw new DuplicateCareersUrlException(careersUrl);
        } catch (DataIntegrityViolationException r) {
            throw new BadUpdateRequestException("Invalid update (constraint violation)");
        }
    }

    public CompanyDto markVisitedToday(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyID is required");
        }

        CompanyDto updated = companyRepository.markVisitedToday(companyId);
        if (updated == null) {
            throw new CompanyNotFoundException();
        }

        return updated;
    }
}
