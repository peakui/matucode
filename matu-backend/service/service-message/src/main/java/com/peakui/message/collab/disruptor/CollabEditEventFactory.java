package com.peakui.message.collab.disruptor;

import com.lmax.disruptor.EventFactory;
import org.springframework.stereotype.Component;

@Component
public class CollabEditEventFactory implements EventFactory<CollabEditEvent> {

    @Override
    public CollabEditEvent newInstance() {
        return new CollabEditEvent();
    }
}
