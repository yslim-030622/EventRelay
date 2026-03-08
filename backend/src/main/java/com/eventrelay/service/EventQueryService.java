package com.eventrelay.service;

import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import com.eventrelay.repository.IncomingEventRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class EventQueryService {

    private final IncomingEventRepository incomingEventRepository;

    public EventQueryService(IncomingEventRepository incomingEventRepository) {
        this.incomingEventRepository = incomingEventRepository;
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
