package com.adconnect.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "adconnect")
public record AuthProperties(Jwt jwt, Seed seed, String frontendUrl) {

    public record Jwt(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
    }

    public record Seed(String adminEmail, String adminPassword, String managerEmail, String managerPassword) {
    }
}
