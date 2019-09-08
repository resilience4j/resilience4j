package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.timelimiter.utils.MetricNames.*;
import static io.github.resilience4j.timelimiter.utils.MetricNames.TIMEOUT;
import static java.util.Objects.requireNonNull;

public class TimeLimiterMetricsPublisher extends AbstractMetricsPublisher<TimeLimiter> {

    private final String prefix;

    public TimeLimiterMetricsPublisher() {
        this(DEFAULT_PREFIX, new MetricRegistry());
    }

    public TimeLimiterMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX, metricRegistry);
    }

    public TimeLimiterMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(TimeLimiter entry) {

        String name = entry.getName();
        String successfulName = name(prefix, name, SUCCESSFUL);
        String failedName = name(prefix, name, FAILED);
        String timeoutName = name(prefix, name, TIMEOUT);

        Counter successes = metricRegistry.counter(successfulName);
        Counter failures = metricRegistry.counter(failedName);
        Counter timeouts = metricRegistry.counter(timeoutName);

        entry.getEventPublisher().onSuccess(event -> successes.inc());
        entry.getEventPublisher().onError(event -> failures.inc());
        entry.getEventPublisher().onTimeout(event -> timeouts.inc());

        List<String> metricNames = Arrays.asList(successfulName, failedName, timeoutName);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(TimeLimiter entry) {
        removeMetrics(entry.getName());
    }
}
