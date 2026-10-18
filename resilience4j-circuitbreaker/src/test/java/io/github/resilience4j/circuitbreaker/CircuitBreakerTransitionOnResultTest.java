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
package io.github.resilience4j.circuitbreaker;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.TransitionCheckResult;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Applies result-driven transitions for failed calls and requests without an explicit wait.
 */
class CircuitBreakerTransitionOnResultTest {

    private static final String NAME = "transitionOnResult";

    /**
     * {@link TransitionCheckResult#transitionToOpen()} must open the breaker for the configured
     * {@code waitIntervalFunctionInOpenState} instead of throwing.
     */
    @Test
    void transitionToOpenWithoutWaitDurationShouldOpenTheCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .transitionOnResult(result -> TransitionCheckResult.transitionToOpen())
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of(NAME, config);

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("BAM!"));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();
        assertThat(circuitBreaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(1L);
    }

    /**
     * A result recorded as a failure is a call outcome like any other, so the hook must be consulted
     * for it too.
     */
    @Test
    void transitionOnResultShouldBeConsultedForResultsRecordedAsFailures() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .recordResult("POISON"::equals)
            .transitionOnResult(result -> TransitionCheckResult
                .transitionToOpenAndWaitFor(Duration.ofSeconds(60)))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of(NAME, config);

        circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, "POISON");

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getMetrics().getNumberOfBufferedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();
    }

    /**
     * {@link TransitionCheckResult#noTransition()} leaves the breaker closed and
     * still accepting calls.
     */
    @Test
    void noTransitionShouldKeepTheCircuitBreakerClosed() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .recordResult("POISON"::equals)
            .transitionOnResult(result -> TransitionCheckResult.noTransition())
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of(NAME, config);

        circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, "POISON");

        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.tryAcquirePermission()).isTrue();
    }

    /**
     * An explicit wait duration can request an open state.
     */
    @Test
    void explicitWaitDurationShouldOpenTheCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .transitionOnResult(result -> TransitionCheckResult
                .transitionToOpenAndWaitFor(Duration.ofHours(1)))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of(NAME, config);

        circuitBreaker.onError(0, TimeUnit.NANOSECONDS, new RuntimeException("BAM!"));

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();
    }

    /**
     * An explicit wait-until instant can open the breaker for successful results.
     */
    @Test
    void explicitWaitUntilShouldOpenTheCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .transitionOnResult(result -> TransitionCheckResult
                .transitionToOpenAndWaitUntil(Instant.now().plus(Duration.ofHours(1))))
            .build();
        CircuitBreaker circuitBreaker = CircuitBreaker.of(NAME, config);

        circuitBreaker.onResult(0, TimeUnit.NANOSECONDS, "OK");

        assertThat(circuitBreaker.getMetrics().getNumberOfSuccessfulCalls()).isEqualTo(1);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(circuitBreaker.tryAcquirePermission()).isFalse();
    }
}
