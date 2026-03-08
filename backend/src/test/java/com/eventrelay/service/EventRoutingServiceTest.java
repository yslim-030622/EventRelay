package com.eventrelay.service;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EventRoutingServiceTest {

    @Test
    void publishUsesConfiguredExchangeAndRoutingKey() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        EventRoutingService eventRoutingService = new EventRoutingService(rabbitTemplate, "events.exchange");

        eventRoutingService.publish(42L, "github.push");

        verify(rabbitTemplate).convertAndSend("events.exchange", "github.push", 42L);
    }
}
