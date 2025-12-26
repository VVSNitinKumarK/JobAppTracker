package com.jobapptracker.backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ConfigProbe {
    @Bean
    ApplicationRunner probe(Environment env) {
        return args -> { };
    }
}
