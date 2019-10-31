package io.github.resilience4j.prometheus;

import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;

class CallMeterChildImpl implements CallMeter.Child {

    private final Histogram.Child histogram;
    private final Counter.Child totalCounter;
    private final Counter.Child errorCounter;

    CallMeterChildImpl(Histogram.Child histogram, Counter.Child totalCounter,
        Counter.Child errorCounter) {
        this.histogram = histogram;
        this.totalCounter = totalCounter;
        this.errorCounter = errorCounter;
    }

    @Override
    public Timer startTimer() {
        final Histogram.Timer timer = histogram.startTimer();
        totalCounter.inc();

        return new Timer() {
            @Override
            public void onError() {
                errorCounter.inc();
            }

            @Override
            public void onSuccess() {
                timer.observeDuration();
            }
        };
    }
}
