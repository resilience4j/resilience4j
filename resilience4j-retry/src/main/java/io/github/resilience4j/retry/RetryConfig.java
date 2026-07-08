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
package io.github.resilience4j.retry;


import io.github.resilience4j.core.IntervalBiFunction;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.predicate.PredicateCreator;
import io.github.resilience4j.retry.internal.BudgetRetryImpl;
import io.github.resilience4j.retry.internal.SlidingWindowSynchronizationStrategy;

import java.io.Serializable;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryConfig implements Serializable {

    private static final long serialVersionUID = 3522903275067138911L;

    public static final long DEFAULT_WAIT_DURATION = 500;
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final IntervalFunction DEFAULT_INTERVAL_FUNCTION = numOfAttempts -> DEFAULT_WAIT_DURATION;
    private static final IntervalBiFunction DEFAULT_INTERVAL_BI_FUNCTION = IntervalBiFunction.ofIntervalFunction(DEFAULT_INTERVAL_FUNCTION);
    private static final Predicate<Throwable> DEFAULT_RECORD_FAILURE_PREDICATE = throwable -> true;


    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] retryExceptions = new Class[0];
    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] ignoreExceptions = new Class[0];

    @Nullable
    private transient Predicate<Throwable> retryOnExceptionPredicate;

    @Nullable
    private transient Predicate retryOnResultPredicate;

    @Nullable
    private BiConsumer consumeResultBeforeRetryAttempt;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private boolean failAfterMaxAttempts = false;
    private boolean writableStackTraceEnabled = true;

    private SlidingWindowSynchronizationStrategy synchronizationStrategy = SlidingWindowSynchronizationStrategy.LOCK_FREE;
    private int windowSize = 10;
    private double maxRetryRatio = 10.0;
    private Clock clock = Clock.systemUTC();
    private long minSampleSize = 100;

    @Nullable
    private transient IntervalFunction intervalFunction;

    private transient IntervalBiFunction intervalBiFunction = DEFAULT_INTERVAL_BI_FUNCTION;

    // The final exception predicate
    private transient Predicate<Throwable> exceptionPredicate;

    private RetryConfig() {
    }

    /**
     * Returns a builder to create a custom RetryConfig.
     *
     * @param <T> The type being built.
     * @return a {@link Builder}
     */
    public static <T> Builder<T> custom() {
        return new Builder<>();
    }

    public static <T> Builder<T> from(RetryConfig baseConfig) {
        return new Builder<>(baseConfig);
    }

    /**
     * Creates a default Retry configuration.
     *
     * @return a default Retry configuration.
     */
    public static RetryConfig ofDefaults() {
        return new Builder().build();
    }

    /**
     * @return the maximum allowed attempts to make.
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * @return if exception should be thrown after max attempts are made with no satisfactory result
     */
    public boolean isFailAfterMaxAttempts() {
        return failAfterMaxAttempts;
    }

    /**
     * @return if any thrown {@link MaxRetriesExceededException} should contain a stacktrace
     */
    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }

    /**
     * @return the sliding window synchronization strategy for {@link BudgetRetryImpl}
     */
    public SlidingWindowSynchronizationStrategy getSynchronizationStrategy() {
        return synchronizationStrategy;
    }

    /**
     * @return the sliding window size for {@link BudgetRetryImpl}
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * @return the sliding window failure/total ratio for {@link BudgetRetryImpl}
     */
    public double getMaxRetryRatio() {
        return maxRetryRatio;
    }

    /**
     * Use {@link RetryConfig#intervalBiFunction} instead, this method is kept for backwards compatibility
     */
    @Nullable
    @Deprecated
    public Function<Integer, Long> getIntervalFunction() {
        return intervalFunction;
    }

    /**
     * Return the IntervalBiFunction which calculates wait interval based on result or exception
     *
     * @param <T> The type of result.
     * @return the interval bi function
     */
    @SuppressWarnings("unchecked")
    public <T> IntervalBiFunction<T> getIntervalBiFunction() {
        return intervalBiFunction;
    }

    public Predicate<Throwable> getExceptionPredicate() {
        return exceptionPredicate;
    }

    /**
     * Return the Predicate which evaluates if a result should be retried. The Predicate must
     * return true if the result should be retried, otherwise it must return false.
     *
     * @param <T> The type of result.
     * @return the result predicate
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Predicate<T> getResultPredicate() {
        return retryOnResultPredicate;
    }

    /**
     * Return the BiConsumer which performs post actions if the result should be retried.
     *
     * @param <T> The type of result.
     * @return the onFailureBiConsumer
     */
    public <T> BiConsumer<Integer, T> getConsumeResultBeforeRetryAttempt(){
        return consumeResultBeforeRetryAttempt;
    }
    @Override
    public String toString() {
        StringBuilder retryConfig = new StringBuilder("RetryConfig {");
        retryConfig.append("maxAttempts=");
        retryConfig.append(maxAttempts);
        retryConfig.append(", failAfterMaxAttempts=");
        retryConfig.append(failAfterMaxAttempts);
        retryConfig.append(", writableStackTraceEnabled=");
        retryConfig.append(writableStackTraceEnabled);
        retryConfig.append(", intervalFunction=");
        retryConfig.append(intervalFunction);
        retryConfig.append(", retryOnExceptionPredicate=");
        retryConfig.append(retryOnExceptionPredicate);
        retryConfig.append(", retryOnResultPredicate=");
        retryConfig.append(retryOnResultPredicate);
        retryConfig.append(", intervalBiFunction=");
        retryConfig.append(intervalBiFunction);
        retryConfig.append(", consumeResultBeforeRetryAttempt=");
        retryConfig.append(consumeResultBeforeRetryAttempt);
        retryConfig.append(", retryExceptions=");
        retryConfig.append(Arrays.toString(retryExceptions));
        retryConfig.append(", ignoreExceptions=");
        retryConfig.append(Arrays.toString(ignoreExceptions));
        retryConfig.append(", slidingWindowSynchronizationStrategy=");
        retryConfig.append(synchronizationStrategy);
        retryConfig.append(", windowSize=");
        retryConfig.append(windowSize);
        retryConfig.append(", ratio=");
        retryConfig.append(maxRetryRatio);
        retryConfig.append(", clock=");
        retryConfig.append(clock);
        retryConfig.append(", minSampleSize=");
        retryConfig.append(minSampleSize);
        retryConfig.append("}");
        return retryConfig.toString();
    }

    public Clock getClock() {
        return clock;
    }

    public long getMinSampleSize() {
        return minSampleSize;
    }

    public static class Builder<T> {

        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private boolean failAfterMaxAttempts = false;
        private boolean writableStackTraceEnabled = true;

        private SlidingWindowSynchronizationStrategy synchronizationStrategy = SlidingWindowSynchronizationStrategy.LOCK_FREE;
        private int windowSize = 10;
        private double maxRetryRatio = 10.0;
        private Clock clock = Clock.systemUTC();
        private long minSampleSize = 100;

        @Nullable
        private IntervalFunction intervalFunction;

        @Nullable
        private Predicate<Throwable> retryOnExceptionPredicate;
        @Nullable
        private Predicate<T> retryOnResultPredicate;

        @Nullable
        private IntervalBiFunction<T> intervalBiFunction;

        @Nullable
        BiConsumer<Integer, T> consumeResultBeforeRetryAttempt;

        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] retryExceptions = new Class[0];
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] ignoreExceptions = new Class[0];

        public Builder() {
        }

        @SuppressWarnings("unchecked")
        public Builder(RetryConfig baseConfig) {
            this.maxAttempts = baseConfig.maxAttempts;
            this.retryOnExceptionPredicate = baseConfig.retryOnExceptionPredicate;
            this.retryOnResultPredicate = baseConfig.retryOnResultPredicate;
            this.consumeResultBeforeRetryAttempt = baseConfig.consumeResultBeforeRetryAttempt;
            this.failAfterMaxAttempts = baseConfig.failAfterMaxAttempts;
            this.writableStackTraceEnabled = baseConfig.writableStackTraceEnabled;
            this.retryExceptions = baseConfig.retryExceptions;
            this.ignoreExceptions = baseConfig.ignoreExceptions;
            this.synchronizationStrategy = baseConfig.synchronizationStrategy;
            this.windowSize = baseConfig.windowSize;
            this.maxRetryRatio = baseConfig.maxRetryRatio;
            this.clock = baseConfig.clock;
            this.minSampleSize = baseConfig.minSampleSize;
            if (baseConfig.intervalFunction != null) {
                this.intervalFunction = baseConfig.intervalFunction;
            } else {
                this.intervalBiFunction = baseConfig.intervalBiFunction;
            }
        }

        public Builder<T> maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException(
                    "maxAttempts must be greater than or equal to 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder<T> waitDuration(Duration waitDuration) {
            if (waitDuration.toMillis() >= 0) {
                this.intervalBiFunction((attempt, either) -> waitDuration.toMillis());
            } else {
                throw new IllegalArgumentException(
                    "waitDuration must be a positive value");
            }
            return this;
        }

        /**
         * Configures a Predicate which evaluates if an result should be retried. The Predicate must
         * return true if the result should be retried, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an result should be retried or not.
         * @return the RetryConfig.Builder
         */
        public Builder<T> retryOnResult(Predicate<T> predicate) {
            this.retryOnResultPredicate = predicate;
            return this;
        }

        /**
         * Configure a BiConsumer which performs post actions if a result should be retried.
         *
         * @param consumeResultBeforeRetryAttempt the BiConsumer which performs post actions if a result should be retried.
         * @return the RetryConfig.Builder
         */
        public Builder<T> consumeResultBeforeRetryAttempt(BiConsumer<Integer, T> consumeResultBeforeRetryAttempt){
            this.consumeResultBeforeRetryAttempt = consumeResultBeforeRetryAttempt;
            return this;
        }

        /**
         * Configures the Retry to throw a {@link MaxRetriesExceeded} exception once {@link #maxAttempts} has been reached,
         * and the result is still not satisfactory (according to {@link #retryOnResultPredicate})
         *
         * @param bool a boolean flag to enable or disable throwing. (Default is {@code false}
         * @return the RetryConfig.Builder
         */
        public Builder<T> failAfterMaxAttempts(boolean bool) {
            this.failAfterMaxAttempts = bool;
            return this;
        }

        /**
         * Enables writable stack traces. When set to false, {@link Exception#getStackTrace()}
         * returns a zero length array. This may be used to reduce log spam when the Retry
         * has exceeded the maximum nbr of attempts, and flag {@link #failAfterMaxAttempts} has been enabled.
         * The thrown {@link MaxRetriesExceededException} will then have no stacktrace.
         *
         * @param bool the flag to enable writable stack traces.
         * @return the RetryConfig.Builder
         */
        public Builder<T> writableStackTraceEnabled(boolean bool) {
            this.writableStackTraceEnabled = bool;
            return this;
        }

        /**
         * Set a function to modify the waiting interval after a failure. By default the interval
         * stays the same.
         *
         * @param f Function to modify the interval after a failure
         * @return the RetryConfig.Builder
         */
        public Builder<T> intervalFunction(IntervalFunction f) {
            this.intervalFunction = f;
            this.intervalBiFunction = null;
            return this;
        }

        /**
         * Set a function to modify the waiting interval after a failure based on attempt number and result or exception.
         *
         * @param f Function to modify the interval after a failure
         * @return the RetryConfig.Builder
         */
        public Builder<T> intervalBiFunction(IntervalBiFunction<T> f) {
            this.intervalBiFunction = f;
            this.intervalFunction = null;
            return this;
        }

        /**
         * Configures a Predicate which evaluates if an exception should be retried. The Predicate
         * must return true if the exception should be retried, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be retried or not.
         * @return the RetryConfig.Builder
         */
        public Builder<T> retryOnException(Predicate<Throwable> predicate) {
            this.retryOnExceptionPredicate = predicate;
            return this;
        }

        /**
         * Configures a list of error classes that are recorded as a failure and thus are retried.
         * Any exception matching or inheriting from one of the list will be retried, unless ignored
         * via
         *
         * @param errorClasses the error classes that are retried
         * @return the RetryConfig.Builder
         * @see #ignoreExceptions(Class[]) ). Ignoring an exception has priority over retrying an
         * exception.
         * <p>
         * Example: retryOnExceptions(Throwable.class) and ignoreExceptions(RuntimeException.class)
         * would retry all Errors and checked Exceptions, and ignore unchecked
         * <p>
         * For a more sophisticated exception management use the
         * @see #retryOnException(Predicate) method
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder<T> retryExceptions(
            @Nullable Class<? extends Throwable>... errorClasses) {
            this.retryExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Configures a list of error classes that are ignored and thus are not retried. Any
         * exception matching or inheriting from one of the list will not be retried, even if marked
         * via
         *
         * @param errorClasses the error classes that are retried
         * @return the RetryConfig.Builder
         * @see #retryExceptions(Class[]) . Ignoring an exception has priority over retrying an
         * exception.
         * <p>
         * Example: ignoreExceptions(Throwable.class) and retryOnExceptions(Exception.class) would
         * capture nothing
         * <p>
         * Example: ignoreExceptions(Exception.class) and retryOnExceptions(Throwable.class) would
         * capture Errors
         * <p>
         * For a more sophisticated exception management use the
         * @see #retryOnException(Predicate) method
         */
        @SuppressWarnings("unchecked")
        @SafeVarargs
        public final Builder<T> ignoreExceptions(
            @Nullable Class<? extends Throwable>... errorClasses) {
            this.ignoreExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        public Builder<T> slidingWindowSynchronizationStrategy(SlidingWindowSynchronizationStrategy synchronizationStrategy) {
            this.synchronizationStrategy = synchronizationStrategy;
            return this;
        }

        public Builder<T> windowSize(int windowSize) {
            if (windowSize < 1) {
                throw new IllegalArgumentException(
                        "windowSize must be greater than or equal to 1");
            }

            this.windowSize = windowSize;
            return this;
        }

        public Builder<T> maxRetryRatio(double maxRetryRatio) {
            if (maxRetryRatio < 0) {
                throw new IllegalArgumentException(
                        "maxRetryRatio must be greater than or equal to 0");
            }

            this.maxRetryRatio = maxRetryRatio;
            return this;
        }

        public Builder<T> clock(Clock clock) {
            this.clock = clock;

            return this;
        }

        public Builder<T> minSampleSize(long minSampleSize) {
            if (minSampleSize < 0) {
                throw new IllegalArgumentException(
                        "minSampleSize must be greater than or equal to 0");
            }

            this.minSampleSize = minSampleSize;
            return this;
        }

        public RetryConfig build() {
            if (intervalFunction != null && intervalBiFunction != null) {
                throw new IllegalStateException("The intervalFunction was configured twice which could result in an" +
                    " undesired state. Please use either intervalFunction or intervalBiFunction.");
            }
            RetryConfig config = new RetryConfig();
            config.maxAttempts = maxAttempts;
            config.failAfterMaxAttempts = failAfterMaxAttempts;
            config.writableStackTraceEnabled = writableStackTraceEnabled;
            config.retryOnExceptionPredicate = retryOnExceptionPredicate;
            config.retryOnResultPredicate = retryOnResultPredicate;
            config.consumeResultBeforeRetryAttempt = consumeResultBeforeRetryAttempt;
            config.retryExceptions = retryExceptions;
            config.ignoreExceptions = ignoreExceptions;
            config.exceptionPredicate = createExceptionPredicate();
            config.intervalFunction = createIntervalFunction();
            config.intervalBiFunction = Optional.ofNullable(intervalBiFunction)
                .orElse(IntervalBiFunction.ofIntervalFunction(config.intervalFunction));
            config.synchronizationStrategy = synchronizationStrategy;
            config.windowSize = windowSize;
            config.maxRetryRatio = maxRetryRatio;
            config.clock = clock;
            config.minSampleSize = minSampleSize;
            return config;
        }

        @Nullable
        private IntervalFunction createIntervalFunction() {
            if (intervalFunction == null && intervalBiFunction == null) {
                return IntervalFunction.ofDefaults();
            }
            return intervalFunction;
        }

        private Predicate<Throwable> createExceptionPredicate() {
            return createRetryOnExceptionPredicate()
                .and(PredicateCreator.createNegatedExceptionsPredicate(ignoreExceptions)
                    .orElse(DEFAULT_RECORD_FAILURE_PREDICATE));
        }

        private Predicate<Throwable> createRetryOnExceptionPredicate() {
            return PredicateCreator.createExceptionsPredicate(retryOnExceptionPredicate, retryExceptions)
                .orElse(DEFAULT_RECORD_FAILURE_PREDICATE);
        }
    }
}
