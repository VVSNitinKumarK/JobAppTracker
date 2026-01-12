package com.jobapptracker.backend.checklist.dto;

import jakarta.validation.constraints.NotNull;

public record ChecklistUpdateRequest(
        @NotNull(message = "completed is required")
        Boolean completed
) {}