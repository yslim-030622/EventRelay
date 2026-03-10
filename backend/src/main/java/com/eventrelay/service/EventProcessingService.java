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

    @Transactional
    public void process(Long eventPk, String consumerName) {
        IncomingEvent event = incomingEventRepository.findById(eventPk)
            .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventPk));

        long startedAt = System.currentTimeMillis();
        try {
            event.setStatus(EventStatus.PROCESSING);
            incomingEventRepository.save(event);

            // Application-specific delivery logic should be added here.
            event.setStatus(EventStatus.PROCESSED);
            event.setProcessedAt(Instant.now());
            incomingEventRepository.save(event);
            recordDelivery(event, consumerName, "success", null, startedAt);
        } catch (Exception exception) {
            event.setRetryCount(event.getRetryCount() + 1);
            event.setStatus(EventStatus.FAILED);
            event.setLastError(exception.getMessage());
            incomingEventRepository.save(event);
            recordDelivery(event, consumerName, "failed", exception.getMessage(), startedAt);

            if (event.getRetryCount() >= event.getMaxRetries()) {
                deadLetterService.moveToDeadLetter(event, exception.getMessage(), "Max retries exceeded");
            }
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
