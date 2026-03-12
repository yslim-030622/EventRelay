package com.eventrelay.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeduplicationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private DeduplicationService deduplicationService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        deduplicationService = new DeduplicationService(redisTemplate, 86400L);
    }

    @Test
    void newEventIsNotDuplicate() {
        when(valueOperations.setIfAbsent(eq("eventrelay:dedup:event-123"), eq("1"), any(Duration.class)))
            .thenReturn(true);

        assertThat(deduplicationService.isDuplicate("event-123")).isFalse();
    }

    @Test
    void existingEventIsDuplicate() {
        when(valueOperations.setIfAbsent(eq("eventrelay:dedup:event-456"), eq("1"), any(Duration.class)))
            .thenReturn(false);

        assertThat(deduplicationService.isDuplicate("event-456")).isTrue();
    }

    @Test
    void nullResultFromRedisIsDuplicate() {
        when(valueOperations.setIfAbsent(eq("eventrelay:dedup:event-789"), eq("1"), any(Duration.class)))
            .thenReturn(null);

        assertThat(deduplicationService.isDuplicate("event-789")).isTrue();
    }
}
