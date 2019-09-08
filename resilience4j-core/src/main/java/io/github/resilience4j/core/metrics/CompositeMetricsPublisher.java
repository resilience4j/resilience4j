package io.github.resilience4j.core.metrics;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class CompositeMetricsPublisher<E> implements MetricsPublisher<E> {

    private final List<MetricsPublisher<E>> delegates;

    public CompositeMetricsPublisher() {
        this.delegates = new ArrayList<>();
    }

    public CompositeMetricsPublisher(List<MetricsPublisher<E>> delegates) {
        this.delegates = new ArrayList<>(requireNonNull(delegates));
    }

    public void addMetricsPublisher(MetricsPublisher<E> metricsPublisher) {
        delegates.add(requireNonNull(metricsPublisher));
    }

    @Override
    public void publishMetrics(E entry) {
        delegates.forEach(publisher -> publisher.publishMetrics(entry));
    }

    @Override
    public void removeMetrics(E entry) {
        delegates.forEach(publisher -> publisher.removeMetrics(entry));
    }

}
