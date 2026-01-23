package com.jobapptracker.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.Arrays;

@Configuration
public class DbConfig {

    private static final Logger log = LoggerFactory.getLogger(DbConfig.class);

    private static final int POOL_MAX_SIZE = 10;
    private static final int POOL_MIN_IDLE = 2;
    private static final long IDLE_TIMEOUT_MS = 300_000;
    private static final long CONNECTION_TIMEOUT_MS = 20_000;
    private static final long MAX_LIFETIME_MS = 1_200_000;

    private final Environment springEnv;

    public DbConfig(Environment springEnv) {
        this.springEnv = springEnv;
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return value;
    }

    private boolean isDockerProfile() {
        return Arrays.asList(springEnv.getActiveProfiles()).contains("docker");
    }

    @Bean
    public DataSource dataSource() {
        log.info("Initializing database connection");

        String jdbcUrl;
        String user;
        String password;

        if (isDockerProfile()) {
            // Docker: use Spring environment variables (from docker-compose)
            log.info("Using Docker profile - reading from environment variables");
            jdbcUrl = require(springEnv.getProperty("SPRING_DATASOURCE_URL"), "SPRING_DATASOURCE_URL");
            user = require(springEnv.getProperty("SPRING_DATASOURCE_USERNAME"), "SPRING_DATASOURCE_USERNAME");
            password = require(springEnv.getProperty("SPRING_DATASOURCE_PASSWORD"), "SPRING_DATASOURCE_PASSWORD");
        } else {
            // Local: use AF_ENV_DIR and .env.local file
            String envDir = System.getenv("AF_ENV_DIR");
            if (envDir == null || envDir.isBlank()) {
                throw new IllegalStateException(
                        "AF_ENV_DIR environment variable must be set. " +
                        "Expected: path to directory containing JobAppTracker/.env.local"
                );
            }

            String envFilePath = envDir + "/JobAppTracker/.env.local";
            log.info("Loading environment from: {}", envFilePath);

            Dotenv dotenv;
            try {
                dotenv = Dotenv.configure()
                        .directory(envDir + "/JobAppTracker")
                        .filename(".env.local")
                        .load();
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Failed to load .env.local from " + envFilePath + ". " +
                        "Ensure the file exists and is readable.", e
                );
            }

            String host = require(dotenv.get("DB_HOST"), "DB_HOST");
            String port = require(dotenv.get("DB_PORT"), "DB_PORT");
            String db = require(dotenv.get("DB_NAME"), "DB_NAME");
            String schema = require(dotenv.get("DB_SCHEMA"), "DB_SCHEMA");
            user = require(dotenv.get("DB_USERNAME"), "DB_USERNAME");
            password = require(dotenv.get("DB_PASSWORD"), "DB_PASSWORD");

            jdbcUrl = String.format(
                    "jdbc:postgresql://%s:%s/%s?currentSchema=%s",
                    host, port, db, schema
            );

            log.debug("Database connection configured: host={}, port={}, database={}, schema={}",
                    host, port, db, schema);
        }

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(user);
        dataSource.setPassword(password);

        dataSource.setMaximumPoolSize(POOL_MAX_SIZE);
        dataSource.setMinimumIdle(POOL_MIN_IDLE);
        dataSource.setIdleTimeout(IDLE_TIMEOUT_MS);
        dataSource.setConnectionTimeout(CONNECTION_TIMEOUT_MS);
        dataSource.setMaxLifetime(MAX_LIFETIME_MS);
        dataSource.setPoolName("JobAppTrackerPool");

        log.info("HikariCP connection pool initialized: maxPoolSize={}, minIdle={}",
                dataSource.getMaximumPoolSize(), dataSource.getMinimumIdle());

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }
}
