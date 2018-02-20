package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;

public class MonoCircuitBreaker<T> extends MonoOperator<T, T> {
    private CircuitBreaker circuitBreaker;

    public MonoCircuitBreaker(Mono<? extends T> source, CircuitBreaker circuitBreaker) {
        super(source);
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        source.subscribe(new CircuitBreakerSubscriber<>(circuitBreaker, actual));
    }
}
