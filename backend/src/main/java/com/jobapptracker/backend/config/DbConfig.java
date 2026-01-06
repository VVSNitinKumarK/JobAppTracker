package com.jobapptracker.backend.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
public class DbConfig {

    private static String require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing required env var: " + name);
        }
        return v;
    }

    @Bean
    public DataSource dataSource() {
        String envDir = System.getenv("AF_ENV_DIR");
        if (envDir == null || envDir.isBlank()) {
            throw new IllegalStateException("AF_ENV_DIR must be set as a system variable");
        }

        Dotenv dotenv = Dotenv.configure()
                .directory(envDir + "/JobAppTracker")
                .filename(".env.local")
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        String host = require(dotenv.get("DB_HOST"), "DB_HOST");
        String port = require(dotenv.get("DB_PORT"), "DB_PORT");
        String db = require(dotenv.get("DB_NAME"), "DB_NAME");
        String schema = require(dotenv.get("DB_SCHEMA"), "DB_SCHEMA");
        String user = require(dotenv.get("DB_USERNAME"), "DB_USERNAME");
        String password = require(dotenv.get("DB_PASSWORD"), "DB_PASSWORD");


        String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%s/%s?currentSchema=%s",
                host, port, db, schema
        );

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setUrl(jdbcUrl);
        ds.setUsername(user);
        ds.setPassword(password);

        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }
}
