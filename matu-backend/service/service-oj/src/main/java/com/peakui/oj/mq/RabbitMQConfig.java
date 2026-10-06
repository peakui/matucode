package com.peakui.oj.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {
    public static final String JUDGE_EXCHANGE = "oj.judge.exchange";
    public static final String JUDGE_QUEUE = "oj.judge.queue";
    public static final String JUDGE_ROUTING_KEY = "oj.judge.submit";
    public static final String JUDGE_DLX = "oj.judge.dlx";
    public static final String JUDGE_DLQ = "oj.judge.dlq";
    public static final String JUDGE_DLQ_ROUTING_KEY = "oj.judge.dlq.routing";

    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(JUDGE_EXCHANGE, true, false);
    }

    @Bean
    public Queue judgeQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", JUDGE_DLX);
        arguments.put("x-dead-letter-routing-key", JUDGE_DLQ_ROUTING_KEY);
        return QueueBuilder.durable(JUDGE_QUEUE).withArguments(arguments).build();
    }

    @Bean
    public Binding judgeBinding(@Qualifier("judgeQueue") Queue judgeQueue,
                                 @Qualifier("judgeExchange") DirectExchange judgeExchange) {
        return BindingBuilder.bind(judgeQueue).to(judgeExchange).with(JUDGE_ROUTING_KEY);
    }

    @Bean
    public DirectExchange judgeDlx() {
        return new DirectExchange(JUDGE_DLX, true, false);
    }

    @Bean
    public Queue judgeDlq() {
        return QueueBuilder.durable(JUDGE_DLQ).build();
    }

    @Bean
    public Binding judgeDlqBinding(@Qualifier("judgeDlq") Queue judgeDlq,
                                    @Qualifier("judgeDlx") DirectExchange judgeDlx) {
        return BindingBuilder.bind(judgeDlq).to(judgeDlx).with(JUDGE_DLQ_ROUTING_KEY);
    }
}
