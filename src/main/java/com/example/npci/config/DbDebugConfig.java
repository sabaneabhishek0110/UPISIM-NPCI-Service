package com.example.npci.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DbDebugConfig {
    private final JdbcTemplate jdbcTemplate;

    public DbDebugConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void checkDbUser(){
        String user = jdbcTemplate.queryForObject(
                "select current_user",
                String.class
        );
        System.out.println("Backend is connected with DB user : "+user);
    }
}
