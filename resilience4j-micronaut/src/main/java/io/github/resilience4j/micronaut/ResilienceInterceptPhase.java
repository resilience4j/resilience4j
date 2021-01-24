/*
 * Copyright 2020 Michael Pollind
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
package io.github.resilience4j.micronaut;

import io.github.resilience4j.micronaut.retry.RetryInterceptor;
import io.micronaut.aop.Interceptor;

/**
 * <p>{@link Interceptor} classes implement the {@link io.micronaut.core.order.Ordered} interface
 * in order to control the order of execution when multiple interceptors are present.</p>
 *
 * <p> This class provides a set of phases used for resilience4j</p>
 * <p>
 * The default order of phases are: <code>Retry ( CircuitBreaker ( RateLimiter ( TimeLimiter ( Bulkhead ( Function ) ) ) ) )</code>
 * The order places this at {@link RetryInterceptor} and before {@link io.micronaut.retry.intercept.RecoveryInterceptor}
 */
public enum ResilienceInterceptPhase {

    /**
     * Retry phase of execution.
     */
    RETRY(-60),

    /**
     * Retry phase of execution.
     */
    CIRCUIT_BREAKER(-55),

    /**
     * Retry phase of execution.
     */
    RATE_LIMITER(-50),

    /**
     * Retry phase of execution.
     */
    TIME_LIMITER(-45),

    /**
     * Retry phase of execution.
     */
    BULKHEAD(-42);

    private final int position;

    /**
     * Constructor.
     *
     * @param position The order of position
     */
    ResilienceInterceptPhase(int position) {
        this.position = position;
    }

    /**
     * @return The position
     */
    public int getPosition() {
        return position;
    }
}
