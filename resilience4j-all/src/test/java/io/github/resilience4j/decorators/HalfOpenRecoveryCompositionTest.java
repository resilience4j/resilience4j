/*
 *
 *  Copyright 2026 authors
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
package io.github.resilience4j.decorators;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.HALF_OPEN;
import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
import static org.assertj.core.api.Assertions.assertThat;

class HalfOpenRecoveryCompositionTest {

    @Test
    void gradualRecoveryFitsAnIndependentlyRecoveringProviderUnderTheSameOfferedLoad() {
        RecoveryRun standard = new RecoveryRun(false);
        RecoveryRun gradual = new RecoveryRun(true);

        standard.offerTraffic(4_200, Duration.ofMillis(10));
        gradual.offerTraffic(4_200, Duration.ofMillis(10));

        assertThat(standard.offered).isEqualTo(4_200);
        assertThat(gradual.offered).isEqualTo(standard.offered);
        assertThat(standard.closedAtMillis.getFirst()).isLessThan(1_000L);
        assertThat(standard.provider.overloaded).isPositive();
        assertThat(standard.reopened).isPositive();
        assertThat(standard.breaker.getState()).isEqualTo(CLOSED);

        assertThat(gradual.provider.overloaded).isZero();
        assertThat(gradual.reopened).isZero();
        assertThat(gradual.closedAtMillis).hasSize(1);
        assertThat(gradual.closedAtMillis.getFirst())
            .isGreaterThanOrEqualTo(40_000L).isLessThan(42_000L);
        assertThat(gradual.breaker.getState()).isEqualTo(CLOSED);
        assertThat(gradual.phaseTargets).containsExactly(25, 50, 75, 100);
        for (int second = 0; second < 42; second++) {
            assertThat(gradual.provider.receivedBySecond[second])
                .as("provider calls in second %s", second)
                .isLessThanOrEqualTo(RecoveringPaymentProvider.capacityAt(second));
        }
        assertThat(gradual.succeeded).isPositive();
        assertThat(gradual.shed).isPositive();
        assertThat(gradual.failed).isZero();
        assertThat(gradual.breaker.getAdmissionMetrics().getNumberOfAttempts())
            .isEqualTo(gradual.offered);
        assertThat(gradual.breaker.getAdmissionMetrics().getNumberOfPermittedAttempts())
            .isEqualTo(gradual.succeeded);
        for (RecoveryRun run : List.of(standard, gradual)) {
            assertThat(run.failed).isEqualTo(run.provider.overloaded);
            assertThat(run.errorEvents).isEqualTo(run.failed);
            assertThat(run.rejectedEvents).isEqualTo(run.shed);
            assertThat(run.succeeded + run.failed + run.shed).isEqualTo(run.offered);
        }
    }

    @Test
    void aFixedShareCanStillOverloadTheProviderWhenTheOfferedRateIsTooHigh() {
        RecoveryRun gradual = new RecoveryRun(true);

        gradual.offerTraffic(200, Duration.ofMillis(5));

        assertThat(gradual.provider.receivedBySecond[0])
            .isGreaterThan(RecoveringPaymentProvider.capacityAt(0));
        assertThat(gradual.failed).isPositive();
        assertThat(gradual.reopened).isEqualTo(1);
        assertThat(gradual.breaker.getState()).isEqualTo(OPEN);
        assertThat(gradual.breaker.getAdmissionMetrics().getNumberOfAttempts()).isEqualTo(200);
        assertThat(gradual.breaker.getAdmissionMetrics().getNumberOfPermittedAttempts())
            .isEqualTo(gradual.succeeded + gradual.failed);
        assertThat(gradual.errorEvents).isEqualTo(gradual.failed);
        assertThat(gradual.rejectedEvents).isEqualTo(gradual.shed);
    }

    @Test
    void retryOutsideTheBreakerPreservesSheddingWhenRejectionsAreIgnored() {
        CircuitBreaker breaker = recoveringBreaker("ignore-rejected", new MutableClock());
        Retry retry = Retry.of("ignore-rejected", RetryConfig.custom()
            .maxAttempts(4)
            .waitDuration(Duration.ZERO)
            .ignoreExceptions(CallNotPermittedException.class)
            .build());
        AtomicInteger providerCalls = new AtomicInteger();
        AtomicInteger retryEvents = new AtomicInteger();
        retry.getEventPublisher().onRetry(event -> retryEvents.incrementAndGet());
        Supplier<Integer> request = Decorators.ofSupplier(providerCalls::incrementAndGet)
            .withCircuitBreaker(breaker).withRetry(retry).decorate();

        int successfulRequests = offerLogicalRequests(request, 100);

        assertThat(successfulRequests).isEqualTo(25);
        assertThat(providerCalls.get()).isEqualTo(25);
        assertThat(retryEvents.get()).isZero();
        assertThat(breaker.getAdmissionMetrics().getNumberOfAttempts()).isEqualTo(100);
        assertThat(breaker.getAdmissionMetrics().getNumberOfPermittedAttempts()).isEqualTo(25);
        assertThat(breaker.getMetrics().getNumberOfNotPermittedCalls()).isEqualTo(75);
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(25);
    }

    @Test
    void retryingRejectionsCanAdmitEveryLogicalRequestWhileStillSheddingAttempts() {
        CircuitBreaker breaker = recoveringBreaker("retry-rejected", new MutableClock());
        Retry retry = Retry.of("retry-rejected", RetryConfig.custom()
            .maxAttempts(4)
            .waitDuration(Duration.ZERO)
            .retryExceptions(CallNotPermittedException.class)
            .build());
        AtomicInteger providerCalls = new AtomicInteger();
        AtomicInteger retryEvents = new AtomicInteger();
        retry.getEventPublisher().onRetry(event -> retryEvents.incrementAndGet());
        Supplier<Integer> request = Decorators.ofSupplier(providerCalls::incrementAndGet)
            .withCircuitBreaker(breaker).withRetry(retry).decorate();

        int successfulRequests = offerLogicalRequests(request, 100);

        assertThat(successfulRequests).isEqualTo(100);
        assertThat(providerCalls.get()).isEqualTo(100);
        CircuitBreaker.AdmissionMetrics admission = breaker.getAdmissionMetrics();
        assertThat(admission.getNumberOfAttempts()).isGreaterThanOrEqualTo(300);
        assertThat(admission.getNumberOfPermittedAttempts()).isEqualTo(100);
        assertThat(100.0 * admission.getNumberOfPermittedAttempts()
            / admission.getNumberOfAttempts()).isBetween(24.0, 26.0);
        assertThat(retryEvents.get()).isEqualTo((int) (admission.getNumberOfAttempts() - 100));
        assertThat(breaker.getMetrics().getNumberOfNotPermittedCalls())
            .isEqualTo(retryEvents.get());
        assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(25);
    }

    private static int offerLogicalRequests(Supplier<Integer> request, int numberOfRequests) {
        int succeeded = 0;
        for (int i = 0; i < numberOfRequests; i++) {
            try {
                request.get();
                succeeded++;
            } catch (CallNotPermittedException ignored) {
                // The application observes shedding when Retry propagates this rejection.
            }
        }
        return succeeded;
    }

    private static CircuitBreakerConfig.Builder baseConfig(MutableClock clock) {
        return CircuitBreakerConfig.custom()
            .clock(clock)
            .initialState(HALF_OPEN)
            .permittedNumberOfCallsInHalfOpenState(10)
            .slidingWindowSize(20)
            .minimumNumberOfCalls(20)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(1));
    }

    private static CircuitBreaker recoveringBreaker(String name, MutableClock clock) {
        return CircuitBreaker.of(name, baseConfig(clock)
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom()
                .trafficPercentageStep(25)
                .minimumNumberOfCalls(20)
                .minimumPhaseDuration(Duration.ofSeconds(10))
                .build())
            .build());
    }

    private static final class RecoveryRun {
        private final MutableClock clock = new MutableClock();
        private final RecoveringPaymentProvider provider = new RecoveringPaymentProvider(clock);
        private final CircuitBreaker breaker;
        private final List<Long> closedAtMillis = new ArrayList<>();
        private final List<Integer> phaseTargets = new ArrayList<>();
        private int offered;
        private int succeeded;
        private int failed;
        private int shed;
        private int reopened;
        private int errorEvents;
        private int rejectedEvents;

        private RecoveryRun(boolean gradual) {
            breaker = gradual ? recoveringBreaker("gradual", clock)
                : CircuitBreaker.of("standard", baseConfig(clock).build());
            if (gradual) {
                phaseTargets.add(breaker.getHalfOpenPhase().getPermittedTrafficPercentage());
            }
            breaker.getEventPublisher()
                .onHalfOpenPhaseTransition(event -> phaseTargets.add(
                    event.getPhase().getPermittedTrafficPercentage()))
                .onError(event -> errorEvents++)
                .onCallNotPermitted(event -> rejectedEvents++)
                .onStateTransition(event -> {
                    if (event.getStateTransition().getToState() == OPEN) {
                        reopened++;
                    } else if (event.getStateTransition().getToState() == CLOSED) {
                        closedAtMillis.add(clock.millis());
                    }
                });
        }

        private void offerTraffic(int numberOfRequests, Duration interval) {
            Supplier<Integer> request = CircuitBreaker.decorateSupplier(breaker, provider::pay);
            for (int i = 0; i < numberOfRequests; i++) {
                offered++;
                try {
                    request.get();
                    succeeded++;
                } catch (CallNotPermittedException ignored) {
                    shed++;
                } catch (PaymentCapacityExceededException ignored) {
                    failed++;
                }
                clock.advance(interval);
            }
        }
    }

    /** An illustrative provider model: capacity depends only on elapsed time. */
    private static final class RecoveringPaymentProvider {
        private final Clock clock;
        private final int[] receivedBySecond = new int[42];
        private int overloaded;

        private RecoveringPaymentProvider(Clock clock) {
            this.clock = clock;
        }

        private static int capacityAt(int second) {
            if (second < 10) {
                return 25;
            }
            if (second < 20) {
                return 50;
            }
            if (second < 30) {
                return 75;
            }
            return 100;
        }

        private int pay() {
            int second = (int) clock.instant().getEpochSecond();
            if (++receivedBySecond[second] > capacityAt(second)) {
                overloaded++;
                throw new PaymentCapacityExceededException();
            }
            return second;
        }
    }

    private static final class PaymentCapacityExceededException extends RuntimeException {
    }

    private static final class MutableClock extends Clock {
        private Instant current = Instant.EPOCH;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }

        private void advance(Duration duration) {
            current = current.plus(duration);
        }
    }
}
