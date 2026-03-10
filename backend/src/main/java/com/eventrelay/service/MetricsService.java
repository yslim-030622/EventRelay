package com.eventrelay.service;

import com.eventrelay.dto.MetricsSummaryResponse;
import com.eventrelay.model.EventStatus;
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
        for (Object[] row : incomingEventRepository.countBySourceAndStatusGrouped()) {
            String source = (String) row[0];
            EventStatus status = (EventStatus) row[1];
            long count = (Long) row[2];
            long[] counts = grouped.computeIfAbsent(source, ignored -> new long[3]);
            counts[0] += count;
            if (status == EventStatus.PROCESSED) counts[1] += count;
            if (status == EventStatus.DEAD_LETTER) counts[2] += count;
        }

        List<Map<String, Object>> response = new ArrayList<>();
        grouped.forEach((source, counts) -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("source", source);
            result.put("total", counts[0]);
            result.put("processed", counts[1]);
            result.put("deadLetter", counts[2]);
            response.add(result);
        });
        return response;
    }

    public List<Map<String, Object>> byType() {
        List<Map<String, Object>> response = new ArrayList<>();
        for (Object[] row : incomingEventRepository.countByTypeGrouped()) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("eventType", row[0]);
            result.put("total", row[1]);
            response.add(result);
        }
        return response;
    }
}
