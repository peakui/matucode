package com.peakui.auth.mini;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class WechatTicketStoreTest {
    @Test @SuppressWarnings("unchecked") void onlyOneConcurrentConsumerReceivesIdentity() throws Exception {
        var values = new ConcurrentHashMap<String,String>();
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String,String> ops = mock(ValueOperations.class); when(redis.opsForValue()).thenReturn(ops);
        doAnswer(inv -> { values.put(inv.getArgument(0), inv.getArgument(1)); return null; }).when(ops).set(anyString(), anyString(), any(Duration.class));
        when(ops.getAndDelete(anyString())).thenAnswer(inv -> values.remove(inv.getArgument(0)));
        var store = new WechatTicketStore(redis, new ObjectMapper());
        String ticket = store.create(new MiniModels.WechatIdentity("app","openid",null));
        assertFalse(values.keySet().iterator().next().contains(ticket));
        var successes = new AtomicInteger(); var executor = Executors.newFixedThreadPool(8);
        try {
            var tasks = java.util.stream.IntStream.range(0, 16).<java.util.concurrent.Callable<Void>>mapToObj(i -> () -> {
                try { store.consume(ticket); successes.incrementAndGet(); } catch (RuntimeException expected) {} return null;
            }).toList();
            for (var future : executor.invokeAll(tasks)) future.get();
        } finally { executor.shutdownNow(); }
        assertEquals(1, successes.get());
    }
}
