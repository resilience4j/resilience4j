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

    private static final int DEFAULT_MAX_FAILURES = 3;
    private static final long DEFAULT_WAIT_DURATION = 60000;

    private final int maxFailures;
    private final Duration waitDuration;
    private CircuitBreakerEventListener circuitBreakerEventListener;
    private Predicate<Throwable> exceptionPredicate;

    private CircuitBreakerConfig(int maxFailures,
                                 Duration waitDuration,
                                 Predicate<Throwable> exceptionPredicate,
                                 CircuitBreakerEventListener circuitBreakerEventListener){
        this.maxFailures = maxFailures;
        this.waitDuration = waitDuration;
        this.exceptionPredicate = exceptionPredicate;
        this.circuitBreakerEventListener = circuitBreakerEventListener;
    }

    public int getMaxFailures() {
        return maxFailures;
    }

    public Duration getWaitDuration() {
        return waitDuration;
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
     * @return A {@link CircuitBreakerConfig.Builder}
     */
    public static CircuitBreakerConfig.Builder custom(){
        return new Builder();
    }

    public static class Builder {
        private int maxFailures = DEFAULT_MAX_FAILURES;
        private Duration waitDuration = Duration.ofMillis(DEFAULT_WAIT_DURATION);
        private CircuitBreakerEventListener circuitBreakerEventListener = new DefaultCircuitBreakerEventListener();
        // The default exception predicate counts all exceptions as failures.
        private Predicate<Throwable> exceptionPredicate = (exception) -> true;

        /**
         * Configures the maximum number of allowed failures.
         *
         * @param maxFailures the maximum number of allowed failures
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder maxFailures(int maxFailures) {
            if (maxFailures < 1) {
                throw new IllegalArgumentException("maxFailures must be greater than or equal to 1");
            }
            this.maxFailures = maxFailures;
            return this;
        }

        /**
         * Configures the wait duration which specifies how long the CircuitBreaker should stay OPEN
         *
         * @param waitDuration the wait duration which specifies how long the CircuitBreaker should stay OPEN
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder waitDuration(Duration waitDuration) {
            if (waitDuration.getSeconds() < 0.1) {
                throw new IllegalArgumentException("waitDuration must be at least 100[ms]");
            }
            this.waitDuration = waitDuration;
            return this;
        }

        /**
         *  Configures the CircuitBreakerEventListener which should handle CircuitBreaker events.
         *
         * @param circuitBreakerEventListener the CircuitBreakerEventListener which should handle CircuitBreaker events.
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
         *  Configures a Predicate which evaluates if an exception should be recorded as a failure and thus trigger the circuit breaker.
         *  The Predicate must return true if the exception should count as a failure, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be recorded as a failure and thus trigger the circuit breaker.
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
            return new CircuitBreakerConfig(maxFailures, waitDuration, exceptionPredicate, circuitBreakerEventListener);
        }
    }
}
