package io.github.robwin.cache.consumer;

import io.github.robwin.cache.event.CacheEvent;
import io.github.robwin.circuitbreaker.internal.CircularFifoBuffer;
import io.reactivex.functions.Consumer;
import javaslang.collection.List;

/**
 * A RxJava consumer which stores CircuitBreakerEvents in a circular buffer with a fixed capacity.
 */
public class CacheEventConsumer<T extends  CacheEvent> implements Consumer<T> {

    private CircularFifoBuffer<T> eventCircularFifoBuffer;

    /**
     * Creates an {@code CacheEventConsumer} with the given (fixed)
     * capacity
     *
     * @param capacity the capacity of this CacheEventConsumer
     * @throws IllegalArgumentException if {@code capacity < 1}
     */
    public CacheEventConsumer(int capacity) {
        this.eventCircularFifoBuffer = new CircularFifoBuffer<>(capacity);
    }

    @Override
    public void accept(T cacheEvent) throws Exception {
        eventCircularFifoBuffer.add(cacheEvent);
    }

    /**
     * Returns a list containing all of the buffered events.
     *
     * @return a list containing all of the buffered events.
     */
    public List<T> getBufferedCacheEvents(){
        return eventCircularFifoBuffer.toList();
    }
}
