package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerOpenException;
import io.github.resilience4j.core.StopWatch;
import io.github.resilience4j.internal.PermittedOperator;

import static java.util.Objects.requireNonNull;

/**
 * A disposable circuit-breaker acting as a base class for circuit-breaker operators.
 *
 * @param <T> the type of the emitted event
 * @param <D> the actual type of the disposable/subscription
 */
abstract class AbstractCircuitBreakerOperator<T, D> extends PermittedOperator<T, D> {
    private final CircuitBreaker circuitBreaker;
    private StopWatch stopWatch;

    AbstractCircuitBreakerOperator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    @Override
    protected boolean tryCallPermit() {
        stopWatch = StopWatch.start(circuitBreaker.getName());
        return circuitBreaker.isCallPermitted();
    }

    @Override
    protected Exception notPermittedException() {
        return new CircuitBreakerOpenException(String.format("CircuitBreaker '%s' is open", circuitBreaker.getName()));
    }

    @Override
    protected void doOnSuccess() {
        circuitBreaker.onSuccess(stopWatch.stop().getProcessingDuration().toNanos());
    }

    @Override
    protected void doOnError(Throwable e) {
        circuitBreaker.onError(stopWatch.stop().getProcessingDuration().toNanos(), e);
    }
}
