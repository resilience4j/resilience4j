/*
 *
 *  Copyright 2020: KrnSaurabh
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
package io.github.resilience4j.decorators;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.VavrBulkhead;
import io.github.resilience4j.cache.Cache;
import io.github.resilience4j.cache.VavrCache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.VavrCircuitBreaker;
import io.github.resilience4j.core.VavrCheckedFunctionUtils;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.VavrRateLimiter;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.VavrRetry;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedFunction1;
import io.vavr.CheckedFunction2;
import io.vavr.CheckedRunnable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface VavrDecorators {
    static <T> DecorateCheckedSupplier<T> ofCheckedSupplier(CheckedFunction0<T> supplier) {
        return new DecorateCheckedSupplier<>(supplier);
    }

    static <T, R> DecorateCheckedFunction<T, R> ofCheckedFunction(CheckedFunction1<T, R> function) {
        return new DecorateCheckedFunction<>(function);
    }

    static DecorateCheckedRunnable ofCheckedRunnable(CheckedRunnable supplier) {
        return new DecorateCheckedRunnable(supplier);
    }

    class DecorateCheckedSupplier<T> {

        private CheckedFunction0<T> supplier;

        private DecorateCheckedSupplier(CheckedFunction0<T> supplier) {
            this.supplier = supplier;
        }


        public DecorateCheckedSupplier<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            supplier = VavrCircuitBreaker.decorateCheckedSupplier(circuitBreaker, supplier);
            return this;
        }

        public DecorateCheckedSupplier<T> withRetry(Retry retryContext) {
            supplier = VavrRetry.decorateCheckedSupplier(retryContext, supplier);
            return this;
        }

        public DecorateCheckedSupplier<T> withRateLimiter(RateLimiter rateLimiter) {
            return withRateLimiter(rateLimiter, 1);
        }

        public DecorateCheckedSupplier<T> withRateLimiter(RateLimiter rateLimiter, int permits) {
            supplier = VavrRateLimiter.decorateCheckedSupplier(rateLimiter, permits, supplier);
            return this;
        }

        public <K> DecorateCheckedFunction<K, T> withCache(Cache<K, T> cache) {
            return VavrDecorators.ofCheckedFunction(VavrCache.decorateCheckedSupplier(cache, supplier));
        }

        public DecorateCheckedSupplier<T> withBulkhead(Bulkhead bulkhead) {
            supplier = VavrBulkhead.decorateCheckedSupplier(bulkhead, supplier);
            return this;
        }

        public DecorateCheckedSupplier<T> withFallback(CheckedFunction2<T, Throwable, T> handler) {
            supplier = VavrCheckedFunctionUtils.andThen(supplier, handler);
            return this;
        }

        public DecorateCheckedSupplier<T> withFallback(Predicate<T> resultPredicate, CheckedFunction1<T, T> resultHandler) {
            supplier = VavrCheckedFunctionUtils.recover(supplier, resultPredicate, resultHandler);
            return this;
        }

        public DecorateCheckedSupplier<T> withFallback(List<Class<? extends Throwable>> exceptionTypes, CheckedFunction1<Throwable, T> exceptionHandler) {
            supplier = VavrCheckedFunctionUtils.recover(supplier, exceptionTypes, exceptionHandler);
            return this;
        }

        public DecorateCheckedSupplier<T> withFallback(CheckedFunction1<Throwable, T> exceptionHandler) {
            supplier = VavrCheckedFunctionUtils.recover(supplier, exceptionHandler);
            return this;
        }

        public <X extends Throwable> DecorateCheckedSupplier<T> withFallback(Class<X> exceptionType, CheckedFunction1<Throwable, T> exceptionHandler) {
            supplier = VavrCheckedFunctionUtils.recover(supplier, exceptionType, exceptionHandler);
            return this;
        }

        public CheckedFunction0<T> decorate() {
            return supplier;
        }

        public T get() throws Throwable {
            return supplier.apply();
        }
    }

    class DecorateCheckedFunction<T, R> {

        private CheckedFunction1<T, R> function;

        private DecorateCheckedFunction(CheckedFunction1<T, R> function) {
            this.function = function;
        }

        public DecorateCheckedFunction<T, R> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            function = VavrCircuitBreaker.decorateCheckedFunction(circuitBreaker, function);
            return this;
        }

        public DecorateCheckedFunction<T, R> withRetry(Retry retryContext) {
            function = VavrRetry.decorateCheckedFunction(retryContext, function);
            return this;
        }

        public DecorateCheckedFunction<T, R> withRateLimiter(RateLimiter rateLimiter) {
            return withRateLimiter(rateLimiter, 1);
        }

        public DecorateCheckedFunction<T, R> withRateLimiter(RateLimiter rateLimiter, int permits) {
            function = VavrRateLimiter.decorateCheckedFunction(rateLimiter, permits, function);
            return this;
        }

        public DecorateCheckedFunction<T, R> withRateLimiter(RateLimiter rateLimiter,
                                                             Function<T, Integer> permitsCalculator) {
            function = VavrRateLimiter
                .decorateCheckedFunction(rateLimiter, permitsCalculator, function);
            return this;
        }

        public DecorateCheckedFunction<T, R> withBulkhead(Bulkhead bulkhead) {
            function = VavrBulkhead.decorateCheckedFunction(bulkhead, function);
            return this;
        }

        public CheckedFunction1<T, R> decorate() {
            return function;
        }

        public R apply(T t) throws Throwable {
            return function.apply(t);
        }
    }

    class DecorateCheckedRunnable {

        private CheckedRunnable runnable;

        private DecorateCheckedRunnable(CheckedRunnable runnable) {
            this.runnable = runnable;
        }

        public DecorateCheckedRunnable withCircuitBreaker(CircuitBreaker circuitBreaker) {
            runnable = VavrCircuitBreaker.decorateCheckedRunnable(circuitBreaker, runnable);
            return this;
        }

        public DecorateCheckedRunnable withRetry(Retry retryContext) {
            runnable = VavrRetry.decorateCheckedRunnable(retryContext, runnable);
            return this;
        }

        public DecorateCheckedRunnable withRateLimiter(RateLimiter rateLimiter) {
            runnable = VavrRateLimiter.decorateCheckedRunnable(rateLimiter, runnable);
            return this;
        }

        public DecorateCheckedRunnable withRateLimiter(RateLimiter rateLimiter, int permits) {
            runnable = VavrRateLimiter.decorateCheckedRunnable(rateLimiter, permits, runnable);
            return this;
        }

        public DecorateCheckedRunnable withBulkhead(Bulkhead bulkhead) {
            runnable = VavrBulkhead.decorateCheckedRunnable(bulkhead, runnable);
            return this;
        }

        public CheckedRunnable decorate() {
            return runnable;
        }

        public void run() throws Throwable {
            runnable.run();
        }
    }

}
