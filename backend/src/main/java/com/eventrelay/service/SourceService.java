package com.eventrelay.service;

import com.eventrelay.dto.SourceRequest;
import com.eventrelay.model.WebhookSource;
import com.eventrelay.repository.WebhookSourceRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SourceService {

    private final WebhookSourceRepository sourceRepository;

    public SourceService(WebhookSourceRepository sourceRepository) {
        this.sourceRepository = sourceRepository;
    }

    public List<WebhookSource> findAll() {
        return sourceRepository.findAll();
    }

    public WebhookSource findActiveSource(String sourceName) {
        return sourceRepository.findByNameAndActiveTrue(sourceName)
            .orElseThrow(() -> new EntityNotFoundException("Unknown webhook source: " + sourceName));
    }

    @Transactional
    public WebhookSource create(SourceRequest request) {
        if (sourceRepository.existsByName(request.name())) {
            throw new IllegalArgumentException("Source already exists: " + request.name());
        }

        WebhookSource source = new WebhookSource();
        source.setName(request.name());
        source.setDisplayName(request.displayName());
        source.setSigningSecret(request.signingSecret());
        source.setActive(true);
        return sourceRepository.save(source);
    }
}
