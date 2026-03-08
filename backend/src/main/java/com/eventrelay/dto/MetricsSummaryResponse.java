package com.eventrelay.dto;

public record MetricsSummaryResponse(
    long totalEvents,
    long receivedEvents,
    long processingEvents,
    long processedEvents,
    long failedEvents,
    long deadLetterEvents
) {
}
