package com.jobapptracker.backend.company.dto;

import java.time.LocalDate;
import java.util.List;

public record CompanyUpdateRequest(
        String companyName,
        String careersUrl,
        LocalDate lastVisitedOn,
        Integer revisitAfterDays,
        List<String> tags
) {}
