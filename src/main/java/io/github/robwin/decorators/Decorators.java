package io.github.robwin.decorators;

import io.github.robwin.cache.Cache;
import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.ratelimiter.RateLimiter;
import io.github.robwin.retry.Retry;
import javaslang.control.Try;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Decorator builder which can be used to apply multiple decorators to a (Checked-)Supplier, (Checked-)Function or (Checked-)Runnable.
 */
public interface Decorators{

    static <T> DecorateSupplier<T> ofSupplier(Supplier<T> supplier){
        return new DecorateSupplier<>(supplier);
    }

    static <T, R> DecorateFunction<T, R> ofFunction(Function<T, R> function){
        return new DecorateFunction<>(function);
    }

    static DecorateRunnable ofRunnable(Runnable runnable){
        return new DecorateRunnable(runnable);
    }

    static <T> Decorators.DecorateCheckedSupplier<T> ofCheckedSupplier(Try.CheckedSupplier<T> supplier){
        return new Decorators.DecorateCheckedSupplier<>(supplier);
    }

    static <T, R> Decorators.DecorateCheckedFunction<T, R> ofCheckedFunction(Try.CheckedFunction<T, R> function){
        return new Decorators.DecorateCheckedFunction<>(function);
    }

    static Decorators.DecorateCheckedRunnable ofCheckedRunnable(Try.CheckedRunnable supplier){
        return new Decorators.DecorateCheckedRunnable(supplier);
    }

    class DecorateSupplier<T>{
        private Supplier<T> supplier;

        private DecorateSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }


        public DecorateSupplier<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            supplier = CircuitBreaker.decorateSupplier(circuitBreaker, supplier);
            return this;
        }

        public DecorateSupplier<T> withRetry(Retry retryContext) {
            supplier = Retry.decorateSupplier(retryContext, supplier);
            return this;
        }

        public <K> DecorateFunction<K, T> withCache(Cache<K, T> cache) {
            return Decorators.ofFunction(Cache.decorateSupplier(cache, supplier));
        }

        public DecorateSupplier<T> withRateLimiter(RateLimiter rateLimiter) {
            supplier = RateLimiter.decorateSupplier(rateLimiter, supplier);
            return this;
        }

        public Supplier<T> decorate() {
            return supplier;
        }
    }

    class DecorateFunction<T, R>{
        private Function<T, R> function;

        private DecorateFunction(Function<T, R> function) {
            this.function = function;
        }

        public DecorateFunction<T, R> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            function = CircuitBreaker.decorateFunction(circuitBreaker, function);
            return this;
        }

        public DecorateFunction<T, R> withRetry(Retry retryContext) {
            function = Retry.decorateFunction(retryContext, function);
            return this;
        }

        public DecorateFunction<T, R> withRateLimiter(RateLimiter rateLimiter) {
            function = RateLimiter.decorateFunction(rateLimiter, function);
            return this;
        }

        public Function<T, R> decorate() {
            return function;
        }
    }

    class DecorateRunnable{
        private Runnable runnable;

        private DecorateRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        public DecorateRunnable withCircuitBreaker(CircuitBreaker circuitBreaker) {
            runnable = CircuitBreaker.decorateRunnable(circuitBreaker, runnable);
            return this;
        }

        public DecorateRunnable withRetry(Retry retryContext) {
            runnable = Retry.decorateRunnable(retryContext, runnable);
            return this;
        }

        public DecorateRunnable withRateLimiter(RateLimiter rateLimiter) {
            runnable = RateLimiter.decorateRunnable(rateLimiter, runnable);
            return this;
        }

        public Runnable decorate() {
            return runnable;
        }
    }

    class DecorateCheckedSupplier<T>{
        private Try.CheckedSupplier<T> supplier;

        private DecorateCheckedSupplier(Try.CheckedSupplier<T>supplier) {
            this.supplier = supplier;
        }


        public DecorateCheckedSupplier<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            supplier = CircuitBreaker.decorateCheckedSupplier(circuitBreaker, supplier);
            return this;
        }

        public DecorateCheckedSupplier<T> withRetry(Retry retryContext) {
            supplier = Retry.decorateCheckedSupplier(retryContext, supplier);
            return this;
        }

        public DecorateCheckedSupplier<T> withRateLimiter(RateLimiter rateLimiter) {
            supplier = RateLimiter.decorateCheckedSupplier(rateLimiter, supplier);
            return this;
        }

        public <K> DecorateCheckedFunction<K, T> withCache(Cache<K, T> cache) {
            return Decorators.ofCheckedFunction(Cache.decorateCheckedSupplier(cache, supplier));
        }

        public Try.CheckedSupplier<T> decorate() {
            return supplier;
        }
    }

    class DecorateCheckedFunction<T, R>{
        private Try.CheckedFunction<T, R> function;

        private DecorateCheckedFunction(Try.CheckedFunction<T, R> function) {
            this.function = function;
        }

        public DecorateCheckedFunction<T, R> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            function = CircuitBreaker.decorateCheckedFunction(circuitBreaker, function);
            return this;
        }

        public DecorateCheckedFunction<T, R> withRetry(Retry retryContext) {
            function = Retry.decorateCheckedFunction(retryContext, function);
            return this;
        }

        public DecorateCheckedFunction<T, R> withRateLimiter(RateLimiter rateLimiter) {
            function = RateLimiter.decorateCheckedFunction(rateLimiter, function);
            return this;
        }

        public Try.CheckedFunction<T, R> decorate() {
            return function;
        }
    }

    class DecorateCheckedRunnable {
        private Try.CheckedRunnable runnable;

        private DecorateCheckedRunnable(Try.CheckedRunnable runnable) {
            this.runnable = runnable;
        }

        public DecorateCheckedRunnable withCircuitBreaker(CircuitBreaker circuitBreaker) {
            runnable = CircuitBreaker.decorateCheckedRunnable(circuitBreaker, runnable);
            return this;
        }

        public DecorateCheckedRunnable withRetry(Retry retryContext) {
            runnable = Retry.decorateCheckedRunnable(retryContext, runnable);
            return this;
        }

        public DecorateCheckedRunnable withRateLimiter(RateLimiter rateLimiter) {
            runnable = RateLimiter.decorateCheckedRunnable(rateLimiter, runnable);
            return this;
        }

        public Try.CheckedRunnable decorate() {
            return runnable;
        }
    }
}