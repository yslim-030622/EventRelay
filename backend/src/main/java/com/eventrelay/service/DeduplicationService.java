package com.eventrelay.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DeduplicationService {

    private final StringRedisTemplate redisTemplate;
    private final long dedupTtlSeconds;

    public DeduplicationService(
        StringRedisTemplate redisTemplate,
        @Value("${app.dedup-ttl-seconds}") long dedupTtlSeconds
    ) {
        this.redisTemplate = redisTemplate;
        this.dedupTtlSeconds = dedupTtlSeconds;
    }

    public boolean isDuplicate(String eventId) {
        Boolean wasClaimed = redisTemplate.opsForValue()
            .setIfAbsent(redisKey(eventId), "1", Duration.ofSeconds(dedupTtlSeconds));
        return wasClaimed == null || !wasClaimed;
    }

    private String redisKey(String eventId) {
        return "eventrelay:dedup:" + eventId;
    }
}
