package com.eventrelay.controller;

import com.eventrelay.dto.WebhookResponse;
import com.eventrelay.service.IngestionResult;
import com.eventrelay.service.WebhookIngestionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private final WebhookIngestionService webhookIngestionService;

    public WebhookController(WebhookIngestionService webhookIngestionService) {
        this.webhookIngestionService = webhookIngestionService;
    }

    @PostMapping(value = "/{source}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebhookResponse> receive(
        @PathVariable String source,
        @RequestBody byte[] payload,
        @RequestHeader HttpHeaders headers
    ) {
        IngestionResult result = webhookIngestionService.ingest(source, payload, headers);
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
