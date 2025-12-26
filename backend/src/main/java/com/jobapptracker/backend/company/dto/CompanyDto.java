package com.jobapptracker.backend.company.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CompanyDto (
    UUID companyId,
    String companyName,
    String careersUrl,
    LocalDate lastVisitedOn,
    int revisitAfterDays,
    List<String> tags,
    LocalDate nextVisitOn,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
