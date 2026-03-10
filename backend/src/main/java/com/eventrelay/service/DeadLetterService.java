package com.eventrelay.service;

import com.eventrelay.model.DeadLetterEvent;
import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import com.eventrelay.repository.DeadLetterEventRepository;
import com.eventrelay.repository.IncomingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class DeadLetterService {

    private final DeadLetterEventRepository deadLetterEventRepository;
    private final IncomingEventRepository incomingEventRepository;
    private final EventRoutingService eventRoutingService;
    private final DiscordAlertService discordAlertService;

    public DeadLetterService(
        DeadLetterEventRepository deadLetterEventRepository,
        IncomingEventRepository incomingEventRepository,
        EventRoutingService eventRoutingService,
        DiscordAlertService discordAlertService
    ) {
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.incomingEventRepository = incomingEventRepository;
        this.eventRoutingService = eventRoutingService;
        this.discordAlertService = discordAlertService;
    }

    public List<DeadLetterEvent> findAll() {
        return deadLetterEventRepository.findAll();
    }

    @Transactional
    public DeadLetterEvent moveToDeadLetter(IncomingEvent event, String errorMessage, String reason) {
        event.setStatus(EventStatus.DEAD_LETTER);
        event.setLastError(errorMessage);
        incomingEventRepository.save(event);

        DeadLetterEvent deadLetterEvent = new DeadLetterEvent();
        deadLetterEvent.setEvent(event);
        deadLetterEvent.setErrorMessage(errorMessage);
        deadLetterEvent.setReason(reason);
        DeadLetterEvent saved = deadLetterEventRepository.save(deadLetterEvent);

        discordAlertService.sendAlert(
            "Dead Letter: " + event.getSource().getName() + " / " + event.getEventType(),
            "Event ID: " + event.getEventId() + "\nError: " + errorMessage,
            0xFF0000
        );

        return saved;
    }

    @Transactional
    public void markReplayed(Long deadLetterId) {
        DeadLetterEvent deadLetterEvent = deadLetterEventRepository.findById(deadLetterId)
            .orElseThrow(() -> new EntityNotFoundException("Dead-letter event not found: " + deadLetterId));

        deadLetterEvent.setReplayed(true);
        deadLetterEvent.setReplayedAt(Instant.now());

        IncomingEvent event = deadLetterEvent.getEvent();
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
    }

    private String recomputeRoutingKey(String sourceName, String eventType) {
        return switch (sourceName) {
            case "github" -> "github." + eventType;
            case "stripe" -> "payment." + eventType;
            default -> "generic." + eventType;
        };
    }
}
