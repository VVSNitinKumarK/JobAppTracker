package com.jobapptracker.backend.company;

public final class CompanyConstants {

    private CompanyConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final int DEFAULT_REVISIT_AFTER_DAYS = 7;

    public static final int MIN_REVISIT_AFTER_DAYS = 1;
}