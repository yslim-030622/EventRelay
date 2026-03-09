package com.eventrelay.dto;

import com.eventrelay.model.DeadLetterEvent;

import java.time.Instant;

public record DeadLetterResponse(
    Long id,
    Long eventPk,
    String eventId,
    String eventType,
    String source,
    String errorMessage,
    String reason,
    boolean replayed,
    Instant createdAt,
    Instant replayedAt
) {
    public static DeadLetterResponse from(DeadLetterEvent deadLetterEvent) {
        return new DeadLetterResponse(
            deadLetterEvent.getId(),
            deadLetterEvent.getEvent().getId(),
            deadLetterEvent.getEvent().getEventId(),
            deadLetterEvent.getEvent().getEventType(),
            deadLetterEvent.getEvent().getSource().getName(),
            deadLetterEvent.getErrorMessage(),
            deadLetterEvent.getReason(),
            deadLetterEvent.isReplayed(),
            deadLetterEvent.getCreatedAt(),
            deadLetterEvent.getReplayedAt()
        );
    }
}
