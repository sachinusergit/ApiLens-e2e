package com.apilens.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class JwtFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    // Public routes — no JWT required
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/token",
            "/api/auth/health",
            "/api/files/health",
            "/actuator"
    );

    private final SecretKey key;

    public JwtFilter(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be >= 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String path = req.getPath().value();

        // Generate/propagate correlation id
        String reqId = req.getHeaders().getFirst("X-Request-Id");
        if (reqId == null) {
            reqId = java.util.UUID.randomUUID().toString();
        }
        final String finalReqId = reqId;

        // Allow public paths through without auth
        for (String pub : PUBLIC_PATHS) {
            if (path.equals(pub) || path.startsWith(pub + "/")) {
                return chain.filter(withHeaders(exchange, null, null, finalReqId));
            }
        }

        String authHeader = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(authHeader.substring(7))
                    .getPayload();

            String userId = claims.getSubject();
            String orgId = String.valueOf(claims.get("orgId"));

            return chain.filter(withHeaders(exchange, userId, orgId, finalReqId));
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private ServerWebExchange withHeaders(ServerWebExchange exchange, String userId,
                                          String orgId, String reqId) {
        ServerHttpRequest.Builder builder = exchange.getRequest().mutate()
                .header("X-Request-Id", reqId);
        if (userId != null) builder.header("X-User-Id", userId);
        if (orgId != null) builder.header("X-Org-Id", orgId);
        return exchange.mutate().request(builder.build()).build();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
