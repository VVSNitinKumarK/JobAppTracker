package com.jobapptracker.backend.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/health")
    public ResponseEntity<HealthResponse> health() {
        log.debug("GET /api/health - performing health check");

        Map<String, HealthResponse.ComponentHealth> components = new HashMap<>();
        boolean allHealthy = true;

        HealthResponse.ComponentHealth dbHealth = checkDatabase();
        components.put("database", dbHealth);
        if (!"UP".equals(dbHealth.status())) {
            allHealthy = false;
        }

        HealthResponse.ComponentHealth diskHealth = checkDiskSpace();
        components.put("diskSpace", diskHealth);
        if (!"UP".equals(diskHealth.status())) {
            allHealthy = false;
        }

        HealthResponse response = allHealthy
                ? HealthResponse.healthy(components)
                : HealthResponse.unhealthy(components);

        HttpStatus status = allHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        if (!allHealthy) {
            log.error("Health check FAILED: {}", response);
        }

        return ResponseEntity.status(status).body(response);
    }

    private HealthResponse.ComponentHealth checkDatabase() {
        try {
            String schema = jdbc.queryForObject("SELECT current_schema()", String.class);
            Long connectionCount = jdbc.queryForObject(
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = current_database()",
                    Long.class
            );

            Map<String, Object> details = new HashMap<>();
            details.put("schema", schema);
            details.put("activeConnections", connectionCount != null ? connectionCount : 0);
            details.put("database", "PostgreSQL");

            return HealthResponse.ComponentHealth.up(details);
        } catch (Exception e) {
            log.error("Database health check failed", e);
            return HealthResponse.ComponentHealth.down(e.getMessage());
        }
    }

    private HealthResponse.ComponentHealth checkDiskSpace() {
        try {
            File root = new File(".");
            long totalSpace = root.getTotalSpace();
            long freeSpace = root.getFreeSpace();
            long usableSpace = root.getUsableSpace();

            double freePercentage = (usableSpace * 100.0) / totalSpace;

            Map<String, Object> details = new HashMap<>();
            details.put("total", formatBytes(totalSpace));
            details.put("free", formatBytes(freeSpace));
            details.put("usable", formatBytes(usableSpace));
            details.put("freePercentage", String.format("%.2f%%", freePercentage));

            if (freePercentage < 10) {
                log.warn("Low disk space: {}% free", String.format("%.2f", freePercentage));
                return HealthResponse.ComponentHealth.down(
                        "Low disk space: only " + String.format("%.2f%%", freePercentage) + " available"
                );
            }

            return HealthResponse.ComponentHealth.up(details);
        } catch (Exception e) {
            log.error("Disk space health check failed", e);
            return HealthResponse.ComponentHealth.down(e.getMessage());
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
