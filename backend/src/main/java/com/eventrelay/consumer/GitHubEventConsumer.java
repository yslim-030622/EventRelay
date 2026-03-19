package com.eventrelay.consumer;

import com.eventrelay.model.IncomingEvent;
import com.eventrelay.service.EventProcessingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GitHubEventConsumer {

    private static final String CONSUMER_NAME = "github-consumer";

    private final EventProcessingService eventProcessingService;
    private final String githubQueueName;

    public GitHubEventConsumer(
        EventProcessingService eventProcessingService,
        @Value("${app.messaging.github-queue-name}") String githubQueueName
    ) {
        this.eventProcessingService = eventProcessingService;
        this.githubQueueName = githubQueueName;
    }

    @RabbitListener(queues = "${app.messaging.github-queue-name}")
    @CircuitBreaker(name = "githubConsumer")
    @Retry(name = "githubConsumerRetry", fallbackMethod = "fallback")
    public void consume(Long eventPk) {
        long startedAt = System.currentTimeMillis();
        IncomingEvent event = eventProcessingService.beginProcessing(eventPk, CONSUMER_NAME);
        try {
            Map<String, Object> payload = event.getPayload();
            if (payload != null && Boolean.TRUE.equals(payload.get("_forceFailure"))) {
                throw new RuntimeException("Forced failure for testing");
            }
            eventProcessingService.markSuccess(event, CONSUMER_NAME, startedAt);
        } catch (Exception e) {
            eventProcessingService.recordFailureAttempt(event, CONSUMER_NAME, e, startedAt);
        }
    }

    public void fallback(Long eventPk, Throwable e) {
        eventProcessingService.finalizeDeadLetter(eventPk, e.getMessage());
    }

    public String getGithubQueueName() {
        return githubQueueName;
    }
}
