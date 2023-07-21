/*
 *
 *  Copyright 2019: Bohdan Storozhuk, Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive;

import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveBulkheadStateMachine;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.predicate.PredicateCreator;

import java.io.Serial;
import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A {@link AdaptiveBulkheadConfig} configures an adaptation capabilities of {@link AdaptiveBulkheadStateMachine}
 */
public class AdaptiveBulkheadConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = -1632235529951414090L;

    private static final int DEFAULT_MAX_CONCURRENT_CALLS = 25;
    private static final int DEFAULT_MIN_CONCURRENT_CALLS = 2;
    private static final int DEFAULT_INITIAL_CONCURRENT_CALLS = DEFAULT_MIN_CONCURRENT_CALLS;
    private static final Duration DEFAULT_MAX_WAIT_DURATION = Duration.ofSeconds(0);
    private static final float DEFAULT_FAILURE_RATE_THRESHOLD_PERCENTAGE = 50.0f;
    private static final float DEFAULT_SLOW_CALL_RATE_THRESHOLD_PERCENTAGE = 50.0f;
    private static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;
    private static final long DEFAULT_SLOW_CALL_DURATION_THRESHOLD_SECONDS = 5;
    private static final int DEFAULT_MINIMUM_NUMBER_OF_CALLS = 100;
    private static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;
    private static final SlidingWindowType DEFAULT_SLIDING_WINDOW_TYPE = SlidingWindowType.COUNT_BASED;
    // The default exception predicate counts all exceptions as failures.
    private static final Predicate<Throwable> DEFAULT_RECORD_EXCEPTION_PREDICATE = throwable -> true;
    // The default exception predicate ignores no exceptions.
    private static final Predicate<Throwable> DEFAULT_IGNORE_EXCEPTION_PREDICATE = throwable -> false;
    private static final int DEFAULT_INCREASE_AUGEND = 1;
    private static final float DEFAULT_INCREASE_MULTIPLIER = 2f;
    private static final float DEFAULT_DECREASE_MULTIPLIER = 0.5f;
    private static final int DEFAULT_INCREASE_INTERVAL = 1;
    private static final Function<Clock, Long> DEFAULT_TIMESTAMP_FUNCTION = clock -> System.nanoTime();
    private static final TimeUnit DEFAULT_TIMESTAMP_UNIT = TimeUnit.NANOSECONDS;
    private static final Predicate<Object> DEFAULT_RECORD_RESULT_PREDICATE = (Object object) -> false;
    private static final boolean DEFAULT_RESET_METRICS_ON_TRANSITION = false;

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] recordExceptions = new Class[0];
    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] ignoreExceptions = new Class[0];
    @NonNull
    private transient Predicate<Throwable> recordExceptionPredicate = DEFAULT_RECORD_EXCEPTION_PREDICATE;
    @NonNull
    private transient Predicate<Throwable> ignoreExceptionPredicate = DEFAULT_IGNORE_EXCEPTION_PREDICATE;
    private int minimumNumberOfCalls = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
    private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
    private float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD_PERCENTAGE;
    private int slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;
    private SlidingWindowType slidingWindowType = DEFAULT_SLIDING_WINDOW_TYPE;
    private float slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD_PERCENTAGE;
    private Duration slowCallDurationThreshold = Duration
        .ofSeconds(DEFAULT_SLOW_CALL_DURATION_THRESHOLD_SECONDS);
    private int minConcurrentCalls = DEFAULT_MIN_CONCURRENT_CALLS;
    private int initialConcurrentCalls = DEFAULT_INITIAL_CONCURRENT_CALLS;
    private int maxConcurrentCalls = DEFAULT_MAX_CONCURRENT_CALLS;
    private int increaseAugend = DEFAULT_INCREASE_AUGEND;
    private float decreaseMultiplier = DEFAULT_DECREASE_MULTIPLIER;
    private float increaseMultiplier = DEFAULT_INCREASE_MULTIPLIER;
    private int increaseInterval = DEFAULT_INCREASE_INTERVAL;
    private Duration maxWaitDuration = DEFAULT_MAX_WAIT_DURATION;
    private transient Function<Clock, Long> currentTimestampFunction = DEFAULT_TIMESTAMP_FUNCTION;
    private TimeUnit timestampUnit = DEFAULT_TIMESTAMP_UNIT;
    private transient Predicate<Object> recordResultPredicate = DEFAULT_RECORD_RESULT_PREDICATE;
    private boolean resetMetricsOnTransition = DEFAULT_RESET_METRICS_ON_TRANSITION;

    private AdaptiveBulkheadConfig() {
    }

    public Predicate<Throwable> getRecordExceptionPredicate() {
        return recordExceptionPredicate;
    }

    public Predicate<Throwable> getIgnoreExceptionPredicate() {
        return ignoreExceptionPredicate;
    }

    public int getMinimumNumberOfCalls() {
        return minimumNumberOfCalls;
    }

    public float getFailureRateThreshold() {
        return failureRateThreshold;
    }

    public int getSlidingWindowSize() {
        return slidingWindowSize;
    }

    @NonNull
    public SlidingWindowType getSlidingWindowType() {
        return slidingWindowType;
    }

    public float getSlowCallRateThreshold() {
        return slowCallRateThreshold;
    }

    public Duration getSlowCallDurationThreshold() {
        return slowCallDurationThreshold;
    }

    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }

    public int getMinConcurrentCalls() {
        return minConcurrentCalls;
    }

    public int getInitialConcurrentCalls() {
        return initialConcurrentCalls;
    }

    public int getMaxConcurrentCalls() {
        return maxConcurrentCalls;
    }

    public int getIncreaseAugend() {
        return increaseAugend;
    }

    public float getDecreaseMultiplier() {
        return decreaseMultiplier;
    }

    public float getIncreaseMultiplier() {
        return increaseMultiplier;
    }

    public int getIncreaseInterval() {
        return increaseInterval;
    }

    public Duration getMaxWaitDuration() {
        return maxWaitDuration;
    }

    public Function<Clock, Long> getCurrentTimestampFunction() {
        return currentTimestampFunction;
    }

    public TimeUnit getTimestampUnit() {
        return timestampUnit;
    }

    public Predicate<Object> getRecordResultPredicate() {
        return recordResultPredicate;
    }

    public boolean isResetMetricsOnTransition() {
        return resetMetricsOnTransition;
    }

    @Override
    public String toString() {
        return "AdaptiveBulkheadConfig{" +
            "recordExceptions=" + Arrays.toString(recordExceptions) +
            ", ignoreExceptions=" + Arrays.toString(ignoreExceptions) +
            ", minimumNumberOfCalls=" + minimumNumberOfCalls +
            ", writableStackTraceEnabled=" + writableStackTraceEnabled +
            ", failureRateThreshold=" + failureRateThreshold +
            ", slidingWindowSize=" + slidingWindowSize +
            ", slidingWindowType=" + slidingWindowType +
            ", slowCallRateThreshold=" + slowCallRateThreshold +
            ", slowCallDurationThreshold=" + slowCallDurationThreshold +
            ", minConcurrentCalls=" + minConcurrentCalls +
            ", initialConcurrentCalls=" + initialConcurrentCalls +
            ", maxConcurrentCalls=" + maxConcurrentCalls +
            ", increaseAugend=" + increaseAugend +
            ", decreaseMultiplier=" + decreaseMultiplier +
            ", increaseMultiplier=" + increaseMultiplier +
            ", maxWaitDuration=" + maxWaitDuration +
            '}';
    }

    public enum SlidingWindowType {
        TIME_BASED, COUNT_BASED
    }

    /**
     * Returns a builder to create a custom AdaptiveBulkheadConfig.
     *
     * @return a {@link AdaptiveBulkheadConfig.Builder}
     */
    public static Builder from(AdaptiveBulkheadConfig baseConfig) {
        return AdaptiveBulkheadConfig.builder(baseConfig);
    }

    /**
     * Creates a default Bulkhead configuration.
     *
     * @return a default Bulkhead configuration.
     */
    public static AdaptiveBulkheadConfig ofDefaults() {
        return AdaptiveBulkheadConfig.custom().build();
    }

    /**
     * Returns a builder to create a custom AdaptiveBulkheadConfig.
     *
     * @return a {@link AdaptiveBulkheadConfig.Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Returns a builder to create a custom AdaptiveBulkheadConfig.
     *
     * @return a {@link AdaptiveBulkheadConfig.Builder}
     */
    public static Builder builder(AdaptiveBulkheadConfig bulkheadConfig) {
        return new Builder(bulkheadConfig);
    }

    public static class Builder implements Serializable {

        private float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD_PERCENTAGE;
        private int minimumNumberOfCalls = DEFAULT_MINIMUM_NUMBER_OF_CALLS;
        private int slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;
        private SlidingWindowType slidingWindowType = DEFAULT_SLIDING_WINDOW_TYPE;
        private float slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD_PERCENTAGE;
        private Duration slowCallDurationThreshold = Duration
            .ofSeconds(DEFAULT_SLOW_CALL_DURATION_THRESHOLD_SECONDS);
        private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
        private int minConcurrentCalls = DEFAULT_MIN_CONCURRENT_CALLS;
        private int maxConcurrentCalls = DEFAULT_MAX_CONCURRENT_CALLS;
        private int initialConcurrentCalls = DEFAULT_INITIAL_CONCURRENT_CALLS;
        private int increaseAugend = DEFAULT_INCREASE_AUGEND;
        private float decreaseMultiplier = DEFAULT_DECREASE_MULTIPLIER;
        private float increaseMultiplier = DEFAULT_INCREASE_MULTIPLIER;
        private int increaseInterval = DEFAULT_INCREASE_INTERVAL;
        private Duration maxWaitDuration = DEFAULT_MAX_WAIT_DURATION;
        @Nullable
        private transient Predicate<Throwable> recordExceptionPredicate;
        @Nullable
        private transient Predicate<Throwable> ignoreExceptionPredicate;
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] recordExceptions = new Class[0];
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] ignoreExceptions = new Class[0];
        private transient Function<Clock, Long> currentTimestampFunction = DEFAULT_TIMESTAMP_FUNCTION;
        private TimeUnit timestampUnit = DEFAULT_TIMESTAMP_UNIT;
        private transient Predicate<Object> recordResultPredicate = DEFAULT_RECORD_RESULT_PREDICATE;
        private boolean resetMetricsOnTransition = DEFAULT_RESET_METRICS_ON_TRANSITION;

        private Builder() {
        }

        private Builder(AdaptiveBulkheadConfig baseConfig) {
            this.slidingWindowSize = baseConfig.slidingWindowSize;
            this.slidingWindowType = baseConfig.slidingWindowType;
            this.minimumNumberOfCalls = baseConfig.minimumNumberOfCalls;
            this.failureRateThreshold = baseConfig.failureRateThreshold;
            this.ignoreExceptions = baseConfig.ignoreExceptions;
            this.recordExceptions = baseConfig.recordExceptions;
            this.recordExceptionPredicate = baseConfig.recordExceptionPredicate;
            this.writableStackTraceEnabled = baseConfig.writableStackTraceEnabled;
            this.slowCallRateThreshold = baseConfig.slowCallRateThreshold;
            this.slowCallDurationThreshold = baseConfig.slowCallDurationThreshold;
            this.minConcurrentCalls = baseConfig.minConcurrentCalls;
            this.maxConcurrentCalls = baseConfig.maxConcurrentCalls;
            this.initialConcurrentCalls = baseConfig.initialConcurrentCalls;
            this.increaseAugend = baseConfig.increaseAugend;
            this.decreaseMultiplier = baseConfig.decreaseMultiplier;
            this.increaseMultiplier = baseConfig.increaseMultiplier;
            this.increaseInterval = baseConfig.increaseInterval;
            this.currentTimestampFunction = baseConfig.currentTimestampFunction;
            this.timestampUnit = baseConfig.timestampUnit;
            this.recordResultPredicate = baseConfig.recordResultPredicate;
            this.resetMetricsOnTransition = baseConfig.resetMetricsOnTransition;
        }

        /**
         * Configures a threshold in percentage. The  AdaptiveBulkhead considers a call as slow when
         * the call duration is greater than {@link #slowCallDurationThreshold(Duration)}.
         *
         * <p>
         * The threshold must be greater than 0 and not greater than 100. Default value is 100
         * percentage which means that all recorded calls must be slower than {@link
         * #slowCallDurationThreshold(Duration)}.
         *
         * @param slowCallRateThreshold the slow calls' threshold in percentage
         * @return the Builder
         * @throws IllegalArgumentException if {@code slowCallRateThreshold <= 0 ||
         *                                  slowCallRateThreshold > 100}
         */
        public Builder slowCallRateThreshold(float slowCallRateThreshold) {
            if (slowCallRateThreshold <= 0 || slowCallRateThreshold > 100) {
                throw new IllegalArgumentException(
                    "slowCallRateThreshold must be between 1 and 100");
            }
            this.slowCallRateThreshold = slowCallRateThreshold;
            return this;
        }

        /**
         * Configures the duration threshold above which calls are considered as slow and increase
         * the slow calls' percentage. Default value is 60 seconds.
         *
         * @param slowCallDurationThreshold the duration above which calls are considered as slow
         * @return the Builder
         * @throws IllegalArgumentException if {@code slowCallDurationThreshold.toNanos() < 1}
         */
        public Builder slowCallDurationThreshold(Duration slowCallDurationThreshold) {
            if (slowCallDurationThreshold.toNanos() < 1) {
                throw new IllegalArgumentException(
                    "slowCallDurationThreshold must be at least 1[ns]");
            }
            this.slowCallDurationThreshold = slowCallDurationThreshold;
            return this;
        }

        /**
         * Configures the size of the sliding window which is used to record the outcome of calls.
         * {@code slidingWindowSize} configures the size of the sliding window.
         * <p>
         * The {@code slidingWindowSize} must be greater than 0.
         * <p>
         * Default slidingWindowSize is 100.
         *
         * @param slidingWindowSize the size of the sliding window when the AdaptiveBulkhead is
         *                          closed.
         * @return the Builder
         * @throws IllegalArgumentException if {@code slidingWindowSize < 1}
         */
        public Builder slidingWindowSize(int slidingWindowSize) {
            if (slidingWindowSize < 1) {
                throw new IllegalArgumentException("slidingWindowSize must be greater than 0");
            }
            this.slidingWindowSize = slidingWindowSize;
            return this;
        }

        /**
         * Configures the type of the sliding window which is used to record the outcome of calls.
         * Sliding window can either be count-based or time-based.
         * <p>
         * Default slidingWindowType is COUNT_BASED.
         *
         * @param slidingWindowType the type of the sliding window. Either COUNT_BASED or
         *                          TIME_BASED.
         * @return the Builder
         */
        public Builder slidingWindowType(SlidingWindowType slidingWindowType) {
            this.slidingWindowType = slidingWindowType;
            return this;
        }

        /**
         * Configures the failure rate threshold in percentage.
         * <p>
         * The threshold must be greater than 0 and not greater than 100. Default value is 50
         * percentage.
         *
         * @param failureRateThreshold the failure rate threshold in percentage
         * @return the Builder
         * @throws IllegalArgumentException if {@code failureRateThreshold <= 0 ||
         *                                  failureRateThreshold > 100}
         */
        public Builder failureRateThreshold(float failureRateThreshold) {
            if (failureRateThreshold <= 0 || failureRateThreshold > 100) {
                throw new IllegalArgumentException(
                    "failureRateThreshold must be between 1 and 100");
            }
            this.failureRateThreshold = failureRateThreshold;
            return this;
        }

        /**
         * Configures a Predicate which evaluates if an exception should be ignored and neither count as a failure nor success.
         * The Predicate must return true if the exception must be ignored .
         * The Predicate must return false, if the exception must count as a failure.
         *
         * @param predicate the Predicate which checks if an exception should count as a failure
         * @return the Builder
         */
        public final Builder ignoreException(Predicate<Throwable> predicate) {
            this.ignoreExceptionPredicate = predicate;
            return this;
        }

        /**
         * Configures a list of error classes that are recorded as a failure and thus increase the failure rate.
         * an exception matching or inheriting from one of the list should count as a failure
         *
         * @param errorClasses the error classes which are recorded
         * @return the Builder
         * @see #ignoreExceptions(Class[]) ). Ignoring an exception has more priority over recording an exception.
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder recordExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
            this.recordExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Configures a Predicate which evaluates if an exception should be recorded as a failure and thus increase the failure rate.
         * The Predicate must return true if the exception should count as a failure. The Predicate must return false, if the exception should count as a success
         * ,unless the exception is explicitly ignored by {@link #ignoreExceptions(Class[])} or {@link #ignoreException(Predicate)}.
         *
         * @param predicate the Predicate which checks if an exception should count as a failure
         * @return the Builder
         */
        public final Builder recordException(Predicate<Throwable> predicate) {
            this.recordExceptionPredicate = predicate;
            return this;
        }

        /**
         * Configures a list of error classes that are ignored and thus neither count as a failure nor success.
         * an exception matching or inheriting from one of that list will not count as a failure nor success
         *
         * @param errorClasses the error classes which are ignored
         * @return the Builder
         * @see #recordExceptions(Class[]) . Ignoring an exception has priority over recording an exception.
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder ignoreExceptions(@Nullable Class<? extends Throwable>... errorClasses) {
            this.ignoreExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Configures the minimum number of calls which are required (per sliding window period)
         * before the AdaptiveBulkhead can calculate the error or slow rate. For example, if {@code
         * minimumNumberOfCalls} is 10, then at least 10 calls must be recorded, before the failure
         * rate can be calculated.
         * <p>
         * Default minimumNumberOfCalls is 100
         *
         * @param minimumNumberOfCalls the minimum number of calls that must be recorded before the
         *                             failure rate can be calculated.
         * @return the Builder
         * @throws IllegalArgumentException if {@code minimumNumberOfCalls < 1}
         */
        public Builder minimumNumberOfCalls(int minimumNumberOfCalls) {
            if (minimumNumberOfCalls < 1) {
                throw new IllegalArgumentException("minimumNumberOfCalls must be greater than 0");
            }
            this.minimumNumberOfCalls = minimumNumberOfCalls;
            return this;
        }

        public final Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            this.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        public final Builder minConcurrentCalls(int minConcurrentCalls) {
            if (minConcurrentCalls <= 0) {
                throw new IllegalArgumentException(
                    "minConcurrentCalls must greater than 0");
            }
            this.minConcurrentCalls = minConcurrentCalls;
            return this;
        }

        public final Builder maxConcurrentCalls(int maxConcurrentCalls) {
            if (maxConcurrentCalls <= 0) {
                throw new IllegalArgumentException(
                    "maxConcurrentCalls must greater than 0");
            }
            this.maxConcurrentCalls = maxConcurrentCalls;
            return this;
        }

        public final Builder initialConcurrentCalls(int initialConcurrentCalls) {
            if (initialConcurrentCalls <= 0) {
                throw new IllegalArgumentException(
                    "initialConcurrentCalls must greater than 0");
            }
            this.initialConcurrentCalls = initialConcurrentCalls;
            return this;
        }

        public final Builder increaseAugend(int increaseAugend) {
            if (increaseAugend <= 0) {
                throw new IllegalArgumentException(
                    "increaseAugend must greater than 0");
            }
            this.increaseAugend = increaseAugend;
            return this;
        }

        public final Builder decreaseMultiplier(float decreaseMultiplier) {
            if (decreaseMultiplier < 0 || decreaseMultiplier > 1) {
                throw new IllegalArgumentException(
                    "decreaseMultiplier must be between 0 and 1");
            }
            this.decreaseMultiplier = decreaseMultiplier;
            return this;
        }

        public final Builder increaseMultiplier(float increaseMultiplier) {
            if (increaseMultiplier <= 1) {
                throw new IllegalArgumentException(
                    "increaseMultiplier must greater than 1");
            }
            this.increaseMultiplier = increaseMultiplier;
            return this;
        }

        /**
         * Increase concurrency limit every nth success in SlowStartState where n is a value of increaseInterval.
         *
         * @param increaseInterval minimum 1
         * @return the Builder
         */
        public final Builder increaseInterval(int increaseInterval) {
            if (increaseInterval < 1) {
                throw new IllegalArgumentException(
                    "increaseInterval must be at least 1");
            }
            this.increaseInterval = increaseInterval;
            return this;
        }

        /**
         * Configures a maximum amount of time which the calling thread will wait to enter the
         * bulkhead. If bulkhead has space available, entry is guaranteed and immediate. If bulkhead
         * is full, calling threads will contest for space, if it becomes available. maxWaitDuration
         * can be set to 0.
         * <p>
         * Note: for threads running on an event-loop or equivalent (rx computation pool, etc),
         * setting maxWaitDuration to 0 is highly recommended. Blocking an event-loop thread will
         * most likely have a negative effect on application throughput.
         *
         * @param maxWaitDuration maximum wait time for bulkhead entry
         * @return the Builder
         */
        public AdaptiveBulkheadConfig.Builder maxWaitDuration(Duration maxWaitDuration) {
            if (maxWaitDuration.toMillis() < 0) {
                throw new IllegalArgumentException(
                    "maxWaitDuration must be a positive integer value >= 0");
            }
            this.maxWaitDuration = maxWaitDuration;
            return this;
        }

        /**
         * Configures a function that returns current timestamp for the bulkhead.
         * Default implementation uses System.nanoTime() to compute current timestamp.
         * Configure currentTimestampFunction to provide different implementation to compute current timestamp.
         * <p>
         *
         * @param currentTimestampFunction function that computes current timestamp.
         * @param timeUnit                 TimeUnit of timestamp returned by the function.
         * @return the Builder
         */
        public Builder currentTimestampFunction(Function<Clock, Long> currentTimestampFunction, TimeUnit timeUnit) {
            this.timestampUnit = timeUnit;
            this.currentTimestampFunction = currentTimestampFunction;
            return this;
        }

        /**
         * Configures a Predicate which evaluates if the result of the protected function call
         * should be recorded as a failure and thus increase the failure rate.
         * The Predicate must return true if the result should count as a failure.
         * The Predicate must return false, if the result should count as a success.
         *
         * @param recordResultPredicate the Predicate which evaluates if a result should count as a failure
         * @return the Builder
         */
        public Builder recordResult(Predicate<Object> recordResultPredicate) {
            this.recordResultPredicate = recordResultPredicate;
            return this;
        }

        public Builder resetMetricsOnTransition(boolean resetMetricsOnTransition) {
            this.resetMetricsOnTransition = resetMetricsOnTransition;
            return this;
        }

        public AdaptiveBulkheadConfig build() {
            AdaptiveBulkheadConfig config = new AdaptiveBulkheadConfig();
            config.slidingWindowType = slidingWindowType;
            config.slowCallDurationThreshold = slowCallDurationThreshold;
            config.slowCallRateThreshold = slowCallRateThreshold;
            config.failureRateThreshold = failureRateThreshold;
            config.slidingWindowSize = slidingWindowSize;
            config.minimumNumberOfCalls = minimumNumberOfCalls;
            config.recordExceptions = recordExceptions;
            config.ignoreExceptions = ignoreExceptions;
            config.writableStackTraceEnabled = writableStackTraceEnabled;
            config.minConcurrentCalls = minConcurrentCalls;
            config.maxConcurrentCalls = maxConcurrentCalls;
            config.initialConcurrentCalls = initialConcurrentCalls;
            config.increaseAugend = increaseAugend;
            config.decreaseMultiplier = decreaseMultiplier;
            config.increaseMultiplier = increaseMultiplier;
            config.increaseInterval = increaseInterval;
            config.maxWaitDuration = maxWaitDuration;
            config.recordExceptionPredicate = PredicateCreator
                .createExceptionsPredicate(recordExceptionPredicate, recordExceptions)
                .orElse(DEFAULT_RECORD_EXCEPTION_PREDICATE);
            config.ignoreExceptionPredicate = PredicateCreator
                .createExceptionsPredicate(ignoreExceptionPredicate, ignoreExceptions)
                .orElse(DEFAULT_IGNORE_EXCEPTION_PREDICATE);
            config.currentTimestampFunction = currentTimestampFunction;
            config.timestampUnit = timestampUnit;
            config.recordResultPredicate = recordResultPredicate;
            config.resetMetricsOnTransition = resetMetricsOnTransition;
            return config;
        }

    }

}
