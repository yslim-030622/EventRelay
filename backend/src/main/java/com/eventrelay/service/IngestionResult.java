package com.eventrelay.service;

import com.eventrelay.dto.WebhookResponse;
import org.springframework.http.HttpStatus;

public record IngestionResult(
    HttpStatus status,
    WebhookResponse body
) {
}
