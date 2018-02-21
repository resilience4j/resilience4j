package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;

public class FluxCircuitBreaker<T> extends FluxOperator<T, T> {

    private CircuitBreaker circuitBreaker;

    public FluxCircuitBreaker(Flux<? extends T> source, CircuitBreaker circuitBreaker) {
        super(source);
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        source.subscribe(new CircuitBreakerSubscriber<>(circuitBreaker, actual));
    }

}
