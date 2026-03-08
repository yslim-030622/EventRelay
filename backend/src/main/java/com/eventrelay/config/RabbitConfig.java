package com.eventrelay.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.messaging.exchange-name}")
    private String exchangeName;

    @Value("${app.messaging.github-queue-name}")
    private String githubQueueName;

    @Value("${app.messaging.generic-queue-name}")
    private String genericQueueName;

    @Value("${app.messaging.dead-letter-queue-name}")
    private String deadLetterQueueName;

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(exchangeName);
    }

    @Bean
    public Queue githubQueue() {
        return QueueBuilder.durable(githubQueueName)
            .withArgument("x-dead-letter-exchange", exchangeName)
            .withArgument("x-dead-letter-routing-key", "dead-letter")
            .build();
    }

    @Bean
    public Queue genericQueue() {
        return QueueBuilder.durable(genericQueueName)
            .withArgument("x-dead-letter-exchange", exchangeName)
            .withArgument("x-dead-letter-routing-key", "dead-letter")
            .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueueName).build();
    }

    @Bean
    public Binding githubBinding() {
        return BindingBuilder.bind(githubQueue()).to(eventExchange()).with("github.#");
    }

    @Bean
    public Binding genericBinding() {
        return BindingBuilder.bind(genericQueue()).to(eventExchange()).with("generic.#");
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(eventExchange()).with("dead-letter");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        rabbitTemplate.setExchange(exchangeName);
        return rabbitTemplate;
    }
}
