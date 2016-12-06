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

import javaslang.collection.Stream;
import javaslang.control.Try;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

public class RetryContext implements Retry {

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_WAIT_DURATION = 500;

    private final AtomicInteger numOfAttempts = new AtomicInteger(0);
    private AtomicReference<Exception> lastException = new AtomicReference<>();
    private AtomicReference<RuntimeException> lastRuntimeException = new AtomicReference<>();

    /*package*/ static Try.CheckedConsumer<Long> sleepFunction = Thread::sleep;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private Duration waitDuration = Duration.ofMillis(DEFAULT_WAIT_DURATION);
    private Function<Duration, Duration> backoffFunction = Function.identity();
    // The default exception predicate retries all exceptions.
    private Predicate<Throwable> exceptionPredicate = (exception) -> true;

    private RetryContext(){
    }

    @Override
    public void throwOrSleepAfterException() throws Exception {
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts == maxAttempts){
            throw lastException.get();
        }else{
            waitIntervalAfterFailure();
        }
    }
    @Override
    public void throwOrSleepAfterRuntimeException(){
        int currentNumOfAttempts = numOfAttempts.incrementAndGet();
        if(currentNumOfAttempts == maxAttempts){
            throw lastRuntimeException.get();
        }else{
            waitIntervalAfterFailure();
        }
    }

    private void waitIntervalAfterFailure() {
        // wait interval until the next attempt should start
        long interval = Stream.iterate(waitDuration, backoffFunction)
            .get(numOfAttempts.get()-1)
            .toMillis();
        Try.run(() -> sleepFunction.accept(interval))
            .getOrElseThrow(ex -> lastRuntimeException.get());
    }

    @Override
    public void handleException(Exception exception) throws Throwable{
        if(exceptionPredicate.test(exception)){
            lastException.set(exception);
        }else{
            throw exception;
        }
    }

    @Override
    public void handleRuntimeException(RuntimeException runtimeException){
        if(exceptionPredicate.test(runtimeException)){
            lastRuntimeException.set(runtimeException);
        }else{
            throw runtimeException;
        }
    }

    private static class Context {
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private Duration waitDuration = Duration.ofMillis(DEFAULT_WAIT_DURATION);
        private Function<Duration, Duration> backoffFunction = Function.identity();
        // The default exception predicate retries all exceptions.
        private Predicate<Throwable> exceptionPredicate = (exception) -> true;

    }

    public static class Builder {
        private RetryContext retryContext = new RetryContext();

        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < 1) {
                throw new IllegalArgumentException("maxAttempts must be greater than or equal to 1");
            }
            retryContext.maxAttempts = maxAttempts;
            return this;
        }

        public Builder waitDuration(Duration waitDuration) {
            if (waitDuration.toMillis() < 10) {
                throw new IllegalArgumentException("waitDurationInOpenState must be at least 10ms");
            }
            retryContext.waitDuration = waitDuration;
            return this;
        }

        /**
         * Set a function to modify the waiting interval
         * after a failure. By default the interval stays
         * the same.
         *
         * @param f Function to modify the interval after a failure
         */
        public Builder backoffFunction(Function<Duration, Duration> f) {
            retryContext.backoffFunction = f;
            return this;
        }

        /**
         *  Configures a Predicate which evaluates if an exception should be retried.
         *  The Predicate must return true if the exception should count be retried, otherwise it must return false.
         *
         * @param predicate the Predicate which evaluates if an exception should be retried or not.
         * @return the CircuitBreakerConfig.Builder
         */
        public Builder retryOnException(Predicate<Throwable> predicate) {
            retryContext.exceptionPredicate = predicate;
            return this;
        }

        public Retry build() {
            return retryContext;
        }
    }
}
