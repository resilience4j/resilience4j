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
package io.github.robwin.retry;


import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryConfig {

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_WAIT_DURATION = 500;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    private IntervalFunction intervalFunction = (x) -> DEFAULT_WAIT_DURATION;
    // The default exception predicate retries all exceptions.
    private Predicate<Throwable> exceptionPredicate = (exception) -> true;

    private RetryConfig(){
    }

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
     * @return a {@link RetryConfig.Builder}
     */
    public static RetryConfig.Builder custom(){
        return new RetryConfig.Builder();
    }

    /**
     * Creates a default Retry configuration.
     *
     * @return a default Retry configuration.
     */
    public static RetryConfig ofDefaults(){
        return new RetryConfig.Builder().build();
    }

    public static class Builder {
        private RetryConfig config = new RetryConfig();

        public RetryConfig.Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
            }
            config.maxAttempts = maxAttempts;
            return this;
        }

        public RetryConfig.Builder waitDuration(Duration waitDuration) {
            if (waitDuration.toMillis() < 10) {
                throw new IllegalArgumentException("waitDurationInOpenState must be at least 10ms");
            }
            config.intervalFunction = (x) -> waitDuration.toMillis();
            return this;
        }

        /**
         * Set a function to modify the waiting interval
         * after a failure. By default the interval stays
         * the same.
         *
         * @param f Function to modify the interval after a failure
         */
        public RetryConfig.Builder intervalFunction(IntervalFunction f) {
            config.intervalFunction = f;
            return this;
        }

        /**
         *  Configures a Predicate which evaluates if an exception should be retried.
         *  The Predicate must return true if the exception should count be retried, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be retried or not.
         * @return the CircuitBreakerConfig.Builder
         */
        public RetryConfig.Builder retryOnException(Predicate<Throwable> predicate) {
            config.exceptionPredicate = predicate;
            return this;
        }

        public RetryConfig build() {
            return config;
        }
    }
}
