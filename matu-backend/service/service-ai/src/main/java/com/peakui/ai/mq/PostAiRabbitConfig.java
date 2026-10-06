package com.peakui.ai.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostAiRabbitConfig {
    public static final String EXCHANGE = "post.ai.exchange";
    public static final String QUEUE = "post.ai.queue";
    public static final String ROUTING_KEY = "post.published";
    public static final String DLX = "post.ai.dlx";
    public static final String DLQ = "post.ai.dlq";

    @Bean
    public DirectExchange postAiExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue postAiQueue() {
        return QueueBuilder.durable(QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", DLQ)
                .build();
    }

    @Bean
    public Binding postAiBinding() {
        return BindingBuilder.bind(postAiQueue()).to(postAiExchange()).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange postAiDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue postAiDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding postAiDlqBinding() {
        return BindingBuilder.bind(postAiDlq()).to(postAiDlx()).with(DLQ);
    }
}
