package com.jobapptracker.backend.config;

public final class PaginationConstants {

    private PaginationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final int DEFAULT_PAGE = 0;

    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final int MAX_PAGE_SIZE = 200;

    public static final int MAX_PAGE_NUMBER = 1_000_000;
}