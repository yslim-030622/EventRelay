package com.eventrelay.consumer;

import com.eventrelay.service.EventProcessingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GitHubEventConsumer {

    private final EventProcessingService eventProcessingService;
    private final String githubQueueName;

    public GitHubEventConsumer(
        EventProcessingService eventProcessingService,
        @Value("${app.messaging.github-queue-name}") String githubQueueName
    ) {
        this.eventProcessingService = eventProcessingService;
        this.githubQueueName = githubQueueName;
    }

    @RabbitListener(queues = "#{@githubEventConsumer.githubQueueName}")
    @CircuitBreaker(name = "githubConsumer")
    @Retry(name = "githubConsumerRetry")
    public void consume(Long eventPk) {
        eventProcessingService.process(eventPk, "github-consumer");
    }

    public String getGithubQueueName() {
        return githubQueueName;
    }
}
