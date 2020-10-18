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


import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.core.lang.Nullable;
import io.github.resilience4j.core.predicate.PredicateCreator;

import java.io.Serializable;
import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryConfig implements Serializable {

    private static final long serialVersionUID = 3522903275067138911L;

    public static final long DEFAULT_WAIT_DURATION = 500;
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final IntervalFunction DEFAULT_INTERVAL_FUNCTION = numOfAttempts -> DEFAULT_WAIT_DURATION;
    private static final Predicate<Throwable> DEFAULT_RECORD_FAILURE_PREDICATE = throwable -> true;

    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] retryExceptions = new Class[0];
    @SuppressWarnings("unchecked")
    private Class<? extends Throwable>[] ignoreExceptions = new Class[0];

    @Nullable
    private Predicate<Throwable> retryOnExceptionPredicate;
    @Nullable
    private Predicate retryOnResultPredicate;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private IntervalFunction intervalFunction = DEFAULT_INTERVAL_FUNCTION;

    // The final exception predicate
    private Predicate<Throwable> exceptionPredicate;

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

    public Function<Integer, Long> getIntervalFunction() {
        return intervalFunction;
    }

    public Predicate<Throwable> getExceptionPredicate() {
        return exceptionPredicate;
    }

    /**
     * Return the Predicate which evaluates if an result should be retried. The Predicate must
     * return true if the result should  be retried, otherwise it must return false.
     *
     * @param <T> The type of result.
     * @return the result predicate
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Predicate<T> getResultPredicate() {
        return retryOnResultPredicate;
    }

    public static class Builder<T> {

        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private IntervalFunction intervalFunction = IntervalFunction.ofDefaults();

        @Nullable
        private Predicate<Throwable> retryOnExceptionPredicate;
        @Nullable
        private Predicate<T> retryOnResultPredicate;

        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] retryExceptions = new Class[0];
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] ignoreExceptions = new Class[0];


        public Builder() {
        }

        @SuppressWarnings("unchecked")
        public Builder(RetryConfig baseConfig) {
            this.maxAttempts = baseConfig.maxAttempts;
            this.intervalFunction = baseConfig.intervalFunction;
            this.retryOnExceptionPredicate = baseConfig.retryOnExceptionPredicate;
            this.retryOnResultPredicate = baseConfig.retryOnResultPredicate;
            this.retryExceptions = baseConfig.retryExceptions;
            this.ignoreExceptions = baseConfig.ignoreExceptions;
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
                this.intervalFunction = (x) -> waitDuration.toMillis();
            } else {
                throw new IllegalArgumentException(
                    "waitDurationInOpenState must be a positive value");
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
         * Set a function to modify the waiting interval after a failure. By default the interval
         * stays the same.
         *
         * @param f Function to modify the interval after a failure
         * @return the RetryConfig.Builder
         */
        public Builder<T> intervalFunction(IntervalFunction f) {
            this.intervalFunction = f;
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

        public RetryConfig build() {
            RetryConfig config = new RetryConfig();
            config.intervalFunction = intervalFunction;
            config.maxAttempts = maxAttempts;
            config.retryOnExceptionPredicate = retryOnExceptionPredicate;
            config.retryOnResultPredicate = retryOnResultPredicate;
            config.retryExceptions = retryExceptions;
            config.ignoreExceptions = ignoreExceptions;
            config.exceptionPredicate = createExceptionPredicate();
            return config;
        }

        private Predicate<Throwable> createExceptionPredicate() {
            return createRetryOnExceptionPredicate()
                .and(PredicateCreator.createNegatedExceptionsPredicate(ignoreExceptions)
                    .orElse(DEFAULT_RECORD_FAILURE_PREDICATE));
        }

        private Predicate<Throwable> createRetryOnExceptionPredicate() {
            return PredicateCreator.createExceptionsPredicate(retryExceptions)
                .map(predicate -> retryOnExceptionPredicate != null ? predicate
                    .or(retryOnExceptionPredicate) : predicate)
                .orElseGet(() -> retryOnExceptionPredicate != null ? retryOnExceptionPredicate
                    : DEFAULT_RECORD_FAILURE_PREDICATE);
        }
    }
}
