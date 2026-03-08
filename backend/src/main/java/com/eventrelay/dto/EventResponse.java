package com.eventrelay.dto;

import com.eventrelay.model.IncomingEvent;

import java.time.Instant;

public record EventResponse(
    Long id,
    String eventId,
    String source,
    String eventType,
    String status,
    Instant createdAt,
    Instant processedAt
) {
    public static EventResponse from(IncomingEvent event) {
        return new EventResponse(
            event.getId(),
            event.getEventId(),
            event.getSource().getName(),
            event.getEventType(),
            event.getStatus().name(),
            event.getCreatedAt(),
            event.getProcessedAt()
        );
    }
}
