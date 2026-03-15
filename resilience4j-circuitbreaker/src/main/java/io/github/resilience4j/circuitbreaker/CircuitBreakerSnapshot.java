/*
 *
 *  Copyright 2025
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

import io.github.resilience4j.core.lang.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Immutable snapshot of a CircuitBreaker's state and accumulated metrics.
 * <p>
 * This snapshot can be used to preserve state when reconfiguring a CircuitBreaker
 * at runtime without losing accumulated statistics.
 * <p>
 * The snapshot captures:
 * <ul>
 *   <li>Current state (CLOSED, OPEN, HALF_OPEN, DISABLED, FORCED_OPEN, METRICS_ONLY)</li>
 *   <li>Accumulated metrics (success/failure counts, slow call statistics)</li>
 *   <li>State-specific data (retry-after time for OPEN state, attempts count)</li>
 * </ul>
 *
 * <p><strong>Thread Safety:</strong> This class is immutable and thread-safe.
 *
 * <p><strong>Example:</strong>
 * <pre>{@code
 * CircuitBreaker cb1 = CircuitBreaker.of("service", config1);
 * // ... accumulate metrics ...
 *
 * // Capture current state
 * CircuitBreakerSnapshot snapshot = cb1.createSnapshot();
 *
 * // Recreate with new configuration, preserving state
 * CircuitBreaker cb2 = CircuitBreaker.of("service", config2, snapshot);
 * }</pre>
 *
 * @since 2.4.0
 */
public final class CircuitBreakerSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private final CircuitBreaker.State state;
    private final MetricsSnapshot metricsSnapshot;
    private final int attempts;
    @Nullable
    private final Instant retryAfterWaitUntil;

    private CircuitBreakerSnapshot(Builder builder) {
        this.state = Objects.requireNonNull(builder.state, "State must not be null");
        this.metricsSnapshot = Objects.requireNonNull(builder.metricsSnapshot, "MetricsSnapshot must not be null");
        this.attempts = builder.attempts;
        this.retryAfterWaitUntil = builder.retryAfterWaitUntil;
    }

    /**
     * Creates a new Builder for constructing a CircuitBreakerSnapshot.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the state at the time of snapshot creation.
     *
     * @return the circuit breaker state
     */
    public CircuitBreaker.State getState() {
        return state;
    }

    /**
     * Returns the metrics snapshot.
     *
     * @return the metrics snapshot
     */
    public MetricsSnapshot getMetricsSnapshot() {
        return metricsSnapshot;
    }

    /**
     * Returns the number of attempts (relevant for OPEN and HALF_OPEN states).
     *
     * @return the attempts count
     */
    public int getAttempts() {
        return attempts;
    }

    /**
     * Returns the time until which the circuit breaker should wait in OPEN state.
     * This is only relevant when the state is OPEN.
     *
     * @return the retry-after instant, or null if not applicable
     */
    @Nullable
    public Instant getRetryAfterWaitUntil() {
        return retryAfterWaitUntil;
    }

    @Override
    public String toString() {
        return "CircuitBreakerSnapshot{" +
            "state=" + state +
            ", metricsSnapshot=" + metricsSnapshot +
            ", attempts=" + attempts +
            ", retryAfterWaitUntil=" + retryAfterWaitUntil +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CircuitBreakerSnapshot that = (CircuitBreakerSnapshot) o;
        return attempts == that.attempts
            && state == that.state
            && Objects.equals(metricsSnapshot, that.metricsSnapshot)
            && Objects.equals(retryAfterWaitUntil, that.retryAfterWaitUntil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(state, metricsSnapshot, attempts, retryAfterWaitUntil);
    }
    /**
     * Builder for creating CircuitBreakerSnapshot instances.
     */
    public static class Builder {
        private CircuitBreaker.State state;
        private MetricsSnapshot metricsSnapshot;
        private int attempts = 0;
        @Nullable
        private Instant retryAfterWaitUntil;

        private Builder() {
        }

        /**
         * Sets the state.
         *
         * @param state the circuit breaker state
         * @return this builder
         */
        public Builder state(CircuitBreaker.State state) {
            this.state = state;
            return this;
        }

        /**
         * Sets the metrics snapshot.
         *
         * @param metricsSnapshot the metrics snapshot
         * @return this builder
         */
        public Builder metricsSnapshot(MetricsSnapshot metricsSnapshot) {
            this.metricsSnapshot = metricsSnapshot;
            return this;
        }

        /**
         * Sets the attempts count.
         *
         * @param attempts the number of attempts
         * @return this builder
         */
        public Builder attempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        /**
         * Sets the retry-after instant (for OPEN state).
         *
         * @param retryAfterWaitUntil the instant until which to wait
         * @return this builder
         */
        public Builder retryAfterWaitUntil(@Nullable Instant retryAfterWaitUntil) {
            this.retryAfterWaitUntil = retryAfterWaitUntil;
            return this;
        }

        /**
         * Builds the CircuitBreakerSnapshot.
         *
         * @return a new CircuitBreakerSnapshot instance
         * @throws NullPointerException if state or metricsSnapshot is null
         */
        public CircuitBreakerSnapshot build() {
            return new CircuitBreakerSnapshot(this);
        }
    }

    /**
     * Immutable snapshot of CircuitBreaker metrics at a point in time.
     */
    public static final class MetricsSnapshot implements Serializable {

        private static final long serialVersionUID = 1L;

        private final int numberOfSuccessfulCalls;
        private final int numberOfFailedCalls;
        private final int numberOfSlowCalls;
        private final int numberOfSlowSuccessfulCalls;
        private final int numberOfSlowFailedCalls;
        private final long numberOfNotPermittedCalls;

        private MetricsSnapshot(MetricsBuilder builder) {
            this.numberOfSuccessfulCalls = builder.numberOfSuccessfulCalls;
            this.numberOfFailedCalls = builder.numberOfFailedCalls;
            this.numberOfSlowCalls = builder.numberOfSlowCalls;
            this.numberOfSlowSuccessfulCalls = builder.numberOfSlowSuccessfulCalls;
            this.numberOfSlowFailedCalls = builder.numberOfSlowFailedCalls;
            this.numberOfNotPermittedCalls = builder.numberOfNotPermittedCalls;
        }

        /**
         * Creates a new MetricsBuilder for constructing a MetricsSnapshot.
         *
         * @return a new MetricsBuilder instance
         */
        public static MetricsBuilder builder() {
            return new MetricsBuilder();
        }

        /**
         * Returns the number of successful calls.
         *
         * @return the number of successful calls
         */
        public int getNumberOfSuccessfulCalls() {
            return numberOfSuccessfulCalls;
        }

        /**
         * Returns the number of failed calls.
         *
         * @return the number of failed calls
         */
        public int getNumberOfFailedCalls() {
            return numberOfFailedCalls;
        }

        /**
         * Returns the total number of slow calls.
         *
         * @return the number of slow calls
         */
        public int getNumberOfSlowCalls() {
            return numberOfSlowCalls;
        }

        /**
         * Returns the number of slow successful calls.
         *
         * @return the number of slow successful calls
         */
        public int getNumberOfSlowSuccessfulCalls() {
            return numberOfSlowSuccessfulCalls;
        }

        /**
         * Returns the number of slow failed calls.
         *
         * @return the number of slow failed calls
         */
        public int getNumberOfSlowFailedCalls() {
            return numberOfSlowFailedCalls;
        }

        /**
         * Returns the number of not permitted calls.
         *
         * @return the number of not permitted calls
         */
        public long getNumberOfNotPermittedCalls() {
            return numberOfNotPermittedCalls;
        }

        @Override
        public String toString() {
            return "MetricsSnapshot{" +
                "numberOfSuccessfulCalls=" + numberOfSuccessfulCalls +
                ", numberOfFailedCalls=" + numberOfFailedCalls +
                ", numberOfSlowCalls=" + numberOfSlowCalls +
                ", numberOfSlowSuccessfulCalls=" + numberOfSlowSuccessfulCalls +
                ", numberOfSlowFailedCalls=" + numberOfSlowFailedCalls +
                ", numberOfNotPermittedCalls=" + numberOfNotPermittedCalls +
                '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MetricsSnapshot)) {
                return false;
            }
            MetricsSnapshot that = (MetricsSnapshot) o;
            return numberOfSuccessfulCalls == that.numberOfSuccessfulCalls
                && numberOfFailedCalls == that.numberOfFailedCalls
                && numberOfSlowCalls == that.numberOfSlowCalls
                && numberOfSlowSuccessfulCalls == that.numberOfSlowSuccessfulCalls
                && numberOfSlowFailedCalls == that.numberOfSlowFailedCalls
                && numberOfNotPermittedCalls == that.numberOfNotPermittedCalls;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                numberOfSuccessfulCalls,
                numberOfFailedCalls,
                numberOfSlowCalls,
                numberOfSlowSuccessfulCalls,
                numberOfSlowFailedCalls,
                numberOfNotPermittedCalls
            );
        }
        /**
         * Builder for creating MetricsSnapshot instances.
         */
        public static class MetricsBuilder {
            private int numberOfSuccessfulCalls = 0;
            private int numberOfFailedCalls = 0;
            private int numberOfSlowCalls = 0;
            private int numberOfSlowSuccessfulCalls = 0;
            private int numberOfSlowFailedCalls = 0;
            private long numberOfNotPermittedCalls = 0;

            private MetricsBuilder() {
            }

            /**
             * Sets the number of successful calls.
             *
             * @param numberOfSuccessfulCalls the number of successful calls
             * @return this builder
             */
            public MetricsBuilder numberOfSuccessfulCalls(int numberOfSuccessfulCalls) {
                if (numberOfSuccessfulCalls < 0) {
                    throw new IllegalArgumentException("numberOfSuccessfulCalls must be non-negative");
                }
                this.numberOfSuccessfulCalls = numberOfSuccessfulCalls;
                return this;
            }

            /**
             * Sets the number of failed calls.
             *
             * @param numberOfFailedCalls the number of failed calls
             * @return this builder
             */
            public MetricsBuilder numberOfFailedCalls(int numberOfFailedCalls) {
                if (numberOfFailedCalls < 0) {
                    throw new IllegalArgumentException("numberOfFailedCalls must be non-negative");
                }
                this.numberOfFailedCalls = numberOfFailedCalls;
                return this;
            }

            /**
             * Sets the total number of slow calls.
             *
             * @param numberOfSlowCalls the number of slow calls
             * @return this builder
             */
            public MetricsBuilder numberOfSlowCalls(int numberOfSlowCalls) {
                if (numberOfSlowCalls < 0) {
                    throw new IllegalArgumentException("numberOfSlowCalls must be non-negative");
                }
                this.numberOfSlowCalls = numberOfSlowCalls;
                return this;
            }

            /**
             * Sets the number of slow successful calls.
             *
             * @param numberOfSlowSuccessfulCalls the number of slow successful calls
             * @return this builder
             */
            public MetricsBuilder numberOfSlowSuccessfulCalls(int numberOfSlowSuccessfulCalls) {
                if (numberOfSlowSuccessfulCalls < 0) {
                    throw new IllegalArgumentException("numberOfSlowSuccessfulCalls must be non-negative");
                }
                this.numberOfSlowSuccessfulCalls = numberOfSlowSuccessfulCalls;
                return this;
            }

            /**
             * Sets the number of slow failed calls.
             *
             * @param numberOfSlowFailedCalls the number of slow failed calls
             * @return this builder
             */
            public MetricsBuilder numberOfSlowFailedCalls(int numberOfSlowFailedCalls) {
                if (numberOfSlowFailedCalls < 0) {
                    throw new IllegalArgumentException("numberOfSlowFailedCalls must be non-negative");
                }
                this.numberOfSlowFailedCalls = numberOfSlowFailedCalls;
                return this;
            }

            /**
             * Sets the number of not permitted calls.
             *
             * @param numberOfNotPermittedCalls the number of not permitted calls
             * @return this builder
             */
            public MetricsBuilder numberOfNotPermittedCalls(long numberOfNotPermittedCalls) {
                if (numberOfNotPermittedCalls < 0) {
                    throw new IllegalArgumentException("numberOfNotPermittedCalls must be non-negative");
                }
                this.numberOfNotPermittedCalls = numberOfNotPermittedCalls;
                return this;
            }

            /**
             * Builds the MetricsSnapshot.
             *
             * @return a new MetricsSnapshot instance
             */
            public MetricsSnapshot build() {
                return new MetricsSnapshot(this);
            }
        }
    }
}
