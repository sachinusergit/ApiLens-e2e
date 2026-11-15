package com.apilens.auth.config;

import com.apilens.auth.exception.RateLimitExceededException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, String>> invalidToken(JwtException e) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> rateLimited(RateLimitExceededException e) {
        long retryAfter = e.getRetryAfterSeconds();
        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter))
                .body(Map.of(
                        "error", "rate_limit_exceeded",
                        "message", String.valueOf(e.getMessage()),
                        "status", 429,
                        "timestamp", Instant.now().toString(),
                        "retryAfterSeconds", retryAfter,
                        "resetInSeconds", retryAfter
                ));
    }
}
