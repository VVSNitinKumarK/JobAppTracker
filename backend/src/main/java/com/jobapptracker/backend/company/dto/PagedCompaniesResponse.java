package com.jobapptracker.backend.company.dto;

import java.util.List;

public record PagedCompaniesResponse(
        List<CompanyDto> items,
        int page,
        int size,
        long total
) {}
