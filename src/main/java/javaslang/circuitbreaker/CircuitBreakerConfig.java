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

import java.time.Duration;
import java.util.function.Predicate;


public class CircuitBreakerConfig {

    public static final int DEFAULT_MAX_FAILURE_THRESHOLD = 50; // Percentage
    public static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60; // Seconds
    public static final int DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE = 10;
    public static final int DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE = 100;

    private final float failureRateThreshold;
    private final int ringBufferSizeInHalfOpenState;
    private final int ringBufferSizeInClosedState;
    private final Duration waitDurationInOpenState;
    private final Predicate<Throwable> recordFailurePredicate;

    private CircuitBreakerConfig(Context context){
        this.failureRateThreshold = context.failureRateThreshold;
        this.waitDurationInOpenState = context.waitDurationInOpenState;
        this.ringBufferSizeInHalfOpenState = context.ringBufferSizeInHalfOpenState;
        this.ringBufferSizeInClosedState = context.ringBufferSizeInClosedState;
        this.recordFailurePredicate = context.recordFailurePredicate;

    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public int getRingBufferSizeInHalfOpenState() {
        return ringBufferSizeInHalfOpenState;
    }

    public int getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    public Predicate<Throwable> getRecordFailurePredicate() {
        return recordFailurePredicate;
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

        private Context context = new Context();

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
            context.failureRateThreshold = failureRateThreshold;
            return this;
        }

        /**
         * Configures the wait duration which specifies how long the CircuitBreaker should stay open, before it switches to half open.
         * Default value is 60 seconds.
         *
         * @param waitDurationInOpenState the wait duration which specifies how long the CircuitBreaker should stay open
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder waitDurationInOpenState(Duration waitDurationInOpenState) {
            if (waitDurationInOpenState.getSeconds() < 1) {
                throw new IllegalArgumentException("waitDurationInOpenState must be at least 1000[ms]");
            }
            context.waitDurationInOpenState = waitDurationInOpenState;
            return this;
        }

        /**
         * Configures the size of the ring buffer when the CircuitBreaker is half open. The CircuitBreaker stores the success/failure success / failure status of the latest calls in a ring buffer.
         * For example, if {@code ringBufferSizeInClosedState} is 10, then at least 10 calls must be evaluated, before the failure rate can be calculated.
         * If only 9 calls have been evaluated the CircuitBreaker will not trip back to closed or open even if all 9 calls have failed.
         *
         * The size must be greater than 0. Default size is 10.
         *
         * @param ringBufferSizeInHalfOpenState the size of the ring buffer when the CircuitBreaker is is half open
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder ringBufferSizeInHalfOpenState(int ringBufferSizeInHalfOpenState) {
            if (ringBufferSizeInHalfOpenState < 1 ) {
                throw new IllegalArgumentException("ringBufferSizeInHalfOpenState must be greater than 0");
            }
            context.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
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
            context.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
            return this;
        }

        /**
         * Configures a Predicate which evaluates if an exception should be recorded as a failure and thus increase the failure rate.
         * The Predicate must return true if the exception should count as a failure, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be recorded as a failure and thus trigger the CircuitBreaker
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder recordFailure(Predicate<Throwable> predicate) {
            context.recordFailurePredicate = predicate;
            return this;
        }

        /**
         * Builds a CircuitBreakerConfig
         *
         * @return the CircuitBreakerConfig
         */
        public CircuitBreakerConfig build() {
            return new CircuitBreakerConfig(context);
        }
    }

    private static class Context {
        float failureRateThreshold = DEFAULT_MAX_FAILURE_THRESHOLD;
        int ringBufferSizeInHalfOpenState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;
        int ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
        Duration waitDurationInOpenState = Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
        // The default exception predicate counts all exceptions as failures.
        Predicate<Throwable> recordFailurePredicate = (exception) -> true;
    }
}
