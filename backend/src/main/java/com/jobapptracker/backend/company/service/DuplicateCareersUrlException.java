package com.jobapptracker.backend.company.service;

public class DuplicateCareersUrlException extends RuntimeException {

    private final String careersUrl;

    public DuplicateCareersUrlException(String careersUrl) {
        super("A company with this careers URL already exists");
        this.careersUrl = careersUrl;
    }

    public String getCareersUrl() {
        return careersUrl;
    }

    public String getInternalDetails() {
        return "Duplicate careersUrl: " + careersUrl;
    }
}
