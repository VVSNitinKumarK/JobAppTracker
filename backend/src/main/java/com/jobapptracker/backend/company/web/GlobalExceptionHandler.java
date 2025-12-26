package com.jobapptracker.backend.company.web;

import com.jobapptracker.backend.company.service.BadUpdateRequestException;
import com.jobapptracker.backend.company.service.CompanyNotFoundException;
import com.jobapptracker.backend.company.service.DuplicateCareersUrlException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(BadUpdateRequestException.class)
    public ResponseEntity<Map<String, String>> handleBadUpdate(BadUpdateRequestException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(CompanyNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(CompanyNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(DuplicateCareersUrlException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(DuplicateCareersUrlException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));
    }
}
