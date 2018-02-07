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


import io.github.resilience4j.bitset.RingBitSet;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;

import java.util.concurrent.atomic.LongAdder;

class CircuitBreakerMetrics implements CircuitBreaker.Metrics {

    private final int ringBufferSize;
    private final RingBitSet ringBitSet;
    private final LongAdder numberOfNotPermittedCalls;

    CircuitBreakerMetrics(int ringBufferSize) {
        this(ringBufferSize, null);
    }

    CircuitBreakerMetrics(int ringBufferSize, RingBitSet sourceSet) {
        this.ringBufferSize = ringBufferSize;
        if(sourceSet != null) {
            this.ringBitSet = new RingBitSet(this.ringBufferSize, sourceSet);
        }else{
            this.ringBitSet = new RingBitSet(this.ringBufferSize);
        }
        this.numberOfNotPermittedCalls = new LongAdder();
    }

    /**
     * Creates a new CircuitBreakerMetrics instance and copies the content of the current RingBitSet
     * into the new RingBitSet.
     *
     * @param targetRingBufferSize the ringBufferSize of the new CircuitBreakerMetrics instances
     * @return a CircuitBreakerMetrics
     */
    public CircuitBreakerMetrics copy(int targetRingBufferSize) {
        return new CircuitBreakerMetrics(targetRingBufferSize, this.ringBitSet);
    }

    /**
     * Records a failed call and returns the current failure rate in percentage.
     *
     * @return the current failure rate  in percentage.
     */
    float onError() {
        int currentNumberOfFailedCalls = ringBitSet.setNextBit(true);
        return getFailureRate(currentNumberOfFailedCalls);
    }

    /**
     * Records a successful call and returns the current failure rate in percentage.
     *
     * @return the current failure rate in percentage.
     */
    float onSuccess() {
        int currentNumberOfFailedCalls = ringBitSet.setNextBit(false);
        return getFailureRate(currentNumberOfFailedCalls);
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
        return getFailureRate(getNumberOfFailedCalls());
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
        return getNumberOfBufferedCalls() - getNumberOfFailedCalls();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfBufferedCalls() {
        return this.ringBitSet.length();
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
        return this.ringBitSet.cardinality();
    }

    private float getFailureRate(int numberOfFailedCalls) {
        if (getNumberOfBufferedCalls() < ringBufferSize) {
            return -1.0f;
        }
        return numberOfFailedCalls * 100.0f / ringBufferSize;
    }
}
