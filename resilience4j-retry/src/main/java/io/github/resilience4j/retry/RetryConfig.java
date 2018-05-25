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


import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryConfig {

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_WAIT_DURATION = 500;
    public static final IntervalFunction DEFAULT_INTERVAL_FUNCTION = (numOfAttempts) -> DEFAULT_WAIT_DURATION;
    public static final Predicate<Throwable> DEFAULT_RECORD_FAILURE_PREDICATE = (throwable) -> true;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    private IntervalFunction intervalFunction = DEFAULT_INTERVAL_FUNCTION;
    // The default exception predicate retries all exceptions.
    private Predicate<Throwable> exceptionPredicate = DEFAULT_RECORD_FAILURE_PREDICATE;

    private RetryConfig(){
    }

    /**
     * @return the maximum allowed retries to make.
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
     * Returns a builder to create a custom RetryConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder custom(){
        return new Builder();
    }

    /**
     * Creates a default Retry configuration.
     *
     * @return a default Retry configuration.
     */
    public static RetryConfig ofDefaults(){
        return new Builder().build();
    }

    public static class Builder {
        private Predicate<Throwable> retryExceptionPredicate;
        private Predicate<Throwable> exceptionPredicate;
        private IntervalFunction intervalFunction = IntervalFunction.ofDefaults();
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] retryExceptions = new Class[0];
        @SuppressWarnings("unchecked")
        private Class<? extends Throwable>[] ignoreExceptions = new Class[0];
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        public Builder waitDuration(Duration waitDuration) {
            if (waitDuration.toMillis() < 10) {
                throw new IllegalArgumentException("waitDurationInOpenState must be at least 10ms");
            }
            this.intervalFunction = (x) -> waitDuration.toMillis();
            return this;
        }

        /**
         * Set a function to modify the waiting interval
         * after a failure. By default the interval stays
         * the same.
         *
         * @param f Function to modify the interval after a failure
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder intervalFunction(IntervalFunction f) {
            this.intervalFunction = f;
            return this;
        }

        /**
         *  Configures a Predicate which evaluates if an exception should be retried.
         *  The Predicate must return true if the exception should count be retried, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be retried or not.
         * @return the RetryConfig.Builder
         */
        public Builder retryOnException(Predicate<Throwable> predicate) {
            this.retryExceptionPredicate = predicate;
            return this;
        }


        /**
         * Configures a list of error classes that are recorded as a failure and thus increase the failure rate.
         * Any exception matching or inheriting from one of the list will be retried, unless ignored via
         * @see #ignoreExceptions(Class[]) ). Ignoring an exception has priority over retrying an exception.
         *
         * Example:
         *  retryOnExceptions(Throwable.class) and ignoreExceptions(RuntimeException.class)
         *  would retry all Errors and checked Exceptions, and ignore unchecked
         *
         *  For a more sophisticated exception management use the
         *  @see #retryOnException(Predicate) method
         *
         * @param errorClasses the error classes that are retried
         * @return the RetryConfig.Builder
         */
        @SafeVarargs
        @SuppressWarnings("unchecked")
        public final Builder retryExceptions(Class<? extends Throwable>... errorClasses) {
            this.retryExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }

        /**
         * Configures a list of error classes that are ignored and thus are not retried.
         * Any exception matching or inheriting from one of the list will not be retried, even if marked via
         * @see #retryExceptions(Class[]) . Ignoring an exception has priority over retrying an exception.
         *
         * Example:
         *  ignoreExceptions(Throwable.class) and retryOnExceptions(Exception.class)
         *  would capture nothing
         *
         * Example:
         *  ignoreExceptions(Exception.class) and retryOnExceptions(Throwable.class)
         *  would capture Errors
         *
         *  For a more sophisticated exception management use the
         *  @see #retryOnException(Predicate) method
         *
         * @param errorClasses the error classes that are retried
         * @return the RetryConfig.Builder
         */
        @SafeVarargs
        @SuppressWarnings("unchecked")
        public final Builder ignoreExceptions(Class<? extends Throwable>... errorClasses) {
            this.ignoreExceptions = errorClasses != null ? errorClasses : new Class[0];
            return this;
        }


        public RetryConfig build() {
            buildExceptionPredicate();
            RetryConfig config = new RetryConfig();
            config.intervalFunction = intervalFunction;
            config.maxAttempts = maxAttempts;
            config.exceptionPredicate = exceptionPredicate;
            return config;
        }

        private void buildExceptionPredicate() {
            this.exceptionPredicate =
                    getRetryPredicate()
                            .and(buildIgnoreExceptionsPredicate()
                                    .orElse(DEFAULT_RECORD_FAILURE_PREDICATE));
        }

        private Predicate<Throwable> getRetryPredicate() {
            return buildRetryExceptionsPredicate()
                    .map(predicate -> retryExceptionPredicate != null ? predicate.or(retryExceptionPredicate) : predicate)
                    .orElseGet(() -> retryExceptionPredicate != null ? retryExceptionPredicate : DEFAULT_RECORD_FAILURE_PREDICATE);
        }

        private Optional<Predicate<Throwable>> buildRetryExceptionsPredicate() {
            return Arrays.stream(retryExceptions)
                    .distinct()
                    .map(Builder::makePredicate)
                    .reduce(Predicate::or);
        }

        private Optional<Predicate<Throwable>> buildIgnoreExceptionsPredicate() {
            return Arrays.stream(ignoreExceptions)
                    .distinct()
                    .map(Builder::makePredicate)
                    .reduce(Predicate::or)
                    .map(Predicate::negate);
        }

        static Predicate<Throwable> makePredicate(Class<? extends Throwable> exClass) {
            return (Throwable e) -> exClass.isAssignableFrom(e.getClass());
        }
    }
}
