package com.jobapptracker.backend.company.service;

public class DuplicateCareersUrlException extends RuntimeException {
    public DuplicateCareersUrlException(String careersUrl) {
        super("careers_url already exists: " + careersUrl);
    }
}
