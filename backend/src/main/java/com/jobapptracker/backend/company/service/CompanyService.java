package com.jobapptracker.backend.company.service;

import com.jobapptracker.backend.company.dto.CompanyCreateRequest;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.dto.CompanyUpdateRequest;
import com.jobapptracker.backend.company.repository.CompanyRepository;
import com.jobapptracker.backend.company.repository.CompanyTagUtil;
import com.jobapptracker.backend.company.web.DueFilter;
import com.jobapptracker.backend.tag.dto.TagDto;
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
            String dateRaw,
            String lastVisitedOnRaw
    ) {
        int p = (page == null) ? 0 : page;
        int s = (size == null) ? 10 : size;

        if (p < 0) {
            throw new IllegalStateException("page must be >= 0");
        }
        if (s <= 0) {
            throw new IllegalStateException("size must be > 0");
        }

        if (s > 200) {
            s = 200;
        }

        List<String> tags = parseTags(tagsCsv);
        DueFilter due = DueFilter.fromStringOrNull(dueRaw);
        LocalDate date = parseDateOrNull(dateRaw);
        LocalDate lastVisitedOn = parseDateOrNull(lastVisitedOnRaw);

        return companyRepository.findCompanies(p, s, q, tags, due, date, lastVisitedOn);
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
                        // If TagDto is a record, these accessors will exist if you defined them.
                        // If TagDto is a normal class with getters, replace with getTagName/getTagKey.
                        String name = null;
                        String key = null;

                        try {
                            // record-style accessors
                            name = t.tagName();
                            key = t.tagKey();
                        } catch (Throwable ignored) {
                            // fallback to alternate field names if you modeled TagDto differently
                        }

                        // if your TagDto also has name/key variants, keep these as fallback
                        if (name == null) {
                            try {
                                name = t.tagName();
                            } catch (Throwable ignored) {}
                        }
                        if (key == null) {
                            try {
                                key = t.tagKey();
                            } catch (Throwable ignored) {}
                        }

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

        // ✅ FIX: normalize whatever request.tags() is into List<String>
        List<String> tagNames = normalizeTagDisplayNames(request.tags());

        try {
            return companyRepository.insertCompany(name, url, request.lastVisitedOn(), revisit, tagNames);
        } catch (DuplicateKeyException e) {
            throw new DuplicateCareersUrlException(url);
        }
    }

    @Transactional
    public CompanyDto updateCompany(UUID companyId, CompanyUpdateRequest request) {
        if (companyId == null) {
            throw new BadUpdateRequestException("companyId is required");
        }

        if (request == null) {
            throw new BadUpdateRequestException("Request body is required");
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

        // ✅ FIX: normalize tags if they are present in request
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
            throw new BadUpdateRequestException("No fields provided to update");
        }

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
                throw new CompanyNotFoundException();
            }

            return updated;
        } catch (DuplicateKeyException e) {
            throw new DuplicateCareersUrlException(careersUrl);
        } catch (DataIntegrityViolationException e) {
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

    public void deleteCompany(UUID companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("companyId is required");
        }

        int deleted = companyRepository.deleteCompany(companyId);
        if (deleted == 0) {
            throw new CompanyNotFoundException();
        }
    }
}
