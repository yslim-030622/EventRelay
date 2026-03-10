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
public class PaymentEventConsumer {

    private static final String CONSUMER_NAME = "payment-consumer";

    private final EventProcessingService eventProcessingService;
    private final String paymentQueueName;

    public PaymentEventConsumer(
        EventProcessingService eventProcessingService,
        @Value("${app.messaging.payment-queue-name}") String paymentQueueName
    ) {
        this.eventProcessingService = eventProcessingService;
        this.paymentQueueName = paymentQueueName;
    }

    @RabbitListener(queues = "${app.messaging.payment-queue-name}")
    @CircuitBreaker(name = "paymentConsumer", fallbackMethod = "fallback")
    @Retry(name = "paymentConsumerRetry")
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

    public String getPaymentQueueName() {
        return paymentQueueName;
    }
}
