package com.eventrelay.service;

import com.eventrelay.model.EventDelivery;
import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import com.eventrelay.repository.EventDeliveryRepository;
import com.eventrelay.repository.IncomingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EventProcessingService {

    private final IncomingEventRepository incomingEventRepository;
    private final EventDeliveryRepository eventDeliveryRepository;
    private final DeadLetterService deadLetterService;

    public EventProcessingService(
        IncomingEventRepository incomingEventRepository,
        EventDeliveryRepository eventDeliveryRepository,
        DeadLetterService deadLetterService
    ) {
        this.incomingEventRepository = incomingEventRepository;
        this.eventDeliveryRepository = eventDeliveryRepository;
        this.deadLetterService = deadLetterService;
    }

    // ── Lifecycle API (consumers use these directly from C2 onward) ──────

    /**
     * Load event and mark PROCESSING. Called at the start of each consumer attempt.
     */
    @Transactional
    public IncomingEvent beginProcessing(Long eventPk, String consumerName) {
        IncomingEvent event = incomingEventRepository.findById(eventPk)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventPk));
        event.setStatus(EventStatus.PROCESSING);
        return incomingEventRepository.save(event);
    }

    /**
     * Mark event as successfully processed. Called after domain logic succeeds.
     */
    @Transactional
    public void markSuccess(IncomingEvent event, String consumerName, long startedAt) {
        event.setStatus(EventStatus.PROCESSED);
        event.setProcessedAt(Instant.now());
        incomingEventRepository.save(event);
        recordDelivery(event, consumerName, "success", null, startedAt);
    }

    /**
     * Record a failed attempt and rethrow so Resilience4j (@Retry, @CircuitBreaker) can observe it.
     * Does NOT move to dead letter — that is the consumer fallback method's responsibility.
     */
    @Transactional(noRollbackFor = RuntimeException.class)
    public void recordFailureAttempt(IncomingEvent event, String consumerName, Exception e, long startedAt) {
        event.setRetryCount(event.getRetryCount() + 1);
        event.setStatus(EventStatus.FAILED);
        event.setLastError(e.getMessage());
        incomingEventRepository.save(event);
        recordDelivery(event, consumerName, "failed", e.getMessage(), startedAt);
        throw new RuntimeException("Processing failed for event " + event.getId(), e);
    }

    /**
     * Move event to dead letter. Called from consumer fallback methods after retries are exhausted.
     */
    @Transactional
    public void finalizeDeadLetter(Long eventPk, String errorMessage) {
        IncomingEvent event = incomingEventRepository.findById(eventPk)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventPk));
        if (event.getStatus() != EventStatus.DEAD_LETTER) {
            deadLetterService.moveToDeadLetter(event, errorMessage, "Max retries exceeded");
        }
    }

    // ── Backward-compat entry point (used by consumers until C2) ────────

    /**
     * Monolithic process entry point kept for pre-C2 consumer compatibility.
     * Records failure, handles dead-letter threshold, and rethrows so Resilience4j can observe it.
     */
    @Transactional(noRollbackFor = RuntimeException.class)
    public void process(Long eventPk, String consumerName) {
        long startedAt = System.currentTimeMillis();
        IncomingEvent event = beginProcessing(eventPk, consumerName);
        try {
            // Application-specific delivery logic should be added here.
            markSuccess(event, consumerName, startedAt);
        } catch (Exception e) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setStatus(EventStatus.FAILED);
            event.setLastError(e.getMessage());
            incomingEventRepository.save(event);
            recordDelivery(event, consumerName, "failed", e.getMessage(), startedAt);
            if (event.getRetryCount() >= event.getMaxRetries()) {
                deadLetterService.moveToDeadLetter(event, e.getMessage(), "Max retries exceeded");
            }
            throw new RuntimeException("Processing failed for event " + eventPk, e);
        }
    }

    private void recordDelivery(IncomingEvent event, String consumerName, String status, String errorMessage, long startedAt) {
        EventDelivery delivery = new EventDelivery();
        delivery.setEvent(event);
        delivery.setConsumerName(consumerName);
        delivery.setAttemptNumber(event.getRetryCount() + 1);
        delivery.setStatus(status);
        delivery.setDurationMs((int) (System.currentTimeMillis() - startedAt));
        delivery.setErrorMessage(errorMessage);
        eventDeliveryRepository.save(delivery);
    }
}
