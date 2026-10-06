package com.peakui.post.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.peakui.post.like.LikeCacheService;
import com.peakui.post.like.LikeKeys;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class LikeRabbitMessageConfig {

    @Bean
    public MessageConverter likeMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate likeRabbitTemplate(ConnectionFactory connectionFactory,
                                              MessageConverter likeMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(likeMessageConverter);
        template.setMandatory(true);
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory likeRabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter likeMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(likeMessageConverter);
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(false);
        factory.setPrefetchCount(20);
        return factory;
    }

    @Bean
    public RedisMessageListenerContainer likeInvalidationContainer(RedisConnectionFactory connectionFactory,
                                                                    LikeCacheService cacheService) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                cacheService.invalidate(Long.parseLong(new String(message.getBody(), java.nio.charset.StandardCharsets.UTF_8)));
            } catch (RuntimeException ignored) {
                // Invalidations are advisory; the L1 TTL remains the safety net.
            }
        }, new ChannelTopic(LikeKeys.INVALIDATIONS));
        return container;
    }
}
