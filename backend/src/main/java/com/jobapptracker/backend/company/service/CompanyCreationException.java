package com.jobapptracker.backend.company.service;

import java.time.LocalDate;

public class CompanyCreationException extends RuntimeException {

    private final String companyName;
    private final String careersUrl;
    private final LocalDate lastVisitedOn;
    private final Integer revisitAfterDays;

    public CompanyCreationException(
            String companyName,
            String careersUrl,
            LocalDate lastVisitedOn,
            Integer revisitAfterDays
    ) {
        super("Failed to create company");
        this.companyName = companyName;
        this.careersUrl = careersUrl;
        this.lastVisitedOn = lastVisitedOn;
        this.revisitAfterDays = revisitAfterDays;
    }

    public CompanyCreationException(
            String companyName,
            String careersUrl,
            LocalDate lastVisitedOn,
            Integer revisitAfterDays,
            Throwable cause
    ) {
        super("Failed to create company", cause);
        this.companyName = companyName;
        this.careersUrl = careersUrl;
        this.lastVisitedOn = lastVisitedOn;
        this.revisitAfterDays = revisitAfterDays;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getCareersUrl() {
        return careersUrl;
    }

    public LocalDate getLastVisitedOn() {
        return lastVisitedOn;
    }

    public Integer getRevisitAfterDays() {
        return revisitAfterDays;
    }

    public String getInternalDetails() {
        return String.format(
                "companyName='%s', careersUrl='%s', lastVisitedOn=%s, revisitAfterDays=%d",
                companyName, careersUrl, lastVisitedOn, revisitAfterDays
        );
    }
}