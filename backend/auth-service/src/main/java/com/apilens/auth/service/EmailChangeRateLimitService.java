package com.apilens.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class EmailChangeRateLimitService {

    private static final String KEY_PREFIX = "email_change:";

    private final StringRedisTemplate redis;
    private final long maxChanges;
    private final long windowSeconds;

    public EmailChangeRateLimitService(
            StringRedisTemplate redis,
            @Value("${app.email-change.max-changes:1}") long maxChanges,
            @Value("${app.email-change.window-hours:24}") long windowHours) {
        this.redis = redis;
        this.maxChanges = maxChanges;
        this.windowSeconds = windowHours * 3600L;
    }

    public record RateLimitStatus(boolean allowed, long currentCount,
                                  long maxChanges, long resetInSeconds) {}

    public RateLimitStatus check(UUID userId) {
        String key = key(userId);
        long current = 0;
        String raw = redis.opsForValue().get(key);
        if (raw != null) {
            try {
                current = Long.parseLong(raw);
            } catch (NumberFormatException ignored) {
                current = 0;
            }
        }

        Long ttl = redis.getExpire(key);
        long reset = (ttl != null && ttl > 0) ? ttl : windowSeconds;
        return new RateLimitStatus(current < maxChanges, current, maxChanges, reset);
    }

    /**
     * Records a successful email change. The first increment in a window is
     * given the full TTL; later increments keep the original expiry.
     */
    public long increment(UUID userId) {
        String key = key(userId);
        Long count = redis.opsForValue().increment(key);
        if (count != null) {
            Long ttl = redis.getExpire(key);
            if (ttl == null || ttl <= 0) {
                redis.expire(key, Duration.ofSeconds(windowSeconds));
            }
        }
        return count == null ? 1 : count;
    }

    public void reset(UUID userId) {
        redis.delete(key(userId));
    }

    public long windowSeconds() {
        return windowSeconds;
    }

    public long maxChanges() {
        return maxChanges;
    }

    private static String key(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
