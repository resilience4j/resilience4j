package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;

import java.util.Map;
import java.util.function.Supplier;

public record ThreadPoolBulkheadAdapter(ThreadPoolBulkhead threadPoolBulkhead) implements Bulkhead {

    @Override
    public <T> T executeSupplier(Supplier<T> supplier) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getName() {
        return threadPoolBulkhead.getName();
    }

    @Override
    public void changeConfig(BulkheadConfig newConfig) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryAcquirePermission() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void acquirePermission() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void releasePermission() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void onComplete() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BulkheadConfig getBulkheadConfig() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Metrics getMetrics() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> getTags() {
        return threadPoolBulkhead.getTags();
    }

    @Override
    public EventPublisher getEventPublisher() {
        return (EventPublisher) threadPoolBulkhead.getEventPublisher();
    }
}
