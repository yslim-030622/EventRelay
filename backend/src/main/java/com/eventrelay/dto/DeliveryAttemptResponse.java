package com.eventrelay.dto;

import com.eventrelay.model.EventDelivery;

import java.time.Instant;

public record DeliveryAttemptResponse(
    Long id,
    String consumerName,
    int attemptNumber,
    String status,
    Integer durationMs,
    String errorMessage,
    Instant createdAt
) {
    public static DeliveryAttemptResponse from(EventDelivery delivery) {
        return new DeliveryAttemptResponse(
            delivery.getId(),
            delivery.getConsumerName(),
            delivery.getAttemptNumber(),
            delivery.getStatus(),
            delivery.getDurationMs(),
            delivery.getErrorMessage(),
            delivery.getCreatedAt()
        );
    }
}
