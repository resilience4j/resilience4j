package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.ResilienceBaseObserver;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.StopWatch;

import static java.util.Objects.requireNonNull;

/**
 * A base CircuitBreaker observer.
 *
 */
abstract class BaseCircuitBreakerObserver extends ResilienceBaseObserver {

    private final CircuitBreaker circuitBreaker;

    private final StopWatch stopWatch;

    BaseCircuitBreakerObserver(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
        stopWatch = StopWatch.start();
    }

    protected void onSuccess() {
        circuitBreaker.onSuccess(stopWatch.stop().toNanos());
    }

    protected void onError(Throwable t) {
        circuitBreaker.onError(stopWatch.stop().toNanos(), t);
    }

    @Override
    public void hookOnCancel() {
        circuitBreaker.releasePermission();
    }
}
