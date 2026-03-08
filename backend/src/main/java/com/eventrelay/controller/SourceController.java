package com.eventrelay.controller;

import com.eventrelay.dto.SourceRequest;
import com.eventrelay.dto.SourceResponse;
import com.eventrelay.service.SourceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sources")
public class SourceController {

    private final SourceService sourceService;

    public SourceController(SourceService sourceService) {
        this.sourceService = sourceService;
    }

    @GetMapping
    public List<SourceResponse> listSources() {
        return sourceService.findAll().stream().map(SourceResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<SourceResponse> createSource(@Valid @RequestBody SourceRequest request) {
        return ResponseEntity.ok(SourceResponse.from(sourceService.create(request)));
    }
}
