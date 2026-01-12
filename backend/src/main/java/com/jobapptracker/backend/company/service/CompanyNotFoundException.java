package com.jobapptracker.backend.company.service;

import java.util.UUID;

public class CompanyNotFoundException extends RuntimeException {

    private final UUID companyId;

    public CompanyNotFoundException() {
        super("Company not found");
        this.companyId = null;
    }

    public CompanyNotFoundException(UUID companyId) {
        super("Company not found with id: " + companyId);
        this.companyId = companyId;
    }

    public CompanyNotFoundException(String message) {
        super(message);
        this.companyId = null;
    }

    public UUID getCompanyId() {
        return companyId;
    }
}