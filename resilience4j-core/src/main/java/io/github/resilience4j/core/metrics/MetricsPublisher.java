package io.github.resilience4j.core.metrics;

public interface MetricsPublisher<E> {

    void publishMetrics(E entry);

    void removeMetrics(E entry);
}
