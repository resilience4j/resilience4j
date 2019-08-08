package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics;

import java.util.concurrent.atomic.LongAdder;

public class SlidingWindowCircuitBreakerMetrics implements CircuitBreaker.Metrics {

    private final SlidingTimeWindowMetrics metrics;
    private final LongAdder numberOfNotPermittedCalls;

    public SlidingWindowCircuitBreakerMetrics(int timeWindowSizeInSeconds){
        this.metrics = new SlidingTimeWindowMetrics(timeWindowSizeInSeconds);
        this.numberOfNotPermittedCalls = new LongAdder();
    }

    /**
     * Records a call which was not permitted, because the CircuitBreaker state is OPEN.
     */
    void onCallNotPermitted() {
        numberOfNotPermittedCalls.increment();
    }

    @Override
    public float getFailureRate() {
        return 0;
    }

    @Override
    public int getNumberOfBufferedCalls() {
        return 0;
    }

    @Override
    public int getNumberOfFailedCalls() {
        return 0;
    }

    @Override
    public long getNumberOfNotPermittedCalls() {
        return 0;
    }

    @Override
    public int getMaxNumberOfBufferedCalls() {
        return 0;
    }

    @Override
    public int getNumberOfSuccessfulCalls() {
        return 0;
    }
}
