package com.jobapptracker.backend.company;

/**
 * Company-related business constants.
 * Defines default values and business rules for company tracking.
 */
public final class CompanyConstants {

    private CompanyConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Default number of days to wait before revisiting a company.
     * Used when revisitAfterDays is not specified in create requests.
     */
    public static final int DEFAULT_REVISIT_AFTER_DAYS = 7;

    /**
     * Minimum allowed value for revisitAfterDays.
     */
    public static final int MIN_REVISIT_AFTER_DAYS = 1;
}