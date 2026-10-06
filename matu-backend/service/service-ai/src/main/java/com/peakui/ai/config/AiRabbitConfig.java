package com.peakui.ai.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiRabbitConfig {
    public static final String EXCHANGE = "ai.task.exchange";
    public static final String QUEUE = "ai.task.queue";
    public static final String ROUTING_KEY = "ai.task.execute";
    public static final String DLX = "ai.task.dlx";
    public static final String DLQ = "ai.task.dlq";

    @Bean
    public DirectExchange aiTaskExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue aiTaskQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Binding aiTaskBinding() {
        return BindingBuilder.bind(aiTaskQueue()).to(aiTaskExchange()).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange aiTaskDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue aiTaskDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding aiTaskDlqBinding() {
        return BindingBuilder.bind(aiTaskDlq()).to(aiTaskDlx()).with(DLQ);
    }
}
