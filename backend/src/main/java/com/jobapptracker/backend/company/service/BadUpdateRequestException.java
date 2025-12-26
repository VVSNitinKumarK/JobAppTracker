package com.jobapptracker.backend.company.service;

public class BadUpdateRequestException extends RuntimeException {

    public BadUpdateRequestException(String message) {
        super(message);
    }

    public BadUpdateRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}