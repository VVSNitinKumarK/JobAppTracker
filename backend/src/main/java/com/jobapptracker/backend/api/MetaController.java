package com.jobapptracker.backend.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
public class MetaController {

    private final JdbcTemplate jdbcTemplate;

    public MetaController(JdbcTemplate jdbc) {
        this.jdbcTemplate = jdbc;
    }

    @GetMapping("/api/meta/max-next-visit-on")
    public Map<String, Object> getMaxNextVisitOn() {

        LocalDate latestDate = jdbcTemplate.query(
                "SELECT MAX(next_visit_on) as max_next_visit_on FROM company_tracking",
                resultSet -> resultSet.next() ? resultSet.getObject("max_next_visit_on", LocalDate.class) : null
        );

        return Map.of("maxNextVisitOn", Optional.ofNullable(latestDate));
    }
}
