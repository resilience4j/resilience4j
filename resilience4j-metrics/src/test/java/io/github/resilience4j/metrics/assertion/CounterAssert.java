package io.github.resilience4j.metrics.assertion;

import com.codahale.metrics.Counter;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

public class CounterAssert extends AbstractAssert<CounterAssert, Counter> {

    public CounterAssert(Counter actual) {
        super(actual, CounterAssert.class);
    }

    public static CounterAssert assertThat(Counter actual) {
        return new CounterAssert(actual);
    }

    public <T> CounterAssert hasValue(T expected) {
        isNotNull();
        Assertions.assertThat(actual.getCount()).isEqualTo(expected);
        return this;
    }
}