package io.github.resilience4j.metrics.assertion;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import com.codahale.metrics.Gauge;

public class GaugeAssert extends AbstractAssert<GaugeAssert, Gauge> {

    public GaugeAssert(Gauge actual) {
        super(actual, GaugeAssert.class);
    }

    public static GaugeAssert assertThat(Gauge actual) {
        return new GaugeAssert(actual);
    }

    public <T> GaugeAssert hasValue(T expected) {
        isNotNull();
        Assertions.assertThat(actual.getValue())
                .isEqualTo(expected);
        return this;
    }
}