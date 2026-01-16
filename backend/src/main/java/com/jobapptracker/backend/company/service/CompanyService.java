package com.jobapptracker.backend.company.service;

import com.jobapptracker.backend.company.CompanyConstants;
import com.jobapptracker.backend.company.dto.CompanyCreateRequest;
import com.jobapptracker.backend.company.dto.CompanyDto;
import com.jobapptracker.backend.company.dto.CompanyUpdateRequest;
import com.jobapptracker.backend.company.dto.PagedCompaniesResponse;
import com.jobapptracker.backend.company.repository.CompanyRepository;
import com.jobapptracker.backend.company.repository.CompanyTagUtil;
import com.jobapptracker.backend.company.web.DueFilter;
import com.jobapptracker.backend.config.DateUtils;
import com.jobapptracker.backend.config.PaginationConstants;
import com.jobapptracker.backend.tag.dto.TagDto;
import com.jobapptracker.backend.tag.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;
    private final TagRepository tagRepository;

    public CompanyService(CompanyRepository companyRepository, TagRepository tagRepository) {
        this.companyRepository = companyRepository;
        this.tagRepository = tagRepository;
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
            throw new IllegalArgumentException("page must be >= 0, but got: " + p);
        }
        if (p > PaginationConstants.MAX_PAGE_NUMBER) {
            throw new IllegalArgumentException("page must be <= " + PaginationConstants.MAX_PAGE_NUMBER + ", but got: " + p);
        }
        if (s <= 0) {
            throw new IllegalArgumentException("size must be > 0, but got: " + s);
        }

        if (s > PaginationConstants.MAX_PAGE_SIZE) {
            s = PaginationConstants.MAX_PAGE_SIZE;
        }

        List<String> tags = parseTags(tagsCsv);
        DueFilter due = DueFilter.fromStringOrNull(dueRaw);
        LocalDate date = DateUtils.parseDateOrNull(dateRaw);
        LocalDate lastVisitedOn = DateUtils.parseDateOrNull(lastVisitedOnRaw);

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

    private static List<String> extractTagDisplayNames(List<TagDto> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }

        return tags.stream()
                .filter(Objects::nonNull)
                .map(t -> {
                    String name = t.tagName();
                    String key = t.tagKey();
                    String candidate = (name != null && !name.isBlank()) ? name : key;
                    return (candidate == null) ? "" : candidate.trim();
                })
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();
    }

    @Transactional
    public CompanyDto createCompany(CompanyCreateRequest request) {
        String name = request.companyName().trim();
        String url = request.careersUrl().trim();
        int revisit = (request.revisitAfterDays() == null) ? CompanyConstants.DEFAULT_REVISIT_AFTER_DAYS : request.revisitAfterDays();

        log.info("Creating company: name={}, url={}, revisitAfterDays={}",
                name, url, revisit);

        List<String> tagNames = extractTagDisplayNames(request.tags());

        if (!tagNames.isEmpty()) {
            tagRepository.ensureTagsExist(tagNames);
        }

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
            tagNames = extractTagDisplayNames(request.tags());
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

        if (tagNames != null && !tagNames.isEmpty()) {
            tagRepository.ensureTagsExist(tagNames);
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
                throw new CompanyNotFoundException(companyId);
            }

            log.info("Company updated successfully: id={}, name={}", updated.companyId(), updated.companyName());
            return updated;
        } catch (DuplicateKeyException e) {
            throw new DuplicateCareersUrlException(careersUrl);
        } catch (DataIntegrityViolationException dataIntegrityException) {
            throw new BadUpdateRequestException(
                    "Invalid update request",
                    "Constraint violation: " + dataIntegrityException.getMessage(),
                    companyId,
                    null,
                    dataIntegrityException
            );
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
        log.info("Batch deleting {} companies", companyIds.size());
        int deleted = companyRepository.deleteCompanies(companyIds);
        log.info("Batch delete completed: {} out of {} companies deleted", deleted, companyIds.size());
        return deleted;
    }
}
