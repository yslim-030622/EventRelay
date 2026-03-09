package com.eventrelay.consumer;

import com.eventrelay.service.EventProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GenericEventConsumer {

    private final EventProcessingService eventProcessingService;
    private final String genericQueueName;

    public GenericEventConsumer(
        EventProcessingService eventProcessingService,
        @Value("${app.messaging.generic-queue-name}") String genericQueueName
    ) {
        this.eventProcessingService = eventProcessingService;
        this.genericQueueName = genericQueueName;
    }

    @RabbitListener(queues = "${app.messaging.generic-queue-name}")
    public void consume(Long eventPk) {
        eventProcessingService.process(eventPk, "generic-consumer");
    }

    public String getGenericQueueName() {
        return genericQueueName;
    }
}
