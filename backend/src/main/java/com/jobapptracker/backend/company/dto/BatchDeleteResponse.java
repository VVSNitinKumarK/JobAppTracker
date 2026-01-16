package com.jobapptracker.backend.company.dto;

public record BatchDeleteResponse(
        int deleted,
        int requested
) {}