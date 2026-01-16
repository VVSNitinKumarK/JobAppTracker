package com.jobapptracker.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class DateUtils {

    private static final Logger log = LoggerFactory.getLogger(DateUtils.class);

    private DateUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static LocalDate parseDate(String raw) {
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException dateParseException) {
            log.warn("Invalid date format received: '{}'", raw);
            throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD.");
        }
    }

    public static LocalDate parseDateOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return parseDate(raw);
    }
}
