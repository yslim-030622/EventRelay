package com.eventrelay.controller;

import com.eventrelay.dto.DeadLetterResponse;
import com.eventrelay.service.DeadLetterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dead-letters")
public class DeadLetterController {

    private final DeadLetterService deadLetterService;

    public DeadLetterController(DeadLetterService deadLetterService) {
        this.deadLetterService = deadLetterService;
    }

    @GetMapping
    public List<DeadLetterResponse> listDeadLetters() {
        return deadLetterService.findAll().stream().map(DeadLetterResponse::from).toList();
    }

    @PostMapping("/{id}/replay")
    public ResponseEntity<Map<String, Object>> replayDeadLetter(@PathVariable Long id) {
        deadLetterService.markReplayed(id);
        return ResponseEntity.accepted().body(Map.of(
            "id", id,
            "status", "queued",
            "message", "Replay pipeline hook is ready for implementation"
        ));
    }
}
