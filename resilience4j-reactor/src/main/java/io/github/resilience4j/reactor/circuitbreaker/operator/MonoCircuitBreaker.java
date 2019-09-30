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

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoOperator;
import reactor.core.publisher.Operators;

import static io.github.resilience4j.circuitbreaker.CallNotPermittedException.createCallNotPermittedException;

class MonoCircuitBreaker<T> extends MonoOperator<T, T> {

    private CircuitBreaker circuitBreaker;

    MonoCircuitBreaker(Mono<? extends T> source, CircuitBreaker circuitBreaker) {
        super(source);
        this.circuitBreaker = circuitBreaker;
    }

    @Override
    public void subscribe(CoreSubscriber<? super T> actual) {
        if(circuitBreaker.tryAcquirePermission()){
            source.subscribe(new CircuitBreakerSubscriber<>(circuitBreaker, actual, true));
        }else{
            Operators.error(actual, createCallNotPermittedException(circuitBreaker));
        }
    }
}
