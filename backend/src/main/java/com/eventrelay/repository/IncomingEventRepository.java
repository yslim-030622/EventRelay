package com.eventrelay.repository;

import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IncomingEventRepository extends JpaRepository<IncomingEvent, Long> {

    @Override
    @EntityGraph(attributePaths = {"source"})
    List<IncomingEvent> findAll();

    @Override
    @EntityGraph(attributePaths = {"source"})
    Page<IncomingEvent> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"source"})
    Page<IncomingEvent> findByStatus(EventStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"source"})
    Page<IncomingEvent> findBySource_Name(String sourceName, Pageable pageable);

    @EntityGraph(attributePaths = {"source"})
    Page<IncomingEvent> findByStatusAndSource_Name(EventStatus status, String sourceName, Pageable pageable);

    @EntityGraph(attributePaths = {"source", "deliveries"})
    Optional<IncomingEvent> findDetailedById(Long id);

    Optional<IncomingEvent> findByEventId(String eventId);

    long countByStatus(EventStatus status);
}
