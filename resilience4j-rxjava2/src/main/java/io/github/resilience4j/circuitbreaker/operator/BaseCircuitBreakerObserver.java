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
