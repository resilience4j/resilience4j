/*
 *
 *  Copyright 2016 Robert Winkler
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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.predicate.PredicateCreator;

import java.time.Duration;
import java.util.function.Predicate;


/**
 * A {@link CircuitBreakerConfig} configures a {@link CircuitBreaker}
 */
public class CircuitBreakerConfig {

    public static final int DEFAULT_MAX_FAILURE_THRESHOLD = 50; // Percentage
    public static final int DEFAULT_WAIT_DURATION_IN_OPEN_STATE = 60; // Seconds
    public static final int DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE = 10;
    public static final int DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE = 100;
    private static final Predicate<Throwable> DEFAULT_RECORD_FAILURE_PREDICATE = throwable -> true;

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] recordExceptions = new Class[0];
    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] ignoreExceptions = new Class[0];

    private float failureRateThreshold = DEFAULT_MAX_FAILURE_THRESHOLD;
    private int ringBufferSizeInHalfOpenState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;
    private int ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
    private Duration waitDurationInOpenState = Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
    private IntervalFunction waitIntervalFunctionInOpenState = null;
    // The default exception predicate counts all exceptions as failures.
    private Predicate<Throwable> recordFailurePredicate = DEFAULT_RECORD_FAILURE_PREDICATE;
    private boolean automaticTransitionFromOpenToHalfOpenEnabled = false;

    private CircuitBreakerConfig() {
    }

    /**
     * Returns a builder to create a custom CircuitBreakerConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Returns a builder to create a custom CircuitBreakerConfig based on another CircuitBreakerConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder from(CircuitBreakerConfig baseConfig) {
        return new Builder(baseConfig);
    }

    /**
     * Creates a default CircuitBreaker configuration.
     *
     * @return a default CircuitBreaker configuration.
     */
    public static CircuitBreakerConfig ofDefaults() {
        return new Builder().build();
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public Duration getWaitDurationInOpenState() {
        return waitDurationInOpenState;
    }

    public IntervalFunction getWaitIntervalFunctionInOpenState() { return waitIntervalFunctionInOpenState; }

    public int getRingBufferSizeInHalfOpenState() {
        return ringBufferSizeInHalfOpenState;
    }

    public int getRingBufferSizeInClosedState() {
        return ringBufferSizeInClosedState;
    }

    public Predicate<Throwable> getRecordFailurePredicate() {
        return recordFailurePredicate;
    }

    public boolean isAutomaticTransitionFromOpenToHalfOpenEnabled() {
        return automaticTransitionFromOpenToHalfOpenEnabled;
    }

    public static class Builder {
        @Nullable
        private Predicate<Throwable> recordFailurePredicate;
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] recordExceptions = new Class[0];
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] ignoreExceptions = new Class[0];
        private float failureRateThreshold = DEFAULT_MAX_FAILURE_THRESHOLD;
        private int ringBufferSizeInHalfOpenState = DEFAULT_RING_BUFFER_SIZE_IN_HALF_OPEN_STATE;
        private int ringBufferSizeInClosedState = DEFAULT_RING_BUFFER_SIZE_IN_CLOSED_STATE;
        private Duration waitDurationInOpenState = Duration.ofSeconds(DEFAULT_WAIT_DURATION_IN_OPEN_STATE);
        private IntervalFunction waitIntervalFunctionInOpenState = null;
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = false;

        public Builder(CircuitBreakerConfig baseConfig) {
            this.waitDurationInOpenState = baseConfig.waitDurationInOpenState;
            this.waitIntervalFunctionInOpenState = baseConfig.waitIntervalFunctionInOpenState;
            this.ringBufferSizeInHalfOpenState = baseConfig.ringBufferSizeInHalfOpenState;
            this.ringBufferSizeInClosedState = baseConfig.ringBufferSizeInClosedState;
            this.failureRateThreshold = baseConfig.failureRateThreshold;
            this.ignoreExceptions = baseConfig.ignoreExceptions;
            this.recordExceptions = baseConfig.recordExceptions;
            this.recordFailurePredicate = baseConfig.recordFailurePredicate;
            this.automaticTransitionFromOpenToHalfOpenEnabled = baseConfig.automaticTransitionFromOpenToHalfOpenEnabled;
        }

        public Builder() {

        }

        /**
         * Configures the failure rate threshold in percentage above which the CircuitBreaker should trip open and start short-circuiting calls.
         * <p>
         * The threshold must be greater than 0 and not greater than 100. Default value is 50 percentage.
         *
         * @param failureRateThreshold the failure rate threshold in percentage
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder failureRateThreshold(float failureRateThreshold) {
            if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
                throw new IllegalArgumentException("failureRateThreshold must be between 1 and 100");
            }
            this.failureRateThreshold = failureRateThreshold;
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
            if (waitDurationInOpenState.toMillis() < 1) {
                throw new IllegalArgumentException("waitDurationInOpenState must be at least 1[ms]");
            }
            this.waitDurationInOpenState = waitDurationInOpenState;
            return this;
        }

        /**
         * configures the variable wait duration which specifies how long the CircuitBreaker should stay open, before it switches to half open. Useful for exponential back-off
         * @param waitIntervalFunctionInOpenState Interval function that returns wait time as a function of attempts
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder waitIntervalFunctionInOpenState(IntervalFunction waitIntervalFunctionInOpenState) {
            this.waitIntervalFunctionInOpenState = waitIntervalFunctionInOpenState;
            return this;
        }

        /**
         * Configures the size of the ring buffer when the CircuitBreaker is half open. The CircuitBreaker stores the success/failure success / failure status of the latest calls in a ring buffer.
         * For example, if {@code ringBufferSizeInClosedState} is 10, then at least 10 calls must be evaluated, before the failure rate can be calculated.
         * If only 9 calls have been evaluated the CircuitBreaker will not trip back to closed or open even if all 9 calls have failed.
         * <p>
         * The size must be greater than 0. Default size is 10.
         *
         * @param ringBufferSizeInHalfOpenState the size of the ring buffer when the CircuitBreaker is is half open
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder ringBufferSizeInHalfOpenState(int ringBufferSizeInHalfOpenState) {
            if (ringBufferSizeInHalfOpenState < 1) {
                throw new IllegalArgumentException("ringBufferSizeInHalfOpenState must be greater than 0");
            }
            this.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
            return this;
        }

        /**
         * Configures the size of the ring buffer when the CircuitBreaker is closed. The CircuitBreaker stores the success/failure success / failure status of the latest calls in a ring buffer.
         * For example, if {@code ringBufferSizeInClosedState} is 100, then at least 100 calls must be evaluated, before the failure rate can be calculated.
         * If only 99 calls have been evaluated the CircuitBreaker will not trip open even if all 99 calls have failed.
         * <p>
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
         * Configures a Predicate which evaluates if an exception should be recorded as a failure and thus increase the failure rate.
         * The Predicate must return true if the exception should count as a failure, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be recorded as a failure and thus trigger the CircuitBreaker
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder recordFailure(Predicate<Throwable> predicate) {
            this.recordFailurePredicate = predicate;
            return this;
        }

        /**
         * Configures a list of error classes that are recorded as a failure and thus increase the failure rate.
         * Any exception matching or inheriting from one of the list should count as a failure, unless ignored via
         *
         * @param errorClasses the error classes that are recorded
         * @return the CircuitBreakerConfig.Builder
         * @see #ignoreExceptions(Class[]) ). Ignoring an exception has priority over recording an exception.
         * <p>
         * Example:
         * recordExceptions(Throwable.class) and ignoreExceptions(RuntimeException.class)
         * would capture all Errors and checked Exceptions, and ignore unchecked
         * <p>
         * For a more sophisticated exception management use the
         * @see #recordFailure(Predicate) method
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder recordExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
            this.recordExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Configures a list of error classes that are ignored as a failure and thus do not increase the failure rate.
         * Any exception matching or inheriting from one of the list will not count as a failure, even if marked via
         *
         * @param errorClasses the error classes that are recorded
         * @return the CircuitBreakerConfig.Builder
         * @see #recordExceptions(Class[]) . Ignoring an exception has priority over recording an exception.
         * <p>
         * Example:
         * ignoreExceptions(Throwable.class) and recordExceptions(Exception.class)
         * would capture nothing
         * <p>
         * Example:
         * ignoreExceptions(Exception.class) and recordExceptions(Throwable.class)
         * would capture Errors
         * <p>
         * For a more sophisticated exception management use the
         * @see #recordFailure(Predicate) method
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder ignoreExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
            this.ignoreExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Enables automatic transition from OPEN to HALF_OPEN state once the waitDurationInOpenState has passed.
         *
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder enableAutomaticTransitionFromOpenToHalfOpen() {
            this.automaticTransitionFromOpenToHalfOpenEnabled = true;
            return this;
        }

        /**
         * Enables automatic transition from OPEN to HALF_OPEN state once the waitDurationInOpenState has passed.
         *
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder automaticTransitionFromOpenToHalfOpenEnabled(boolean enableAutomaticTransitionFromOpenToHalfOpen) {
            this.automaticTransitionFromOpenToHalfOpenEnabled = enableAutomaticTransitionFromOpenToHalfOpen;
            return this;
        }

        /**
         * Builds a CircuitBreakerConfig
         *
         * @return the CircuitBreakerConfig
         */
        public CircuitBreakerConfig build() {
            CircuitBreakerConfig config = new CircuitBreakerConfig();
            config.waitDurationInOpenState = waitDurationInOpenState;
            config.waitIntervalFunctionInOpenState = waitIntervalFunctionInOpenState;
            config.failureRateThreshold = failureRateThreshold;
            config.ringBufferSizeInClosedState = ringBufferSizeInClosedState;
            config.ringBufferSizeInHalfOpenState = ringBufferSizeInHalfOpenState;
            config.recordExceptions = recordExceptions;
            config.ignoreExceptions = ignoreExceptions;
            config.automaticTransitionFromOpenToHalfOpenEnabled = automaticTransitionFromOpenToHalfOpenEnabled;
            config.recordFailurePredicate = createRecordFailurePredicate();
            return config;
        }

        private Predicate<Throwable> createRecordFailurePredicate() {
            return createRecordExceptionPredicate()
                    .and(PredicateCreator.createIgnoreExceptionsPredicate(ignoreExceptions)
                            .orElse(DEFAULT_RECORD_FAILURE_PREDICATE));
        }

        private Predicate<Throwable> createRecordExceptionPredicate() {
            return PredicateCreator.createRecordExceptionsPredicate(recordExceptions)
                    .map(predicate -> recordFailurePredicate != null ? predicate.or(recordFailurePredicate) : predicate)
                    .orElseGet(() -> recordFailurePredicate != null ? recordFailurePredicate : DEFAULT_RECORD_FAILURE_PREDICATE);
        }
    }
}
