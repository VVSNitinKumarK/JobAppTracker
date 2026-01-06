package com.jobapptracker.backend.company.dto;

import com.jobapptracker.backend.tag.dto.TagDto;

import java.time.LocalDate;
import java.util.List;

public record CompanyUpdateRequest(
        String companyName,
        String careersUrl,
        LocalDate lastVisitedOn,
        Integer revisitAfterDays,
        List<TagDto> tags
) {}
