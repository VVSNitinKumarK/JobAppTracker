package com.jobapptracker.backend.company.dto;

import com.jobapptracker.backend.tag.dto.TagDto;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.List;

/**
 * Request for partial company updates. All fields are optional.
 * Note: JSR-303 doesn't have a built-in "if not null, then not blank" constraint.
 * String fields (companyName, careersUrl) are validated manually in the service layer
 * to ensure they're not blank if provided.
 */
public record CompanyUpdateRequest(
        String companyName,
        String careersUrl,
        LocalDate lastVisitedOn,

        @Positive(message = "revisitAfterDays must be > 0")
        Integer revisitAfterDays,

        List<TagDto> tags
) {}
