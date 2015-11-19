package io.github.robwin.failsafe;

import com.codahale.metrics.Timer;
import io.github.robwin.circuitbreaker.CircuitBreaker;
import io.github.robwin.metrics.Metrics;
import io.github.robwin.retry.Retry;
import javaslang.control.Try;

import java.util.function.Function;
import java.util.function.Supplier;

public interface FailSafe{

    static <T> FailSafeSupplier<T> ofSupplier(Supplier<T> supplier){
        return new FailSafeSupplier<>(supplier);
    }

    static <T, R> FailSafeFunction<T, R> ofFunction(Function<T, R> function){
        return new FailSafeFunction<>(function);
    }

    static FailSafeRunnable ofRunnable(Runnable supplier){
        return new FailSafeRunnable(supplier);
    }

    static <T> FailSafeCheckedSupplier<T> ofCheckedSupplier(Try.CheckedSupplier<T> supplier){
        return new FailSafeCheckedSupplier<>(supplier);
    }

    static <T, R> FailSafeCheckedFunction<T, R> ofCheckedFuction(Try.CheckedFunction<T, R> function){
        return new FailSafeCheckedFunction<>(function);
    }

    static FailSafeCheckedRunnable ofCheckedRunnable(Try.CheckedRunnable supplier){
        return new FailSafeCheckedRunnable(supplier);
    }

    class FailSafeSupplier<T>{
        private Supplier<T> supplier;

        private FailSafeSupplier(Supplier<T> supplier) {
            this.supplier = supplier;
        }


        public FailSafeSupplier<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            supplier = CircuitBreaker.decorateSupplier(supplier, circuitBreaker);
            return this;
        }

        public FailSafeSupplier<T> withRetry(Retry retryContext) {
            supplier = Retry.retryableSupplier(supplier, retryContext);
            return this;
        }

        public FailSafeSupplier<T> withMetrics(Timer timer) {
            supplier = Metrics.timedSupplier(supplier, timer);
            return this;
        }

        public Supplier<T> decorate() {
            return supplier;
        }
    }

    class FailSafeFunction<T, R>{
        private Function<T, R> function;

        private FailSafeFunction(Function<T, R> function) {
            this.function = function;
        }

        public FailSafeFunction<T, R> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            function = CircuitBreaker.decorateFunction(function, circuitBreaker);
            return this;
        }

        public FailSafeFunction<T, R> withRetry(Retry retryContext) {
            function = Retry.retryableFunction(function, retryContext);
            return this;
        }

        public FailSafeFunction<T, R> withMetrics(Timer timer) {
            function = Metrics.timedFunction(function, timer);
            return this;
        }

        public Function<T, R> decorate() {
            return function;
        }
    }

    class FailSafeRunnable{
        private Runnable runnable;

        private FailSafeRunnable(Runnable runnable) {
            this.runnable = runnable;
        }

        public FailSafeRunnable withCircuitBreaker(CircuitBreaker circuitBreaker) {
            runnable = CircuitBreaker.decorateRunnable(runnable, circuitBreaker);
            return this;
        }

        public FailSafeRunnable withRetry(Retry retryContext) {
            runnable = Retry.retryableRunnable(runnable, retryContext);
            return this;
        }

        public FailSafeRunnable withMetrics(Timer timer) {
            runnable = Metrics.timedRunnable(runnable, timer);
            return this;
        }

        public Runnable decorate() {
            return runnable;
        }
    }

    class FailSafeCheckedSupplier<T>{
        private Try.CheckedSupplier<T> supplier;

        private FailSafeCheckedSupplier(Try.CheckedSupplier<T>supplier) {
            this.supplier = supplier;
        }


        public FailSafeCheckedSupplier<T> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            supplier = CircuitBreaker.decorateCheckedSupplier(supplier, circuitBreaker);
            return this;
        }

        public FailSafeCheckedSupplier<T> withRetry(Retry retryContext) {
            supplier = Retry.retryableCheckedSupplier(supplier, retryContext);
            return this;
        }

        public FailSafeCheckedSupplier<T> withMetrics(Timer timer) {
            supplier = Metrics.timedCheckedSupplier(supplier, timer);
            return this;
        }

        public Try.CheckedSupplier<T> decorate() {
            return supplier;
        }
    }

    class FailSafeCheckedFunction<T, R>{
        private Try.CheckedFunction<T, R> function;

        private FailSafeCheckedFunction(Try.CheckedFunction<T, R> function) {
            this.function = function;
        }

        public FailSafeCheckedFunction<T, R> withCircuitBreaker(CircuitBreaker circuitBreaker) {
            function = CircuitBreaker.decorateCheckedFunction(function, circuitBreaker);
            return this;
        }

        public FailSafeCheckedFunction<T, R> withRetry(Retry retryContext) {
            function = Retry.retryableCheckedFunction(function, retryContext);
            return this;
        }

        public FailSafeCheckedFunction<T, R> withMetrics(Timer timer) {
            function = Metrics.timedCheckedFunction(function, timer);
            return this;
        }

        public Try.CheckedFunction<T, R> decorate() {
            return function;
        }
    }

    class FailSafeCheckedRunnable{
        private Try.CheckedRunnable runnable;

        private FailSafeCheckedRunnable(Try.CheckedRunnable runnable) {
            this.runnable = runnable;
        }

        public FailSafeCheckedRunnable withCircuitBreaker(CircuitBreaker circuitBreaker) {
            runnable = CircuitBreaker.decorateCheckedRunnable(runnable, circuitBreaker);
            return this;
        }

        public FailSafeCheckedRunnable withRetry(Retry retryContext) {
            runnable = Retry.retryableCheckedRunnable(runnable, retryContext);
            return this;
        }

        public FailSafeCheckedRunnable withMetrics(Timer timer) {
            runnable = Metrics.timedCheckedRunnable(runnable, timer);
            return this;
        }

        public Try.CheckedRunnable decorate() {
            return runnable;
        }
    }
}
