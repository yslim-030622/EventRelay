package com.eventrelay.controller;

import com.eventrelay.dto.MetricsSummaryResponse;
import com.eventrelay.service.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/summary")
    public MetricsSummaryResponse summary() {
        return metricsService.summary();
    }

    @GetMapping("/by-source")
    public List<Map<String, Object>> bySource() {
        return metricsService.bySource();
    }

    @GetMapping("/by-type")
    public List<Map<String, Object>> byType() {
        return metricsService.byType();
    }
}
