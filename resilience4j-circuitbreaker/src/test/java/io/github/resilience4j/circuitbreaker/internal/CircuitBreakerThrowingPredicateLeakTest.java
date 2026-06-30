/*
 *
 *  Copyright 2026 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Reproduction for issue #2324 (Java core variant): a user-supplied record/ignore exception
 * predicate that throws inside {@code handleThrowable} leaks the acquired permission, because
 * neither {@code releasePermission()} nor {@code stateReference.onError()} is reached.
 */
class CircuitBreakerThrowingPredicateLeakTest {

    @Test
    void shouldNotLeakPermitWhenRecordExceptionPredicateThrowsInHalfOpen() {
        RuntimeException predicateFailure = new RuntimeException("predicate boom");
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("test",
            CircuitBreakerConfig.custom()
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordException(throwable -> {
                    throw predicateFailure;
                })
                .build());

        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        // Acquire the single half-open trial permit.
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();

        // The user-supplied predicate throws while the breaker handles the error.
        assertThatThrownBy(
            () -> circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new IllegalStateException()))
            .isSameAs(predicateFailure);

        // The acquired permit must not be lost: a correct implementation releases it so the
        // half-open breaker can keep permitting trial calls instead of being wedged forever.
        assertThat(circuitBreaker.tryAcquirePermission())
            .as("permit should be released after the predicate throws, but it leaks")
            .isTrue();
    }

    @Test
    void shouldNotLeakPermitWhenRecordResultPredicateThrowsInHalfOpen() {
        RuntimeException predicateFailure = new RuntimeException("predicate boom");
        CircuitBreaker circuitBreaker = new CircuitBreakerStateMachine("test",
            CircuitBreakerConfig.custom()
                .permittedNumberOfCallsInHalfOpenState(1)
                .recordResult(result -> {
                    throw predicateFailure;
                })
                .build());

        circuitBreaker.transitionToOpenState();
        circuitBreaker.transitionToHalfOpenState();

        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();

        assertThatThrownBy(() -> circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, "someResult"))
            .isSameAs(predicateFailure);

        assertThat(circuitBreaker.tryAcquirePermission())
            .as("permit should be released after the result predicate throws, but it leaks")
            .isTrue();
    }
}
