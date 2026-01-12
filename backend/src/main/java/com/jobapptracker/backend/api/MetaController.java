package com.jobapptracker.backend.api;

import com.jobapptracker.backend.config.DatabaseConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
public class MetaController {

    private static final Logger log = LoggerFactory.getLogger(MetaController.class);

    private final JdbcTemplate jdbcTemplate;

    public MetaController(JdbcTemplate jdbc) {
        this.jdbcTemplate = jdbc;
    }

    @GetMapping("/api/meta/max-next-visit-on")
    public Map<String, Object> getMaxNextVisitOn() {
        log.info("GET /api/meta/max-next-visit-on - fetching max next visit date");

        LocalDate latestDate = jdbcTemplate.query(
                "SELECT MAX(next_visit_on) as max_next_visit_on FROM " + DatabaseConstants.TABLE_COMPANY_TRACKING,
                resultSet -> resultSet.next() ? resultSet.getObject("max_next_visit_on", LocalDate.class) : null
        );

        return Map.of("maxNextVisitOn", Optional.ofNullable(latestDate));
    }
}
