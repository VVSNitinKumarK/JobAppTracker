package com.jobapptracker.backend.company.service;

import java.util.UUID;

public class CompanyNotFoundException extends RuntimeException {

    public CompanyNotFoundException() {
        super("Company not found");
    }

    public CompanyNotFoundException(UUID companyId) {
        super("Company not found with id: " + companyId);
    }

    public CompanyNotFoundException(String message) {
        super(message);
    }
}