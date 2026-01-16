package com.jobapptracker.backend.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        log.debug("GET /api/health");

        Map<String, HealthResponse.ComponentHealth> components = new HashMap<>();

        HealthResponse.ComponentHealth dbHealth = checkDatabase();
        components.put("database", dbHealth);

        boolean healthy = HealthResponse.STATUS_UP.equals(dbHealth.status());
        HealthResponse response = healthy
                ? HealthResponse.healthy(components)
                : HealthResponse.unhealthy(components);

        if (!healthy) {
            log.error("Health check FAILED: database unreachable");
        }

        return ResponseEntity
                .status(healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE)
                .body(response);
    }

    private HealthResponse.ComponentHealth checkDatabase() {
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            return HealthResponse.ComponentHealth.up(Map.of("database", "PostgreSQL"));
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return HealthResponse.ComponentHealth.down("Database unreachable");
        }
    }
}
