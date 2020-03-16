/*
 * Copyright 2018 Julien Hoarau, Robert Winkler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.reactor.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.IllegalPublisherException;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * A CircuitBreaker operator which checks if a downstream subscriber/observer can acquire a
 * permission to subscribe to an upstream Publisher. Otherwise emits a {@link
 * CallNotPermittedException} if the CircuitBreaker is OPEN.
 *
 * @param <T> the value type
 */
public class CircuitBreakerOperator<T> implements UnaryOperator<Publisher<T>> {

    private final CircuitBreaker circuitBreaker;
    private final Predicate<T> responseValidator;
    private final Throwable throwable;


    private CircuitBreakerOperator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
        this.responseValidator = T -> true;
        this.throwable = new RuntimeException();
    }

    private CircuitBreakerOperator(CircuitBreaker circuitBreaker, Predicate<T> responseValidator, Throwable throwable) {
        this.circuitBreaker = circuitBreaker;
        this.responseValidator = responseValidator;
        this.throwable = throwable;
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

    /**
     * Creates a CircuitBreakerOperator with response predicate
     *
     * @param circuitBreaker the circuit breaker
     * @param responseValidator response will be considered failure if it fails this predicate
     * @param throwable throwable to be recorded in circuit breaker
     * @param <T> the value type of the upstream and downstream
     * @return a CircuitBreakerOperator
     */
    public static <T> CircuitBreakerOperator<T> of(CircuitBreaker circuitBreaker,  Predicate<T> responseValidator, Throwable throwable) {
        return new CircuitBreakerOperator<>(circuitBreaker, responseValidator, throwable);
    }

    @Override
    public Publisher<T> apply(Publisher<T> publisher) {
        if (publisher instanceof Mono) {
            return new MonoCircuitBreaker<>((Mono<? extends T>) publisher, circuitBreaker, responseValidator, throwable);
        } else if (publisher instanceof Flux) {
            return new FluxCircuitBreaker<>((Flux<? extends T>) publisher, circuitBreaker, responseValidator, throwable);
        } else {
            throw new IllegalPublisherException(publisher);
        }
    }
}
