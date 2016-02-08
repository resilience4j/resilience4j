/*
 *
 *  Copyright 2015 Robert Winkler
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
package javaslang.circuitbreaker;

import javaslang.circuitbreaker.internal.DefaultCircuitBreakerEventListener;

import java.time.Duration;
import java.util.function.Predicate;


public class CircuitBreakerConfig {

    private static final int DEFAULT_MAX_FAILURE_THRESHOLD = 50; // Percentage
    private static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60; // Seconds
    private static final int DEFAULT_RING_BUFFER_SIZE_IN_HALF_CLOSED_STATE = 10;
    private static final int DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE = 100;

    private final float failureRateThreshold;
    private int ringBufferSizeInHalfClosedState;
    private int ringBufferSizeInClosedState;
    private final Duration waitDurationInOpenState;
    private final CircuitBreakerEventListener circuitBreakerEventListener;
    private final Predicate<Throwable> exceptionPredicate;

    private CircuitBreakerConfig(float failureRateThreshold,
                                 Duration waitDurationInOpenState,
                                 int ringBufferSizeInHalfClosedState,
                                 int ringBufferSizeInClosedState,
                                 Predicate<Throwable> exceptionPredicate,
                                 CircuitBreakerEventListener circuitBreakerEventListener){
        this.failureRateThreshold = failureRateThreshold;
        this.waitDurationInOpenState = waitDurationInOpenState;
        this.ringBufferSizeInHalfClosedState = ringBufferSizeInHalfClosedState;
        this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
        this.exceptionPredicate = exceptionPredicate;
        this.circuitBreakerEventListener = circuitBreakerEventListener;

    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public int getRingBufferSizeInHalfClosedState() {
        return ringBufferSizeInHalfClosedState;
    }

    public int getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    public CircuitBreakerEventListener getCircuitBreakerEventListener() {
        return circuitBreakerEventListener;
    }

    public Predicate<Throwable> getExceptionPredicate() {
        return exceptionPredicate;
    }

    /**
     * Returns a builder to create a custom CircuitBreakerConfig.
     *
     * @return a {@link CircuitBreakerConfig.Builder}
     */
    public static CircuitBreakerConfig.Builder custom(){
        return new Builder();
    }

    /**
     * Creates a default CircuitBreaker configuration.
     *
     * @return a default CircuitBreaker configuration.
     */
    public static CircuitBreakerConfig ofDefaults(){
        return new Builder().build();
    }


    public static class Builder {
        private int failureRateThreshold = DEFAULT_MAX_FAILURE_THRESHOLD;
        private int ringBufferSizeInHalfClosedState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_CLOSED_STATE;
        private int ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
        private Duration waitDurationInOpenState = Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
        private CircuitBreakerEventListener circuitBreakerEventListener = new DefaultCircuitBreakerEventListener();
        // The default exception predicate counts all exceptions as failures.
        private Predicate<Throwable> exceptionPredicate = (exception) -> true;

        /**
         * Configures the failure rate threshold in percentage above which the CircuitBreaker should trip open and start short-circuiting calls.
         *
         * The threshold must be between 1 and 100. Default value is 50 percentage.
         *
         * @param failureRateThreshold the failure rate threshold in percentage
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder failureRateThreshold(int failureRateThreshold) {
            if (failureRateThreshold < 1 || failureRateThreshold > 100) {
                throw new IllegalArgumentException("failureRateThreshold must be between 1 and 100");
            }
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        /**
         * Configures the wait duration which specifies how long the CircuitBreaker should stay open, before it switches to half closed.
         * Default value is 60 seconds.
         *
         * @param waitDurationInOpenState the wait duration which specifies how long the CircuitBreaker should stay open
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
            if (waitDurationInOpenState.getSeconds() < 1) {
                throw new IllegalArgumentException("waitDurationInOpenState must be at least 1000[ms]");
            }
            this.waitDurationInOpenState = waitDurationInOpenState;
            return this;
        }

        /**
         * Configures the size of the ring buffer when the CircuitBreaker is half closed. The CircuitBreaker stores the success/failure success / failure status of the latest calls in a ring buffer.
         * For example, if {@code ringBufferSizeInClosedState} is 10, then at least 10 calls must be evaluated, before the failure rate can be calculated.
         * If only 9 calls have been evaluated the CircuitBreaker will not trip back to closed or open even if all 9 calls have failed.
         *
         * The size must be greater than 0. Default size is 10.
         *
         * @param ringBufferSizeInHalfClosedState the size of the ring buffer when the CircuitBreaker is is half closed
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder ringBufferSizeInHalfClosedState(int ringBufferSizeInHalfClosedState) {
            if (ringBufferSizeInHalfClosedState < 1) {
                throw new IllegalArgumentException("ringBufferSizeInHalfClosedState must be greater than 0");
            }
            this.ringBufferSizeInHalfClosedState = ringBufferSizeInHalfClosedState;
            return this;
        }

        /**
         * Configures the size of the ring buffer when the CircuitBreaker is closed. The CircuitBreaker stores the success/failure success / failure status of the latest calls in a ring buffer.
         * For example, if {@code ringBufferSizeInClosedState} is 100, then at least 100 calls must be evaluated, before the failure rate can be calculated.
         * If only 99 calls have been evaluated the CircuitBreaker will not trip open even if all 99 calls have failed.
         *
         * The size must be greater than 0. Default size is 100.
         *
         * @param ringBufferSizeInClosedState the size of the ring buffer when the CircuitBreaker is closed.
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder ringBufferSizeInClosedState(int ringBufferSizeInClosedState) {
            if (ringBufferSizeInClosedState < 1) {
                throw new IllegalArgumentException("ringBufferSizeInClosedState must be greater than 0");
            }
            this.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
            return this;
        }

        /**
         *  Configures the CircuitBreakerEventListener which should handle CircuitBreaker events.
         *
         * @param circuitBreakerEventListener the CircuitBreakerEventListener which should handle CircuitBreaker events
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder onCircuitBreakerEvent(CircuitBreakerEventListener circuitBreakerEventListener) {
            if (circuitBreakerEventListener == null) {
                throw new IllegalArgumentException("circuitBreakerEventListener must not be null");
            }
            this.circuitBreakerEventListener = circuitBreakerEventListener;
            return this;
        }

        /**
         *  Configures a Predicate which evaluates if an exception should be recorded as a failure and thus increase the failure rate.
         *  The Predicate must return true if the exception should count as a failure, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be recorded as a failure and thus trigger the CircuitBreaker
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder recordFailure(Predicate<Throwable> predicate) {
            this.exceptionPredicate = predicate;
            return this;
        }

        /**
         * Builds a CircuitBreakerConfig
         *
         * @return the CircuitBreakerConfig
         */
        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(
                    failureRateThreshold,
                    waitDurationInOpenState,
                    ringBufferSizeInHalfClosedState,
                    ringBufferSizeInClosedState,
                    exceptionPredicate,
                    circuitBreakerEventListener);
        }
    }
}
