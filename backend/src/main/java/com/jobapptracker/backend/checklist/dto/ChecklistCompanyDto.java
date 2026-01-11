package com.jobapptracker.backend.checklist.dto;

import com.jobapptracker.backend.tag.dto.TagDto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ChecklistCompanyDto(
        UUID companyId,
        String companyName,
        String careersUrl,
        LocalDate lastVisitedOn,
        int revisitAfterDays,
        List<TagDto> tags,
        LocalDate nextVisitOn,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        boolean completed,
        boolean inChecklist
) {}
