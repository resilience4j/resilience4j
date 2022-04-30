package io.github.resilience4j.circuitbreaker.utils;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.TransitionCheckResult;
import io.github.resilience4j.core.functions.Either;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

import static io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.TransitionCheckResult.*;

@SuppressWarnings("unchecked")
public final class CircuitBreakerResultUtils {

    private CircuitBreakerResultUtils() {
    }

    public static boolean isFailedWith(Either<Object, Throwable> result, Class<? extends Throwable> exceptionClass) {
        return result.isRight() && exceptionClass.isAssignableFrom(result.get().getClass());
    }

    public static <T extends Throwable> TransitionCheckThrowableBuilder<T> ifFailedWith(Class<T> exceptionClass) {
        return new TransitionCheckThrowableBuilder<>(exceptionClass);
    }

    public static class TransitionCheckThrowableBuilder<T extends Throwable> {
        private final Class<T> exceptionClass;

        private TransitionCheckThrowableBuilder(Class<T> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        public Function<Either<Object, Throwable>, TransitionCheckResult> thenOpen() {
            return result -> isFailedWith(result, exceptionClass) ? transitionToOpen() : noTransition();
        }

        public Function<Either<Object, Throwable>, TransitionCheckResult>  thenOpenFor(
            Function<T, Duration> waitDurationExtractor) {
            return result -> isFailedWith(result, exceptionClass)
                ? transitionToOpenAndWaitFor(waitDurationExtractor.apply((T) result.get()))
                : noTransition();
        }

        public Function<Either<Object, Throwable>, TransitionCheckResult>  thenOpenUntil(
            Function<T, Instant> waitUntilExtractor) {
            return result -> isFailedWith(result, exceptionClass)
                ? transitionToOpenAndWaitUntil(waitUntilExtractor.apply((T) result.get()))
                : noTransition();
        }
    }
}
