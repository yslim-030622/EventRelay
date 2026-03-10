package com.eventrelay.service;

import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import com.eventrelay.repository.IncomingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventQueryService {

    private final IncomingEventRepository incomingEventRepository;
    private final EventRoutingService eventRoutingService;

    public EventQueryService(
        IncomingEventRepository incomingEventRepository,
        EventRoutingService eventRoutingService
    ) {
        this.incomingEventRepository = incomingEventRepository;
        this.eventRoutingService = eventRoutingService;
    }

    public Page<IncomingEvent> list(String status, String source, Pageable pageable) {
        EventStatus parsedStatus = parseStatus(status);

        if (parsedStatus != null && source != null && !source.isBlank()) {
            return incomingEventRepository.findByStatusAndSource_Name(parsedStatus, source, pageable);
        }
        if (parsedStatus != null) {
            return incomingEventRepository.findByStatus(parsedStatus, pageable);
        }
        if (source != null && !source.isBlank()) {
            return incomingEventRepository.findBySource_Name(source, pageable);
        }
        return incomingEventRepository.findAll(pageable);
    }

    public IncomingEvent getById(Long id) {
        return incomingEventRepository.findDetailedById(id)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));
    }

    @Transactional
    public IncomingEvent replay(Long id) {
        IncomingEvent event = incomingEventRepository.findDetailedById(id)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + id));

        event.setStatus(EventStatus.RECEIVED);
        event.setRetryCount(0);
        event.setProcessedAt(null);
        event.setLastError(null);
        event.setNextRetryAt(null);
        incomingEventRepository.save(event);

        String sourceName = event.getSource().getName();
        String eventType = event.getEventType();
        String routingKey = recomputeRoutingKey(sourceName, eventType);
        eventRoutingService.publish(event.getId(), routingKey);

        return event;
    }

    private String recomputeRoutingKey(String sourceName, String eventType) {
        return switch (sourceName) {
            case "github" -> "github." + eventType;
            case "stripe" -> "payment." + eventType;
            default -> "generic." + eventType;
        };
    }

    private EventStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return EventStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported status filter: " + status);
        }
    }
}
