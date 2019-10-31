package io.github.resilience4j.prometheus;


import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;

import static java.util.Objects.requireNonNull;

class CallMeterImpl implements CallMeter {

    private final CallCollectors collectors;

    CallMeterImpl(CallCollectors collectors) {
        requireNonNull(collectors);
        this.collectors = collectors;
    }

    @Override
    public Child labels(String... labels) {
        return new CallMeterChildImpl(
            collectors.histogram.labels(labels),
            collectors.totalCounter.labels(labels),
            collectors.errorCounter.labels(labels));
    }

    @Override
    public Timer startTimer() {

        final Histogram.Timer timer = collectors.histogram.startTimer();
        collectors.totalCounter.inc();

        return new Timer() {
            @Override
            public void onError() {
                collectors.errorCounter.inc();
            }

            @Override
            public void onSuccess() {
                timer.observeDuration();
            }
        };
    }

    @Override
    public CallMeter register(CollectorRegistry registry) {
        registry.register(collectors.histogram);
        registry.register(collectors.totalCounter);
        registry.register(collectors.errorCounter);
        return this;
    }
}
