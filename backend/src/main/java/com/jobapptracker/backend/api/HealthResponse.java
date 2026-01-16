package com.jobapptracker.backend.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HealthResponse(
        String status,
        Instant timestamp,
        Map<String, ComponentHealth> components
) {
    public static final String STATUS_UP = "UP";
    public static final String STATUS_DOWN = "DOWN";

    public static HealthResponse healthy(Map<String, ComponentHealth> components) {
        return new HealthResponse(STATUS_UP, Instant.now(), components);
    }

    public static HealthResponse unhealthy(Map<String, ComponentHealth> components) {
        return new HealthResponse(STATUS_DOWN, Instant.now(), components);
    }

    public record ComponentHealth(
            String status,
            Map<String, Object> details
    ) {
        public static ComponentHealth up(Map<String, Object> details) {
            return new ComponentHealth(STATUS_UP, details);
        }

        public static ComponentHealth down(String error) {
            return new ComponentHealth(STATUS_DOWN, Map.of("error", error));
        }
    }
}
