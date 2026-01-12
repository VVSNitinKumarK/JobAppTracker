package com.jobapptracker.backend.company.service;

public class DuplicateCareersUrlException extends RuntimeException {

    private final String careersUrl;

    public DuplicateCareersUrlException(String careersUrl) {
        super("Career URL already exists: " + careersUrl);
        this.careersUrl = careersUrl;
    }

    public String getCareersUrl() {
        return careersUrl;
    }
}
