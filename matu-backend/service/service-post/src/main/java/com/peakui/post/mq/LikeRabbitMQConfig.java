package com.peakui.post.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class LikeRabbitMQConfig {
    public static final String EXCHANGE = "post.like.exchange";
    public static final String QUEUE = "post.like.queue";
    public static final String ROUTING_KEY = "post.like.changed";
    public static final String DLX = "post.like.dlx";
    public static final String DLQ = "post.like.dlq";
    public static final String DLQ_ROUTING_KEY = "post.like.dead";

    @Bean
    public DirectExchange postLikeExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue postLikeQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", DLX);
        arguments.put("x-dead-letter-routing-key", DLQ_ROUTING_KEY);
        return QueueBuilder.durable(QUEUE).withArguments(arguments).build();
    }

    @Bean
    public Binding postLikeBinding() {
        return BindingBuilder.bind(postLikeQueue()).to(postLikeExchange()).with(ROUTING_KEY);
    }

    @Bean
    public DirectExchange postLikeDlx() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue postLikeDlq() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding postLikeDlqBinding() {
        return BindingBuilder.bind(postLikeDlq()).to(postLikeDlx()).with(DLQ_ROUTING_KEY);
    }
}
