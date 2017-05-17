/*
 * Copyright 2017 Dan Maas
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.resilience4j.ratpack.circuitbreaker;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_MAX_FAILURE_THRESHOLD;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;
import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.DEFAULT_WAIT_DURATION_IN_OPEN_STATE;

public class CircuitBreakerConfig {
    private boolean defaults = false;
    private Integer waitIntervalInMillis = DEFAULT_WAIT_DURATION_IN_OPEN_STATE;
    private Integer failureRateThreshold = DEFAULT_MAX_FAILURE_THRESHOLD;
    private Integer ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
    private Integer ringBufferSizeInHalfOpenState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;

    /**
     * Use config provided by circuitbreaker registry instead of these config values.
     *
     * @param defaults
     * @return
     */
    public CircuitBreakerConfig defaults(boolean defaults) {
        this.defaults = defaults;
        return this;
    }

    public CircuitBreakerConfig waitIntervalInMillis(Integer waitInterval) {
        this.waitIntervalInMillis = waitInterval;
        return this;
    }

    public CircuitBreakerConfig failureRateThreshold(Integer failureRateThreshold) {
        this.failureRateThreshold = failureRateThreshold;
        return this;
    }

    public CircuitBreakerConfig ringBufferSizeInClosedState(Integer ringBufferSizeInClosedState) {
        this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        return this;
    }

    public CircuitBreakerConfig ringBufferSizeInHalfOpenState(Integer ringBufferSizeInHalfOpenState) {
        this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
        return this;
    }

    public boolean getDefaults() {
        return defaults;
    }

    public Integer getWaitIntervalInMillis() {
        return waitIntervalInMillis;
    }

    public Integer getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public Integer getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    public Integer getRingBufferSizeInHalfOpenState() {
        return ringBufferSizeInHalfOpenState;
    }
}
