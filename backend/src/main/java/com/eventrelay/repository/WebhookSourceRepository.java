package com.eventrelay.repository;

import com.eventrelay.model.WebhookSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookSourceRepository extends JpaRepository<WebhookSource, Long> {
    Optional<WebhookSource> findByNameAndActiveTrue(String name);
    boolean existsByName(String name);
}
