package com.jobapptracker.backend.company.service;

import java.util.UUID;

public class CompanyNotFoundException extends RuntimeException {

    private final UUID companyId;

    public CompanyNotFoundException(UUID companyId) {
        super("Company not found");
        this.companyId = companyId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getInternalDetails() {
        return "Company not found with id: " + companyId;
    }
}