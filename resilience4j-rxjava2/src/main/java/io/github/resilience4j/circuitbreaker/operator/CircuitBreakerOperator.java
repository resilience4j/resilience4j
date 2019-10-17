/*
 * Copyright 2019 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.operator;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.*;
import org.reactivestreams.Publisher;

import static java.util.Objects.requireNonNull;

/**
 * A CircuitBreaker operator which checks if a downstream subscriber/observer can acquire a
 * permission to subscribe to an upstream Publisher. Otherwise emits a {@link
 * CallNotPermittedException} if the CircuitBreaker is OPEN.
 *
 * @param <T> the value type
 */
public class CircuitBreakerOperator<T> implements FlowableTransformer<T, T>,
    SingleTransformer<T, T>, MaybeTransformer<T, T>, CompletableTransformer,
    ObservableTransformer<T, T> {

    private final CircuitBreaker circuitBreaker;

    private CircuitBreakerOperator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    /**
     * Creates a CircuitBreakerOperator.
     *
     * @param circuitBreaker the CircuitBreaker
     * @return a CircuitBreakerOperator
     */
    public static <T> CircuitBreakerOperator<T> of(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerOperator<>(circuitBreaker);
    }

    @Override
    public Publisher<T> apply(Flowable<T> upstream) {
        return new FlowableCircuitBreaker<>(upstream, circuitBreaker);
    }


    @Override
    public SingleSource<T> apply(Single<T> upstream) {
        return new SingleCircuitBreaker<>(upstream, circuitBreaker);
    }

    @Override
    public CompletableSource apply(Completable upstream) {
        return new CompletableCircuitBreaker(upstream, circuitBreaker);
    }

    @Override
    public MaybeSource<T> apply(Maybe<T> upstream) {
        return new MaybeCircuitBreaker<>(upstream, circuitBreaker);
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return new ObserverCircuitBreaker<>(upstream, circuitBreaker);
    }
}
