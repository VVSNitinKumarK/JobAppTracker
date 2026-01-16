package com.jobapptracker.backend.company.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BatchDeleteRequest(
        @NotEmpty(message = "companyIds list cannot be empty")
        List<UUID> companyIds
) {}