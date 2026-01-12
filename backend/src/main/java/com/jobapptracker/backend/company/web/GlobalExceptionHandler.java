package com.jobapptracker.backend.company.web;

import com.jobapptracker.backend.company.service.BadUpdateRequestException;
import com.jobapptracker.backend.company.service.CompanyNotFoundException;
import com.jobapptracker.backend.company.service.DuplicateCareersUrlException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException e,
            HttpServletRequest request
    ) {
        log.error("Bad request: {}", e.getMessage(), e);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BadUpdateRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadUpdate(
            BadUpdateRequestException e,
            HttpServletRequest request
    ) {
        if (e.getCompanyId() != null && e.getField() != null) {
            log.error("Bad update request for company {}, field '{}': {}",
                    e.getCompanyId(), e.getField(), e.getMessage(), e);
        } else if (e.getCompanyId() != null) {
            log.error("Bad update request for company {}: {}",
                    e.getCompanyId(), e.getMessage(), e);
        } else {
            log.error("Bad update request: {}", e.getMessage(), e);
        }
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            CompanyNotFoundException e,
            HttpServletRequest request
    ) {
        if (e.getCompanyId() != null) {
            log.warn("Company not found with id {}: {}", e.getCompanyId(), e.getMessage());
        } else {
            log.warn("Company not found: {}", e.getMessage());
        }
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(DuplicateCareersUrlException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(
            DuplicateCareersUrlException e,
            HttpServletRequest request
    ) {
        log.error("Duplicate careers URL '{}': {}", e.getCareersUrl(), e.getMessage(), e);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                e.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception e,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception: {}", e.getMessage(), e);
        ErrorResponse error = ErrorResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
