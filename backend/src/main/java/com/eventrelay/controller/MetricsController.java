package com.eventrelay.controller;

import com.eventrelay.dto.MetricsSummaryResponse;
import com.eventrelay.service.MetricsService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public MetricsController(MetricsService metricsService, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.metricsService = metricsService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
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

    @GetMapping("/circuit-breaker")
    public List<Map<String, Object>> circuitBreakerStatus() {
        return circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .map(cb -> {
                CircuitBreaker.Metrics m = cb.getMetrics();
                return Map.<String, Object>of(
                    "name", cb.getName(),
                    "state", cb.getState().name(),
                    "failureRate", m.getFailureRate(),
                    "bufferedCalls", m.getNumberOfBufferedCalls(),
                    "failedCalls", m.getNumberOfFailedCalls(),
                    "successfulCalls", m.getNumberOfSuccessfulCalls(),
                    "notPermittedCalls", m.getNumberOfNotPermittedCalls()
                );
            })
            .toList();
    }
}
