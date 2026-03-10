package com.eventrelay.service;

import com.eventrelay.dto.WebhookResponse;
import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import com.eventrelay.model.WebhookSource;
import com.eventrelay.repository.IncomingEventRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WebhookIngestionService {

    private final ObjectMapper objectMapper;
    private final SourceService sourceService;
    private final SignatureVerifier signatureVerifier;
    private final DeduplicationService deduplicationService;
    private final IncomingEventRepository incomingEventRepository;
    private final EventRoutingService eventRoutingService;

    public WebhookIngestionService(
        ObjectMapper objectMapper,
        SourceService sourceService,
        SignatureVerifier signatureVerifier,
        DeduplicationService deduplicationService,
        IncomingEventRepository incomingEventRepository,
        EventRoutingService eventRoutingService
    ) {
        this.objectMapper = objectMapper;
        this.sourceService = sourceService;
        this.signatureVerifier = signatureVerifier;
        this.deduplicationService = deduplicationService;
        this.incomingEventRepository = incomingEventRepository;
        this.eventRoutingService = eventRoutingService;
    }

    @Transactional
    public IngestionResult ingest(String sourceName, byte[] payloadBytes, HttpHeaders headers) {
        WebhookSource source = sourceService.findActiveSource(sourceName);
        Map<String, Object> headerMap = normalizeHeaders(headers);

        if (!signatureVerifier.verify(sourceName, payloadBytes, headerMap, source.getSigningSecret())) {
            return new IngestionResult(
                HttpStatus.UNAUTHORIZED,
                new WebhookResponse("error", null, "Signature verification failed")
            );
        }

        Map<String, Object> payload = parsePayload(payloadBytes);
        String eventId = extractEventId(sourceName, headerMap, payload);
        if (deduplicationService.isDuplicate(eventId)) {
            return new IngestionResult(
                HttpStatus.OK,
                new WebhookResponse("duplicate", eventId, "Event already received")
            );
        }

        String eventType = extractEventType(sourceName, headerMap, payload);

        IncomingEvent event = new IncomingEvent();
        event.setEventId(eventId);
        event.setSource(source);
        event.setEventType(eventType);
        event.setHeaders(headerMap);
        event.setPayload(payload);
        event.setStatus(EventStatus.RECEIVED);

        IncomingEvent savedEvent = incomingEventRepository.save(event);
        eventRoutingService.publish(savedEvent.getId(), routingKey(sourceName, eventType));

        return new IngestionResult(
            HttpStatus.ACCEPTED,
            new WebhookResponse("accepted", eventId, "Event received and queued for processing")
        );
    }

    private Map<String, Object> normalizeHeaders(HttpHeaders headers) {
        return headers.toSingleValueMap().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> entry.getKey().toLowerCase(),
                Map.Entry::getValue,
                (left, right) -> right,
                LinkedHashMap::new
            ));
    }

    private Map<String, Object> parsePayload(byte[] payloadBytes) {
        try {
            return objectMapper.readValue(payloadBytes, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new IllegalArgumentException("Payload must be valid JSON");
        }
    }

    private String extractEventId(String sourceName, Map<String, Object> headers, Map<String, Object> payload) {
        if ("github".equals(sourceName) && headers.containsKey("x-github-delivery")) {
            return headers.get("x-github-delivery").toString();
        }
        if ("stripe".equals(sourceName) && payload.containsKey("id")) {
            return payload.get("id").toString();
        }
        return UUID.randomUUID().toString();
    }

    private String extractEventType(String sourceName, Map<String, Object> headers, Map<String, Object> payload) {
        if ("github".equals(sourceName)) {
            String eventName = String.valueOf(headers.getOrDefault("x-github-event", "unknown"));
            Object action = payload.get("action");
            if (action == null) {
                return eventName;
            }
            return eventName + "." + action;
        }

        if ("stripe".equals(sourceName)) {
            Object type = payload.get("type");
            return type != null ? type.toString() : "unknown";
        }

        return "unknown";
    }

    private String routingKey(String sourceName, String eventType) {
        if ("github".equals(sourceName)) {
            return "github." + eventType;
        }
        if ("stripe".equals(sourceName)) {
            return "payment." + eventType;
        }
        return "generic." + eventType;
    }
}
