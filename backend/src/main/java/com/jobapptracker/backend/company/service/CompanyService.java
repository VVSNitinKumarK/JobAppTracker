package com.jobapptracker.backend.company.service;

import com.jobapptracker.backend.company.CompanyConstants;
import com.jobapptracker.backend.company.dto.CompanyCreateRequest;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.dto.CompanyUpdateRequest;
import com.jobapptracker.backend.company.dto.PagedCompaniesResponse;
import com.jobapptracker.backend.company.repository.CompanyRepository;
import com.jobapptracker.backend.company.repository.CompanyTagUtil;
import com.jobapptracker.backend.company.web.DueFilter;
import com.jobapptracker.backend.config.PaginationConstants;
import com.jobapptracker.backend.tag.dto.TagDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository repository) {
        this.companyRepository = repository;
    }

    public PagedCompaniesResponse listCompanies(
            Integer page,
            Integer size,
            String q,
            String tagsCsv,
            String dueRaw,
            String dateRaw,
            String lastVisitedOnRaw
    ) {
        int p = (page == null) ? PaginationConstants.DEFAULT_PAGE : page;
        int s = (size == null) ? PaginationConstants.DEFAULT_PAGE_SIZE : size;

        if (p < 0) {
            throw new IllegalStateException("page must be >= 0, but got: " + p);
        }
        // Prevent integer overflow when calculating offset = page * size
        if (p > PaginationConstants.MAX_PAGE_NUMBER) {
            throw new IllegalStateException("page must be <= " + PaginationConstants.MAX_PAGE_NUMBER + " (too many records), but got: " + p);
        }
        if (s <= 0) {
            throw new IllegalStateException("size must be > 0, but got: " + s);
        }

        if (s > PaginationConstants.MAX_PAGE_SIZE) {
            s = PaginationConstants.MAX_PAGE_SIZE;
        }

        List<String> tags = parseTags(tagsCsv);
        DueFilter due = DueFilter.fromStringOrNull(dueRaw);
        LocalDate date = parseDateOrNull(dateRaw);
        LocalDate lastVisitedOn = parseDateOrNull(lastVisitedOnRaw);

        List<CompanyDto> items = companyRepository.findCompanies(p, s, q, tags, due, date, lastVisitedOn);
        long total = companyRepository.countCompanies(q, tags, due, date, lastVisitedOn);

        return new PagedCompaniesResponse(items, p, s, total);
    }

    private static List<String> parseTags(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return List.of();
        }

        return Stream.of(tagsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(CompanyTagUtil::toTagKey)
                .distinct()
                .toList();
    }

    private static LocalDate parseDateOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("Invalid date: " + raw + " expected YYYY-MM-DD)");
        }
    }

    /**
     * Normalizes tags coming from request payload into display names.
     * Supports:
     *  - List<String> (display names)
     *  - List<TagDto> (tagName preferred; fallback to tagKey)
     */
    private static List<String> normalizeTagDisplayNames(List<?> rawTags) {
        if (rawTags == null || rawTags.isEmpty()) {
            return List.of();
        }

        Object first = rawTags.getFirst();

        // Already a List<String>
        if (first instanceof String) {
            @SuppressWarnings("unchecked")
            List<String> tags = (List<String>) rawTags;

            return tags.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }

        // List<TagDto>
        if (first instanceof TagDto) {
            @SuppressWarnings("unchecked")
            List<TagDto> tags = (List<TagDto>) rawTags;

            return tags.stream()
                    .filter(Objects::nonNull)
                    .map(t -> {
                        String name = t.tagName();
                        String key = t.tagKey();

                        // Prefer tagName (display name), fallback to tagKey if name is blank
                        String candidate = (name != null && !name.isBlank()) ? name : key;
                        return (candidate == null) ? "" : candidate.trim();
                    })
                    .filter(s -> !s.isBlank())
                    .distinct()
                    .toList();
        }

        throw new IllegalStateException("Unsupported tags payload type: " + first.getClass().getName());
    }

    @Transactional
    public CompanyDto createCompany(CompanyCreateRequest request) {
        // Validation is handled by @Valid annotation in controller + JSR-303 annotations on DTO
        String name = request.companyName().trim();
        String url = request.careersUrl().trim();
        int revisit = (request.revisitAfterDays() == null) ? CompanyConstants.DEFAULT_REVISIT_AFTER_DAYS : request.revisitAfterDays();

        log.info("Creating company: name={}, url={}, revisitAfterDays={}",
                name, url, revisit);

        List<String> tagNames = normalizeTagDisplayNames(request.tags());

        try {
            CompanyDto created = companyRepository.insertCompany(name, url, request.lastVisitedOn(), revisit, tagNames);
            log.info("Company created successfully: id={}, name={}", created.companyId(), created.companyName());
            return created;
        } catch (DuplicateKeyException e) {
            throw new DuplicateCareersUrlException(url);
        }
    }

    @Transactional
    public CompanyDto updateCompany(UUID companyId, CompanyUpdateRequest request) {
        // @Valid annotation handles: null request, revisitAfterDays > 0
        // Manual validation still needed for: blank strings (if provided), at least one field

        log.info("Updating company: id={}", companyId);

        String companyName = (request.companyName() == null) ? null : request.companyName().trim();
        String careersUrl = (request.careersUrl() == null) ? null : request.careersUrl().trim();

        if (request.companyName() != null && companyName.isBlank()) {
            throw new BadUpdateRequestException("companyName must not be blank", companyId, "companyName");
        }

        if (request.careersUrl() != null && careersUrl.isBlank()) {
            throw new BadUpdateRequestException("careersUrl must not be blank", companyId, "careersUrl");
        }

        List<String> tagNames = null;
        if (request.tags() != null) {
            tagNames = normalizeTagDisplayNames(request.tags());
        }

        boolean anyFieldProvided =
                request.companyName() != null ||
                        request.careersUrl() != null ||
                        request.lastVisitedOn() != null ||
                        request.revisitAfterDays() != null ||
                        request.tags() != null;

        if (!anyFieldProvided) {
            throw new BadUpdateRequestException("No fields provided to update", companyId, null);
        }

        Integer revisit = request.revisitAfterDays();

        try {
            CompanyDto updated = companyRepository.updateCompany(
                    companyId,
                    companyName,
                    careersUrl,
                    request.lastVisitedOn(),
                    revisit,
                    tagNames
            );

            if (updated == null) {
                throw new CompanyNotFoundException(companyId);
            }

            log.info("Company updated successfully: id={}, name={}", updated.companyId(), updated.companyName());
            return updated;
        } catch (DuplicateKeyException e) {
            throw new DuplicateCareersUrlException(careersUrl);
        } catch (DataIntegrityViolationException e) {
            throw new BadUpdateRequestException("Invalid update (constraint violation): " + e.getMessage(), companyId, null);
        }
    }

    public void deleteCompany(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }

        log.info("Deleting company: id={}", companyId);
        int deleted = companyRepository.deleteCompany(companyId);
        if (deleted == 0) {
            throw new CompanyNotFoundException(companyId);
        }
        log.info("Company deleted successfully: id={}", companyId);
    }

    @Transactional
    public int deleteCompanies(List<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            throw new IllegalArgumentException("companyIds list cannot be null or empty");
        }

        log.info("Batch deleting {} companies", companyIds.size());
        int deleted = companyRepository.deleteCompanies(companyIds);
        log.info("Batch delete completed: {} out of {} companies deleted", deleted, companyIds.size());
        return deleted;
    }
}
