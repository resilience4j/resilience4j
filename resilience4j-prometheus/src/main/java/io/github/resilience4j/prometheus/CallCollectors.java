package io.github.resilience4j.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

final class CallCollectors {
    final Histogram histogram;
    final Counter totalCounter;
    final Counter errorCounter;

    CallCollectors(Histogram histogram, Counter totalCounter, Counter errorCounter) {
        this.histogram = histogram;
        this.totalCounter = totalCounter;
        this.errorCounter = errorCounter;
    }
}
