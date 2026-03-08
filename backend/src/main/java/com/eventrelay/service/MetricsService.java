package com.eventrelay.service;

import com.eventrelay.dto.MetricsSummaryResponse;
import com.eventrelay.model.EventStatus;
import com.eventrelay.model.IncomingEvent;
import com.eventrelay.repository.IncomingEventRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private final IncomingEventRepository incomingEventRepository;

    public MetricsService(IncomingEventRepository incomingEventRepository) {
        this.incomingEventRepository = incomingEventRepository;
    }

    public MetricsSummaryResponse summary() {
        return new MetricsSummaryResponse(
            incomingEventRepository.count(),
            incomingEventRepository.countByStatus(EventStatus.RECEIVED),
            incomingEventRepository.countByStatus(EventStatus.PROCESSING),
            incomingEventRepository.countByStatus(EventStatus.PROCESSED),
            incomingEventRepository.countByStatus(EventStatus.FAILED),
            incomingEventRepository.countByStatus(EventStatus.DEAD_LETTER)
        );
    }

    public List<Map<String, Object>> bySource() {
        Map<String, long[]> grouped = new LinkedHashMap<>();
        for (IncomingEvent event : incomingEventRepository.findAll()) {
            String source = event.getSource().getName();
            long[] counts = grouped.computeIfAbsent(source, ignored -> new long[3]);
            counts[0]++;
            if (event.getStatus() == EventStatus.PROCESSED) {
                counts[1]++;
            }
            if (event.getStatus() == EventStatus.DEAD_LETTER) {
                counts[2]++;
            }
        }

        List<Map<String, Object>> response = new ArrayList<>();
        grouped.forEach((source, counts) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("source", source);
            row.put("total", counts[0]);
            row.put("processed", counts[1]);
            row.put("deadLetter", counts[2]);
            response.add(row);
        });
        return response;
    }

    public List<Map<String, Object>> byType() {
        Map<String, Long> grouped = new LinkedHashMap<>();
        for (IncomingEvent event : incomingEventRepository.findAll()) {
            grouped.merge(event.getEventType(), 1L, Long::sum);
        }

        List<Map<String, Object>> response = new ArrayList<>();
        grouped.forEach((type, total) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("eventType", type);
            row.put("total", total);
            response.add(row);
        });
        return response;
    }
}
