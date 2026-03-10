package com.eventrelay.consumer;

import com.eventrelay.service.EventProcessingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

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
    @CircuitBreaker(name = "paymentConsumer")
    @Retry(name = "paymentConsumerRetry")
    public void consume(Long eventPk) {
        eventProcessingService.process(eventPk, "payment-consumer");
    }

    public String getPaymentQueueName() {
        return paymentQueueName;
    }
}
