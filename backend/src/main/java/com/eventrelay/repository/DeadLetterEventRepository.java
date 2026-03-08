package com.eventrelay.repository;

import com.eventrelay.model.DeadLetterEvent;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEvent, Long> {

    @Override
    @EntityGraph(attributePaths = {"event", "event.source"})
    List<DeadLetterEvent> findAll();

    @Override
    @EntityGraph(attributePaths = {"event", "event.source"})
    Optional<DeadLetterEvent> findById(Long id);
}
