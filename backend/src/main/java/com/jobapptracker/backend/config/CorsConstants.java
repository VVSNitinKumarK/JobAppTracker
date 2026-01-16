package com.jobapptracker.backend.config;

public final class CorsConstants {

    private CorsConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static final String API_PATH_PATTERN = "/api/**";

    public static final long MAX_AGE_SECONDS = 3600;
}