package com.eventrelay.dto;

public record WebhookResponse(
    String status,
    String eventId,
    String message
) {
}
