/*
 *
 *  Copyright 2016 Robert Winkler, Jan Sykora at GoodData(R) Corporation
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


import io.vavr.CheckedConsumer;

import java.time.Duration;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryConfig {
    public static final int DEFAULT_BUFFER_SIZE = 100;
    public static final double DEFAULT_RETRY_THRESHOLD = 0.2;
    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_WAIT_DURATION = 500;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private double retryThreshold = DEFAULT_RETRY_THRESHOLD;

    private IntervalFunction intervalFunction = (numOfAttempts) -> DEFAULT_WAIT_DURATION;
    // The default exception predicate retries all exceptions.
    private Predicate<Throwable> exceptionPredicate = (exception) -> true;
    private CheckedConsumer<Long> sleepFunction = Thread::sleep;

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

    public CheckedConsumer<Long> getSleepFunction() {
        return sleepFunction;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public double getRetryThreshold() {
        return retryThreshold;
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
        private RetryConfig config = new RetryConfig();

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
            }
            config.maxAttempts = maxAttempts;
            return this;
        }

        public Builder waitDuration(Duration waitDuration) {
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
         * @return the RetryConfig.Builder
         */
        public Builder intervalFunction(IntervalFunction f) {
            config.intervalFunction = f;
            return this;
        }

        /**
         * Set a function that performs a sleep/pause between retries after failure. By default
         * the {@link Thread#sleep(long)} is used.
         *
         * @param sleepFunction function that performs a sleep/pause between retries after failure
         * @return the RetryConfig.Builder
         */
        public Builder sleepFunction(CheckedConsumer<Long> sleepFunction) {
            config.sleepFunction = sleepFunction;
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
            config.exceptionPredicate = predicate;
            return this;
        }

        /**
         * Configures buffer size that is used by retry budget. The buffer is used to keep track of completed
         * executions and retries.
         *
         * @param bufferSize size of buffer for retry budget
         * @return the RetryConfig.Builder
         */
        public Builder bufferSize(int bufferSize) {
            config.bufferSize = bufferSize;
            return this;
        }

        /**
         * Configures retry threshold, which sets maximum allowed occurrences of retries in buffer used by retry
         * budget.
         *
         * @param retryThreshold retry threshold
         * @return the RetryConfig.Builder
         */
        public Builder retryThreshold(double retryThreshold) {
            if (retryThreshold <= 0 || retryThreshold > 1) {
                throw new IllegalArgumentException("Retry threshold should be 0 < x <= 1");
            }
            config.retryThreshold = retryThreshold;
            return this;
        }

        public RetryConfig build() {
            return config;
        }
    }
}
