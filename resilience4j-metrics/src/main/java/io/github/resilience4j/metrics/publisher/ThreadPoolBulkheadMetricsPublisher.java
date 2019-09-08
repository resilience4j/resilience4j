package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.bulkhead.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class ThreadPoolBulkheadMetricsPublisher extends AbstractMetricsPublisher<ThreadPoolBulkhead> {

    private final String prefix;

    public ThreadPoolBulkheadMetricsPublisher() {
        this(DEFAULT_PREFIX_THREAD_POOL, new MetricRegistry());
    }

    public ThreadPoolBulkheadMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX_THREAD_POOL, metricRegistry);
    }

    public ThreadPoolBulkheadMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(ThreadPoolBulkhead entry) {
        String name = entry.getName();
        //number of available concurrent calls as an integer
        String currentThreadPoolSize = name(prefix, name, CURRENT_THREAD_POOL_SIZE);
        String availableQueueCapacity = name(prefix, name, AVAILABLE_QUEUE_CAPACITY);

        metricRegistry.register(currentThreadPoolSize, (Gauge<Integer>) () -> entry.getMetrics().getThreadPoolSize());
        metricRegistry.register(availableQueueCapacity, (Gauge<Integer>) () -> entry.getMetrics().getRemainingQueueCapacity());

        List<String> metricNames = Arrays.asList(currentThreadPoolSize, availableQueueCapacity);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(ThreadPoolBulkhead entry) {
        removeMetrics(entry.getName());
    }
}
