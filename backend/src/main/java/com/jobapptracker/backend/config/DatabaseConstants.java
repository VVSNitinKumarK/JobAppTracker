package com.jobapptracker.backend.config;

/**
 * Database-related constants including schema names and table references.
 * Centralizes database identifiers to avoid hardcoding throughout the codebase.
 */
public final class DatabaseConstants {

    private DatabaseConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Main application schema name.
     * Can be overridden via application.properties if needed for different environments.
     */
    public static final String SCHEMA = "jobapps";

    // Fully qualified table names
    public static final String TABLE_COMPANY_TRACKING = SCHEMA + ".company_tracking";
    public static final String TABLE_COMPANY_TAG = SCHEMA + ".company_tag";
    public static final String TABLE_TAG = SCHEMA + ".tag";
    public static final String TABLE_DAILY_CHECKLIST = SCHEMA + ".daily_checklist";
}