package com.eventrelay.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EventRoutingService {

    private final RabbitTemplate rabbitTemplate;
    private final String exchangeName;

    public EventRoutingService(
        RabbitTemplate rabbitTemplate,
        @Value("${app.messaging.exchange-name}") String exchangeName
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchangeName = exchangeName;
    }

    public void publish(Long eventPk, String routingKey) {
        rabbitTemplate.convertAndSend(exchangeName, routingKey, eventPk);
    }

    public int getMaxRetries(String eventType) {
        if (eventType == null) {
            return 5;
        }
        if (eventType.startsWith("payment.")) {
            return 10;
        }
        if (eventType.startsWith("github.push")) {
            return 3;
        }
        if (eventType.startsWith("github.pull_request") || eventType.startsWith("github.issues")) {
            return 3;
        }
        return 5;
    }
}
