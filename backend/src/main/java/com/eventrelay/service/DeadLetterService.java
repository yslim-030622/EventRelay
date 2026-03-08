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

    public DeadLetterService(
        DeadLetterEventRepository deadLetterEventRepository,
        IncomingEventRepository incomingEventRepository
    ) {
        this.deadLetterEventRepository = deadLetterEventRepository;
        this.incomingEventRepository = incomingEventRepository;
    }

    public List<DeadLetterEvent> findAll() {
        return deadLetterEventRepository.findAll();
    }

    @Transactional
    public DeadLetterEvent moveToDeadLetter(IncomingEvent event, String errorMessage) {
        event.setStatus(EventStatus.DEAD_LETTER);
        event.setLastError(errorMessage);
        incomingEventRepository.save(event);

        DeadLetterEvent deadLetterEvent = new DeadLetterEvent();
        deadLetterEvent.setEvent(event);
        deadLetterEvent.setErrorMessage(errorMessage);
        return deadLetterEventRepository.save(deadLetterEvent);
    }

    @Transactional
    public void markReplayed(Long deadLetterId) {
        DeadLetterEvent deadLetterEvent = deadLetterEventRepository.findById(deadLetterId)
            .orElseThrow(() -> new EntityNotFoundException("Dead-letter event not found: " + deadLetterId));

        deadLetterEvent.setReplayed(true);
        deadLetterEvent.setReplayedAt(Instant.now());
        deadLetterEvent.getEvent().setStatus(EventStatus.RECEIVED);
        deadLetterEvent.getEvent().setLastError(null);
    }
}
