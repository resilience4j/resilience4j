/*
 *
 *  Copyright 2017: Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.circuitbreaker.operator;

import static java.util.Objects.requireNonNull;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.reactivex.CompletableObserver;
import io.reactivex.CompletableOperator;
import io.reactivex.FlowableOperator;
import io.reactivex.MaybeObserver;
import io.reactivex.MaybeOperator;
import io.reactivex.ObservableOperator;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOperator;
import org.reactivestreams.Subscriber;

/**
 * A RxJava operator which protects a reactive type by a CircuitBreaker.
 *
 * @param <T> the value type of the upstream and downstream
 */
public class CircuitBreakerOperator<T> implements ObservableOperator<T, T>, FlowableOperator<T, T>, SingleOperator<T, T>, CompletableOperator, MaybeOperator<T, T> {
    private final CircuitBreaker circuitBreaker;

    private CircuitBreakerOperator(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = requireNonNull(circuitBreaker);
    }

    /**
     * Creates a CircuitBreakerOperator.
     *
     * @param circuitBreaker the CircuitBreaker
     * @param <T>            the value type of the upstream and downstream
     * @return a CircuitBreakerOperator
     */
    public static <T> CircuitBreakerOperator<T> of(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerOperator<>(circuitBreaker);
    }

    @Override
    public Subscriber<? super T> apply(Subscriber<? super T> childSubscriber) throws Exception {
        return new CircuitBreakerSubscriber<>(circuitBreaker, childSubscriber);
    }

    @Override
    public Observer<? super T> apply(Observer<? super T> childObserver) throws Exception {
        return new CircuitBreakerObserver<>(circuitBreaker, childObserver);
    }

    @Override
    public SingleObserver<? super T> apply(SingleObserver<? super T> childObserver) throws Exception {
        return new CircuitBreakerSingleObserver<>(circuitBreaker, childObserver);
    }

    @Override
    public CompletableObserver apply(CompletableObserver observer) throws Exception {
        return new CircuitBreakerCompletableObserver(circuitBreaker, observer);
    }

    @Override
    public MaybeObserver<? super T> apply(MaybeObserver<? super T> observer) throws Exception {
        return new CircuitBreakerMaybeObserver<>(circuitBreaker, observer);
    }
}
