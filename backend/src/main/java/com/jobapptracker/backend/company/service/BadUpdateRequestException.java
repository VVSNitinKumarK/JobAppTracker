package com.jobapptracker.backend.company.service;

import java.util.UUID;

public class BadUpdateRequestException extends RuntimeException {

    private final UUID companyId;
    private final String field;
    private final String internalMessage;

    public BadUpdateRequestException(String safeMessage) {
        super(safeMessage);
        this.companyId = null;
        this.field = null;
        this.internalMessage = safeMessage;
    }

    public BadUpdateRequestException(String safeMessage, UUID companyId, String field) {
        super(safeMessage);
        this.companyId = companyId;
        this.field = field;
        this.internalMessage = safeMessage;
    }

    public BadUpdateRequestException(String safeMessage, String internalMessage, UUID companyId, String field) {
        super(safeMessage);
        this.companyId = companyId;
        this.field = field;
        this.internalMessage = internalMessage;
    }

    public BadUpdateRequestException(String safeMessage, String internalMessage, UUID companyId, String field, Throwable cause) {
        super(safeMessage, cause);
        this.companyId = companyId;
        this.field = field;
        this.internalMessage = internalMessage;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getField() {
        return field;
    }

    public String getInternalDetails() {
        StringBuilder details = new StringBuilder();
        details.append(internalMessage);

        if (companyId != null) {
            details.append(" [companyId=").append(companyId).append("]");
        }
        if (field != null) {
            details.append(" [field=").append(field).append("]");
        }

        return details.toString();
    }
}