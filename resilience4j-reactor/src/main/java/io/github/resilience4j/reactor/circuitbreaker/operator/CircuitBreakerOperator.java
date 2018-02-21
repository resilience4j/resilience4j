package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.FluxResilience;
import io.github.resilience4j.reactor.MonoResilience;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * A Reactor operator which wraps a reactive type in a circuit breaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class CircuitBreakerOperator<T> implements Function<Publisher<T>, Publisher<T>> {

    private final CircuitBreaker circuitBreaker;

    private CircuitBreakerOperator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Creates a CircuitBreakerOperator.
     *
     * @param <T>            the value type of the upstream and downstream
     * @param circuitBreaker the circuit breaker
     * @return a CircuitBreakerOperator
     */
    public static <T> CircuitBreakerOperator<T> of(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerOperator<>(circuitBreaker);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return MonoResilience
                    .onAssembly(new MonoCircuitBreaker<T>((Mono<? extends T>) publisher, circuitBreaker));
        } else if (publisher instanceof Flux) {
            return FluxResilience
                    .onAssembly(new FluxCircuitBreaker<T>((Flux<? extends T>) publisher, circuitBreaker));
        }

        throw new IllegalStateException("Publisher of type <" + publisher.getClass().getSimpleName()
                + "> are not supported by this operator");
    }
}
