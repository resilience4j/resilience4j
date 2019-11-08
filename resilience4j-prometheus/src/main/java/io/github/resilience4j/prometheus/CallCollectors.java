package io.github.resilience4j.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

final class CallCollectors {

    public final Histogram histogram;
    public final Counter totalCounter;
    public final Counter errorCounter;

    CallCollectors(Histogram histogram, Counter totalCounter, Counter errorCounter) {
        this.histogram = histogram;
        this.totalCounter = totalCounter;
        this.errorCounter = errorCounter;
    }
}
