package com.adconnect.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @param jwtSecret   HMAC key shared with auth-service (at least 32 bytes)
 * @param publicPaths Ant-style patterns reachable without a token; prefix with "GET " to make only reads public
 */
@ConfigurationProperties(prefix = "adconnect.security")
public record GatewaySecurityProperties(String jwtSecret, List<String> publicPaths) {
}
