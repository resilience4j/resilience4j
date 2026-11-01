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
package io.github.resilience4j.metrics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.github.resilience4j.metrics.publisher.CircuitBreakerMetricsPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdmissionCircuitBreakerMetricsTest {
    private static final String PREFIX = "custom";
    private static final String NAME = "observed";
    private static final String ATTEMPTS = PREFIX + "." + NAME + ".admission.attempts";
    private static final String PERMITTED = PREFIX + "." + NAME + ".admission.permitted.attempts";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void countersRecordPendingAdmissionsAndDoNotDependOnCompletedOutcomes(boolean publisher) {
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = registry(meters, publisher);
        CircuitBreaker breaker = registry.circuitBreaker(NAME, config(new MutableClock(), 20, 10));
        if (!publisher) {
            meters.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry(PREFIX, registry));
        }
        enterRecovery(breaker);

        offerMixedAttempts(breaker, 100);

        assertCounts(meters, 100, 20);
        assertThat(breaker.getMetrics().getNumberOfBufferedCalls()).isZero();
        breaker.onSuccess(1, TimeUnit.MILLISECONDS);
        breaker.onError(1, TimeUnit.MILLISECONDS, new IllegalStateException("failed"));
        breaker.onError(1, TimeUnit.MILLISECONDS, new IllegalArgumentException("ignored"));
        breaker.releasePermission();

        assertCounts(meters, 100, 20);
        assertThat(meters.getGauges().get(PREFIX + "." + NAME + ".recovery.traffic.percentage").getValue())
            .isEqualTo(20);
        assertThat(meters.getMetrics()).hasSize(14);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void lateBindingIncludesEarlierAttemptsAndStillWorksWhenEventsAreDisabled(boolean publisher) {
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = registry.circuitBreaker(NAME, config(new MutableClock(), 20, 10));
        offerMixedAttempts(breaker, 10);
        breaker.transitionToDisabledState();
        offerMixedAttempts(breaker, 20);
        if (publisher) {
            new CircuitBreakerMetricsPublisher(PREFIX, meters).publishMetrics(breaker);
        } else {
            meters.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry(PREFIX, registry));
        }

        assertCounts(meters, 30, 30);
        offerMixedAttempts(breaker, 10);

        assertCounts(meters, 40, 40);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void phaseChangesAndResetPreserveLifetimeCounters(boolean publisher) {
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = registry(meters, publisher);
        MutableClock clock = new MutableClock();
        CircuitBreaker breaker = registry.circuitBreaker(NAME, config(clock, 50, 2));
        if (!publisher) {
            meters.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry(PREFIX, registry));
        }
        Set<String> metricNames = Set.copyOf(meters.getNames());
        enterRecovery(breaker);
        for (int i = 0; i < 3; i++) {
            if (breaker.tryAcquirePermission()) {
                breaker.onSuccess(1, TimeUnit.MILLISECONDS);
            }
        }

        assertCounts(meters, 3, 2);
        clock.advance(Duration.ofSeconds(10));

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertCounts(meters, 4, 2);
        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(50);
        assertThat(breaker.tryAcquirePermission()).isTrue();
        breaker.onSuccess(1, TimeUnit.MILLISECONDS);
        assertCounts(meters, 5, 3);
        assertThat(breaker.getHalfOpenPhase().getPermittedTrafficPercentage()).isEqualTo(100);
        breaker.reset();

        assertCounts(meters, 5, 3);
        breaker.acquirePermission();
        breaker.transitionToOpenState();

        assertThat(breaker.tryAcquirePermission()).isFalse();
        assertCounts(meters, 7, 4);
        assertThat(meters.getNames()).containsExactlyInAnyOrderElementsOf(metricNames);
    }

    @Test
    void publisherReplacementAndRemovalRetireCountersForTheOldInstance() {
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = registry(meters, true);
        CircuitBreaker original = registry.circuitBreaker(NAME, config(new MutableClock(), 20, 10));
        offerMixedAttempts(original, 100);

        assertCounts(meters, 100, 100);
        CircuitBreaker replacement = CircuitBreaker.of(NAME, config(new MutableClock(), 20, 10));
        registry.replace(NAME, replacement);

        assertCounts(meters, 0, 0);
        replacement.acquirePermission();

        assertCounts(meters, 1, 1);
        registry.remove(NAME);

        assertThat(meters.getMetrics()).isEmpty();
        registry.circuitBreaker(NAME);

        assertThat(meters.getMetrics()).hasSize(10);
        assertThat(meters.getCounters()).doesNotContainKeys(ATTEMPTS, PERMITTED);
    }

    @Test
    void exportedCountersCannotBeMutatedIndependentlyOfAdmissionDecisions() {
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = registry(meters, true);
        registry.circuitBreaker(NAME, config(new MutableClock(), 20, 10));

        assertThatThrownBy(() -> meters.getCounters().get(ATTEMPTS).inc())
            .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> meters.getCounters().get(PERMITTED).dec(10))
            .isInstanceOf(UnsupportedOperationException.class);
        assertCounts(meters, 0, 0);
    }

    private static CircuitBreakerRegistry registry(MetricRegistry meters, boolean publisher) {
        return publisher ? CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(),
            new CircuitBreakerMetricsPublisher(PREFIX, meters)) : CircuitBreakerRegistry.ofDefaults();
    }

    private static CircuitBreakerConfig config(Clock clock, int percentage, int minimumCalls) {
        return CircuitBreakerConfig.custom().clock(clock).ignoreExceptions(IllegalArgumentException.class)
            .halfOpenRecoveryConfig(HalfOpenRecoveryConfig.custom().initialTrafficPercentage(percentage)
                .trafficPercentageStep(50).minimumPhaseDuration(Duration.ofSeconds(10))
                .minimumNumberOfCalls(minimumCalls).build()).build();
    }

    private static void enterRecovery(CircuitBreaker breaker) {
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
    }

    private static void offerMixedAttempts(CircuitBreaker breaker, int attempts) {
        for (int i = 0; i < attempts; i++) {
            if (i % 3 == 0) {
                try {
                    breaker.acquirePermission();
                } catch (CallNotPermittedException expected) {
                    // No completion follows an admission rejection.
                }
            } else {
                breaker.tryAcquirePermission();
            }
        }
    }

    private static void assertCounts(MetricRegistry meters, long attempts, long admitted) {
        assertThat(meters.getCounters().get(ATTEMPTS).getCount()).isEqualTo(attempts);
        assertThat(meters.getCounters().get(PERMITTED).getCount()).isEqualTo(admitted);
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
