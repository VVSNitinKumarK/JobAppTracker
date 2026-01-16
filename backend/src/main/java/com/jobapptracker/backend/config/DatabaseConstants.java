package com.jobapptracker.backend.config;

public final class DatabaseConstants {

    private DatabaseConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String SCHEMA = "jobapps";

    public static final int BATCH_SIZE = 100;

    public static final String TABLE_COMPANY_TRACKING = SCHEMA + ".company_tracking";
    public static final String TABLE_COMPANY_TAG = SCHEMA + ".company_tag";
    public static final String TABLE_TAG = SCHEMA + ".tag";
    public static final String TABLE_DAILY_CHECKLIST = SCHEMA + ".daily_checklist";
}