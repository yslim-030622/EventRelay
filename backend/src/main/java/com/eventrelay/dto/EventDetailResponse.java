package com.eventrelay.dto;

import com.eventrelay.model.IncomingEvent;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record EventDetailResponse(
    Long id,
    String eventId,
    String source,
    String eventType,
    String status,
    int retryCount,
    String lastError,
    Map<String, Object> headers,
    Map<String, Object> payload,
    Instant createdAt,
    Instant processedAt,
    List<DeliveryAttemptResponse> deliveries
) {
    public static EventDetailResponse from(IncomingEvent event) {
        return new EventDetailResponse(
            event.getId(),
            event.getEventId(),
            event.getSource().getName(),
            event.getEventType(),
            event.getStatus().name(),
            event.getRetryCount(),
            event.getLastError(),
            event.getHeaders(),
            event.getPayload(),
            event.getCreatedAt(),
            event.getProcessedAt(),
            event.getDeliveries().stream()
                .sorted(Comparator.comparingInt(delivery -> delivery.getAttemptNumber()))
                .map(DeliveryAttemptResponse::from)
                .toList()
        );
    }
}
