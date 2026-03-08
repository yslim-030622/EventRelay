package com.eventrelay.dto;

import com.eventrelay.model.WebhookSource;

import java.time.Instant;

public record SourceResponse(
    Long id,
    String name,
    String displayName,
    boolean active,
    Instant createdAt
) {
    public static SourceResponse from(WebhookSource source) {
        return new SourceResponse(
            source.getId(),
            source.getName(),
            source.getDisplayName(),
            source.isActive(),
            source.getCreatedAt()
        );
    }
}
