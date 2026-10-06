package com.peakui.message.collab.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import java.util.concurrent.ThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CollabDisruptorConfig {

    @Bean(destroyMethod = "shutdown")
    public Disruptor<CollabEditEvent> collabEditDisruptor(CollabEditEventFactory eventFactory,
                                                          CollabEditEventHandler eventHandler) {
        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName("collab-edit-disruptor");
            thread.setDaemon(true);
            return thread;
        };
        Disruptor<CollabEditEvent> disruptor = new Disruptor<>(eventFactory, 4096, threadFactory,
                ProducerType.MULTI, new BlockingWaitStrategy());
        disruptor.handleEventsWith(eventHandler);
        disruptor.start();
        return disruptor;
    }

    @Bean
    public RingBuffer<CollabEditEvent> collabEditRingBuffer(Disruptor<CollabEditEvent> collabEditDisruptor) {
        return collabEditDisruptor.getRingBuffer();
    }
}
