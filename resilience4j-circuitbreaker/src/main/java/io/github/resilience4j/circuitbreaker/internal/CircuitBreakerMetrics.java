/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.circuitbreaker.internal;


import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.metrics.SlidingWindowMetrics;
import io.github.resilience4j.core.metrics.Snapshot;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static io.github.resilience4j.core.metrics.Metrics.Outcome;

class CircuitBreakerMetrics implements CircuitBreaker.Metrics {

    private final int ringBufferSize;
    private final SlidingWindowMetrics metrics;
    private final LongAdder numberOfNotPermittedCalls;

    CircuitBreakerMetrics(int ringBufferSize) {
        this.ringBufferSize = ringBufferSize;
        this.metrics = new SlidingWindowMetrics(this.ringBufferSize);
        this.numberOfNotPermittedCalls = new LongAdder();
    }

    /**
     * Records a failed call and returns the current failure rate in percentage.
     *
     * @return the current failure rate  in percentage.
     */
    float onError(long duration, TimeUnit durationUnit) {
        Snapshot snapshot = metrics.record(duration, durationUnit, Outcome.ERROR);
        return getFailureRate(snapshot);
    }

    /**
     * Records a successful call and returns the current failure rate in percentage.
     *
     * @return the current failure rate in percentage.
     */
    float onSuccess(long duration, TimeUnit durationUnit) {
        Snapshot snapshot = metrics.record(duration, durationUnit, Outcome.SUCCESS);
        return getFailureRate(snapshot);
    }

    /**
     * Records a call which was not permitted, because the CircuitBreaker state is OPEN.
     */
    void onCallNotPermitted() {
        numberOfNotPermittedCalls.increment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFailureRate() {
        return getFailureRate(metrics.getSnapshot());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxNumberOfBufferedCalls() {
        return ringBufferSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfSuccessfulCalls() {
        return this.metrics.getSnapshot().getNumberOfSuccessfulCalls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfBufferedCalls() {
        return this.metrics.getSnapshot().getTotalNumberOfCalls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNumberOfNotPermittedCalls() {
        return this.numberOfNotPermittedCalls.sum();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfFailedCalls() {
        return this.metrics.getSnapshot().getNumberOfFailedCalls();
    }

    private float getFailureRate(Snapshot snapshot) {
        int bufferedCalls = snapshot.getTotalNumberOfCalls();
        if (bufferedCalls < ringBufferSize) {
            return -1.0f;
        }
        return snapshot.getNumberOfFailedCalls() * 100.0f / bufferedCalls;
    }
}
