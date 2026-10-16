package com.adconnect.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Validates the bearer token for every non-public route and forwards the caller's identity to
 * downstream services as X-User-* headers. Client-supplied X-User-* headers are always stripped so
 * they cannot be spoofed.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final SecretKey key;
    private final GatewaySecurityProperties properties;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(GatewaySecurityProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.jwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String correlationId = request.getHeaders().getFirst(CORRELATION_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String finalCorrelationId = correlationId;
        exchange.getResponse().getHeaders().set(CORRELATION_HEADER, finalCorrelationId);

        String token = extractToken(request);
        boolean isPublic = isPublic(request.getMethod(), request.getPath().value());

        ServerHttpRequest.Builder builder = request.mutate().headers(h -> {
            h.remove("X-User-Id");
            h.remove("X-User-Email");
            h.remove("X-User-Name");
            h.remove("X-User-Role");
            h.set(CORRELATION_HEADER, finalCorrelationId);
        });

        if (token == null) {
            if (isPublic || request.getMethod() == HttpMethod.OPTIONS) {
                return chain.filter(exchange.mutate().request(builder.build()).build());
            }
            return reject(exchange, "Missing bearer token");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            if (isPublic) {
                return chain.filter(exchange.mutate().request(builder.build()).build());
            }
            log.debug("Rejected token on {}: {}", request.getPath(), e.getMessage());
            return reject(exchange, "Invalid or expired token");
        }
        if (!"access".equals(claims.get("type", String.class))) {
            return reject(exchange, "Access token required");
        }

        String name = claims.get("name", String.class);
        builder.headers(h -> {
            h.set("X-User-Id", claims.getSubject());
            h.set("X-User-Email", claims.get("email", String.class));
            h.set("X-User-Role", claims.get("role", String.class));
            if (name != null) {
                h.set("X-User-Name", URLEncoder.encode(name, StandardCharsets.UTF_8));
            }
        });
        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        // Browsers cannot set headers on the WebSocket handshake, so /ws accepts ?access_token=
        if (request.getPath().value().startsWith("/ws")) {
            return request.getQueryParams().getFirst("access_token");
        }
        return null;
    }

    private boolean isPublic(HttpMethod method, String path) {
        for (String entry : properties.publicPaths()) {
            String pattern = entry;
            if (entry.startsWith("GET ")) {
                if (method != HttpMethod.GET) {
                    continue;
                }
                pattern = entry.substring(4);
            }
            if (matcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\",\"path\":\""
                + exchange.getRequest().getPath().value().replace("\"", "") + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
