package io.github.resilience4j.metrics.assertion;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import com.codahale.metrics.MetricRegistry;

public class MetricRegistryAssert extends AbstractAssert<MetricRegistryAssert, MetricRegistry> {

    public MetricRegistryAssert(MetricRegistry actual) {
        super(actual, MetricRegistryAssert.class);
    }

    public static MetricRegistryAssert assertThat(MetricRegistry actual) {
        return new MetricRegistryAssert(actual);
    }

    public MetricRegistryAssert hasMetricsSize(int size) {
        isNotNull();
        Assertions.assertThat(actual.getMetrics())
                .hasSize(size);
        return this;
    }

    public CounterAssert counter(String name) {
        isNotNull();
        Assertions.assertThat(actual.getCounters()).containsKey(name);
        return CounterAssert.assertThat(actual.getCounters().get(name));
    }
}