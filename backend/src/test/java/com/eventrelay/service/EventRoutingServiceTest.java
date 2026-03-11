package com.eventrelay.service;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void getMaxRetriesReturns10ForPaymentEvents() {
        EventRoutingService service = new EventRoutingService(mock(RabbitTemplate.class), "events.exchange");
        assertThat(service.getMaxRetries("payment.charge.succeeded")).isEqualTo(10);
    }

    @Test
    void getMaxRetriesReturns3ForPushEvents() {
        EventRoutingService service = new EventRoutingService(mock(RabbitTemplate.class), "events.exchange");
        assertThat(service.getMaxRetries("github.push")).isEqualTo(3);
    }

    @Test
    void getMaxRetriesReturns5ForUnknownEvents() {
        EventRoutingService service = new EventRoutingService(mock(RabbitTemplate.class), "events.exchange");
        assertThat(service.getMaxRetries("generic.something")).isEqualTo(5);
    }
}
