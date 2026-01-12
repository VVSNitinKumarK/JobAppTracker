package com.jobapptracker.backend.config;

/**
 * CORS (Cross-Origin Resource Sharing) configuration constants.
 * These values control which origins can access the API.
 *
 * Note: Allowed origins are configured via application.yml (cors.allowed-origins)
 * to support environment-specific configuration.
 */
public final class CorsConstants {

    private CorsConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * API path pattern for CORS mapping.
     */
    public static final String API_PATH_PATTERN = "/api/**";

    /**
     * Maximum age (in seconds) for preflight request caching.
     * 3600 seconds = 1 hour.
     */
    public static final long MAX_AGE_SECONDS = 3600;
}