package com.jobapptracker.backend.config;

/**
 * Pagination-related constants for list endpoints.
 * Defines defaults and limits to ensure consistent pagination behavior.
 */
public final class PaginationConstants {

    private PaginationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Default page number when not specified (zero-indexed).
     */
    public static final int DEFAULT_PAGE = 0;

    /**
     * Default page size when not specified.
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Maximum allowed page size to prevent excessive data transfer.
     */
    public static final int MAX_PAGE_SIZE = 200;

    /**
     * Maximum allowed page number to prevent integer overflow.
     * With MAX_PAGE_SIZE = 200, this allows querying up to 200M records.
     */
    public static final int MAX_PAGE_NUMBER = 1_000_000;
}