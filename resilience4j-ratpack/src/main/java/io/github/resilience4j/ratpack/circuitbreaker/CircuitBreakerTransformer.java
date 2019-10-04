/*
 * Copyright 2017 Dan Maas
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
package io.github.resilience4j.ratpack.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratpack.internal.AbstractTransformer;
import ratpack.exec.Downstream;
import ratpack.exec.Upstream;
import ratpack.func.Function;

import java.util.concurrent.TimeUnit;

public class CircuitBreakerTransformer<T> extends AbstractTransformer<T> {
    private CircuitBreaker circuitBreaker;

    private CircuitBreakerTransformer(CircuitBreaker circuitBreaker) {
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Create a new transformer that can be applied to the {@link ratpack.exec.Promise#transform(Function)} method.
     * The Promised value will pass through the circuitbreaker, potentially causing it to open if the thresholds
     * for the circuit breaker are exceeded.
     *
     * @param circuitBreaker the circuit breaker to use
     * @param <T>            the type of object
     * @return the transformer
     */
    public static <T> CircuitBreakerTransformer<T> of(CircuitBreaker circuitBreaker) {
        return new CircuitBreakerTransformer<>(circuitBreaker);
    }

    /**
     * Set a recovery function that will execute when the circuit breaker is open.
     *
     * @param recoverer the recovery function
     * @return the transformer
     */
    public CircuitBreakerTransformer<T> recover(Function<Throwable, ? extends T> recoverer) {
        this.recoverer = recoverer;
        return this;
    }

    @Override
    public Upstream<T> apply(Upstream<? extends T> upstream) throws Exception {
        return down -> {
            long start;
            if (circuitBreaker.tryAcquirePermission()) {
                start = System.nanoTime();
                upstream.connect(new Downstream<T>() {

                    @Override
                    public void success(T value) {
                        long durationInNanos = System.nanoTime() - start;
                        circuitBreaker.onSuccess(durationInNanos, TimeUnit.NANOSECONDS);
                        down.success(value);
                    }

                    @Override
                    public void error(Throwable throwable) {
                        long durationInNanos = System.nanoTime() - start;
                        circuitBreaker.onError(durationInNanos, TimeUnit.NANOSECONDS, throwable);
                        handleRecovery(down, throwable);
                    }

                    @Override
                    public void complete() {
                        circuitBreaker.releasePermission();
                        down.complete();
                    }
                });
            } else {
                Throwable t = CallNotPermittedException.createCallNotPermittedException(circuitBreaker);
                handleRecovery(down, t);
            }
        };
    }

}
