package com.eventrelay.controller;

import com.eventrelay.dto.EventDetailResponse;
import com.eventrelay.dto.EventResponse;
import com.eventrelay.service.EventQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventQueryService eventQueryService;

    public EventController(EventQueryService eventQueryService) {
        this.eventQueryService = eventQueryService;
    }

    @GetMapping
    public Page<EventResponse> listEvents(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String source,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return eventQueryService.list(status, source, pageable).map(EventResponse::from);
    }

    @GetMapping("/{id}")
    public EventDetailResponse getEvent(@PathVariable Long id) {
        return EventDetailResponse.from(eventQueryService.getById(id));
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<EventDetailResponse> replayEvent(@PathVariable Long id) {
        return ResponseEntity.accepted().body(EventDetailResponse.from(eventQueryService.replay(id)));
    }
}
