package com.peakui.post.mq;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Both producer and consumer declare identical durable topology, independent of startup order. */
@Configuration
public class PostAiRabbitConfig {
    public static final String EXCHANGE = "post.ai.exchange";
    public static final String ROUTING_KEY = "post.published";

    @Bean
    public Declarables postAiTopology() {
        DirectExchange exchange = new DirectExchange(EXCHANGE, true, false);
        DirectExchange dlx = new DirectExchange("post.ai.dlx", true, false);
        Queue queue = QueueBuilder.durable("post.ai.queue")
                .deadLetterExchange("post.ai.dlx").deadLetterRoutingKey("post.ai.dlq").build();
        Queue dead = QueueBuilder.durable("post.ai.dlq").build();
        return new Declarables(exchange, dlx, queue, dead,
                BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY),
                BindingBuilder.bind(dead).to(dlx).with("post.ai.dlq"));
    }
}
