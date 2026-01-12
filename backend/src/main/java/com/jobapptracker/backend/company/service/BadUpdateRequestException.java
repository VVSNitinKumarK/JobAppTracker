package com.jobapptracker.backend.company.service;

import java.util.UUID;

public class BadUpdateRequestException extends RuntimeException {

    private final UUID companyId;
    private final String field;

    public BadUpdateRequestException(String message) {
        super(message);
        this.companyId = null;
        this.field = null;
    }

    public BadUpdateRequestException(String message, Throwable cause) {
        super(message, cause);
        this.companyId = null;
        this.field = null;
    }

    public BadUpdateRequestException(String message, UUID companyId, String field) {
        super(message);
        this.companyId = companyId;
        this.field = field;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getField() {
        return field;
    }
}