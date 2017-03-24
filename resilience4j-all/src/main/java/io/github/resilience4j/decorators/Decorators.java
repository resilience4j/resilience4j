package io.github.resilience4j.decorators;

import io.github.resilience4j.cache.Cache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.Retry;
import javaslang.control.Try;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A Decorator builder which can be used to apply multiple decorators to a (Checked-)Supplier, (Checked-)Function,
 * (Checked-)Runnable, (Checked-)CompletionStage or (Checked-)Consumer
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

    static <T> DecorateCheckedSupplier<T> ofCheckedSupplier(Try.CheckedSupplier<T> supplier){
        return new DecorateCheckedSupplier<>(supplier);
    }

    static <T, R> DecorateCheckedFunction<T, R> ofCheckedFunction(Try.CheckedFunction<T, R> function){
        return new DecorateCheckedFunction<>(function);
    }

    static DecorateCheckedRunnable ofCheckedRunnable(Try.CheckedRunnable supplier){
        return new DecorateCheckedRunnable(supplier);
    }

    static <T> DecorateCompletionStage<T> ofCompletionStage(Supplier<CompletionStage<T>> stageSupplier){
        return new DecorateCompletionStage<>(stageSupplier);
    }

    public static <T> DecorateConsumer<T> ofConsumer(Consumer<T> consumer) {
        return new DecorateConsumer<>(consumer);
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

        public T get() {
            return supplier.get();
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

        public R apply(T t) {
            return function.apply(t);
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

        public void run() {
            runnable.run();
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

        public T get() throws Throwable {
            return supplier.get();
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

        public R apply(T t) throws Throwable {
            return function.apply(t);
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

        public void run() throws Throwable {
            runnable.run();
        }
    }

    class DecorateCompletionStage<T> {

        private Supplier<CompletionStage<T>> stageSupplier;

        public DecorateCompletionStage(Supplier<CompletionStage<T>> stageSupplier) {
            this.stageSupplier = stageSupplier;
        }

        public DecorateCompletionStage<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            stageSupplier = CircuitBreaker.decorateCompletionStage(circuitBreaker, stageSupplier);
            return this;
        }

        public DecorateCompletionStage<T> withRetry(AsyncRetry retryContext, ScheduledExecutorService scheduler) {
            stageSupplier = AsyncRetry.decorateCompletionStage(retryContext, scheduler, stageSupplier);
            return this;
        }

        public Supplier<CompletionStage<T>> decorate() {
            return stageSupplier;
        }

        public CompletionStage<T> get() {
            return stageSupplier.get();
        }
    }

    class DecorateConsumer<T> {

        private Consumer<T> consumer;

        private DecorateConsumer(Consumer<T> consumer) {
            this.consumer = consumer;
        }

        public DecorateConsumer<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            consumer = CircuitBreaker.decorateConsumer(circuitBreaker, consumer);
            return this;
        }

        public DecorateConsumer<T> withRateLimiter(RateLimiter rateLimiter) {
            consumer = RateLimiter.decorateConsumer(rateLimiter, consumer);
            return this;
        }

        public Consumer<T> decorate() {
            return consumer;
        }

        public void accept(T obj) {
            consumer.accept(obj);
        }
    }
}
