package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.ratelimiter.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class RateLimiterMetricsPublisher extends AbstractMetricsPublisher<RateLimiter> {

    private final String prefix;

    public RateLimiterMetricsPublisher() {
        this(DEFAULT_PREFIX, new MetricRegistry());
    }

    public RateLimiterMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX, metricRegistry);
    }

    public RateLimiterMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(RateLimiter entry) {
        String name = entry.getName();

        String waitingThreads = name(prefix, name, WAITING_THREADS);
        String availablePermissions = name(prefix, name, AVAILABLE_PERMISSIONS);

        metricRegistry.register(waitingThreads, (Gauge<Integer>) entry.getMetrics()::getNumberOfWaitingThreads);
        metricRegistry.register(availablePermissions, (Gauge<Integer>) entry.getMetrics()::getAvailablePermissions);

        List<String> metricNames = Arrays.asList(waitingThreads, availablePermissions);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(RateLimiter entry) {
        removeMetrics(entry.getName());
    }
}
