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
package io.github.resilience4j.micrometer.tagged;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.github.resilience4j.micrometer.tagged.CircuitBreakerMetricNames.*;
import static org.assertj.core.api.Assertions.assertThat;

class AdmissionCircuitBreakerMetricsTest {

    private static final String NAME = "observed";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void recordsAdmissionsBeforeAnyCompletionAndDoesNotRecountOutcomes(boolean publisher) {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME, config(new MutableClock(), 50, 10));
            enterRecovery(breaker);

            offerMixedAttempts(breaker, 100);

            assertCounts(meters, 100, 50);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_CALLS).tag("kind", "successful").timer().count())
                .isZero();
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_CALLS).tag("kind", "failed").timer().count())
                .isZero();
            breaker.onSuccess(1, TimeUnit.MILLISECONDS);
            breaker.onError(1, TimeUnit.MILLISECONDS, new IllegalStateException("failure"));
            breaker.onError(1, TimeUnit.MILLISECONDS, new IllegalArgumentException("ignored"));
            breaker.releasePermission();

            assertCounts(meters, 100, 50);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_RECOVERY_TRAFFIC_PERCENTAGE).gauge().value())
                .isEqualTo(50);
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void intervalDeltasMeasureAdmissionSeparatelyFromTheTargetAndLifetimeRatio(boolean publisher) {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME, config(new MutableClock(), 20, 10));
            offerMixedAttempts(breaker, 100); // CLOSED: all calls permitted, still in flight.
            double attemptsBefore = attempts(meters);
            double admittedBefore = admitted(meters);
            enterRecovery(breaker);
            offerMixedAttempts(breaker, 100);

            assertCounts(meters, 200, 120);
            double actualPercentage = 100 * (admitted(meters) - admittedBefore)
                / (attempts(meters) - attemptsBefore);

            assertThat(actualPercentage).isEqualTo(20);
            assertThat(100 * admitted(meters) / attempts(meters)).isEqualTo(60);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_RECOVERY_TRAFFIC_PERCENTAGE).gauge().value())
                .isEqualTo(20);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_CALLS).tag("kind", "successful").timer().count())
                .isZero();
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void countersSurvivePhaseChangesResetAndOpenWhileTheTargetRemainsIndependent(boolean publisher) {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            MutableClock clock = new MutableClock();
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME, config(clock, 50, 2));
            Set<Meter.Id> originalIds = ids(meters);
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
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_RECOVERY_TRAFFIC_PERCENTAGE).gauge().value())
                .isEqualTo(100);

            breaker.reset();

            assertCounts(meters, 5, 3);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_RECOVERY_TRAFFIC_PERCENTAGE).gauge().value())
                .isZero();
            breaker.acquirePermission();
            breaker.transitionToOpenState();

            assertThat(breaker.tryAcquirePermission()).isFalse();
            assertCounts(meters, 7, 4);
            assertThat(ids(meters)).containsExactlyInAnyOrderElementsOf(originalIds);
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void bindingAfterActivityIncludesPriorDecisionsWithoutDependingOnEvents(boolean publisher) {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
            CircuitBreaker breaker = registry.circuitBreaker(NAME, config(new MutableClock(), 20, 10));
            enterRecovery(breaker);
            offerMixedAttempts(breaker, 100);
            if (publisher) {
                new TaggedCircuitBreakerMetricsPublisher(meters).publishMetrics(breaker);
            } else {
                TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);
            }

            assertCounts(meters, 100, 20);
            offerMixedAttempts(breaker, 100);

            assertCounts(meters, 200, 40);
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void removalAndReplacementUseTheNewInstanceCountersWithTheSameStableTags(boolean publisher) {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerMetricNames names = CircuitBreakerMetricNames.custom()
                .attemptsMetricName("custom.admission.attempts")
                .permittedAttemptsMetricName("custom.admission.permitted").build();
            CircuitBreakerRegistry registry = registry(meters, publisher, names);
            CircuitBreaker original = registry.circuitBreaker(NAME,
                config(new MutableClock(), 50, 10), Map.of("region", "eu"));
            offerMixedAttempts(original, 10);

            assertThat(meters.get("custom.admission.attempts")
                .tags("name", NAME, "region", "eu")
                .functionCounter()
                .count())
                .isEqualTo(10);
            assertThat(meters.get("custom.admission.permitted")
                .tags("name", NAME, "region", "eu")
                .functionCounter()
                .count())
                .isEqualTo(10);
            assertThat(meters.getMeters()).hasSize(20);
            assertThat(meters.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags()).noneSatisfy(tag ->
                    assertThat(tag.getKey()).isIn("phase", "percentage", "attempt")));

            CircuitBreaker replacement = CircuitBreaker.of(NAME,
                config(new MutableClock(), 50, 10), Map.of("region", "eu"));
            registry.replace(NAME, replacement);

            assertThat(meters.get("custom.admission.attempts").functionCounter().count()).isZero();
            replacement.acquirePermission();

            assertThat(meters.get("custom.admission.permitted").functionCounter().count()).isEqualTo(1);
            registry.remove(NAME);

            assertThat(meters.getMeters()).isEmpty();

            registry.circuitBreaker(NAME);

            assertThat(meters.getMeters()).hasSize(16);
            assertThat(meters.find("custom.admission.attempts").functionCounter()).isNull();
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void prometheusExposesAdmissionCountersAlongsideTheTargetGauge(boolean publisher) {
        PrometheusMeterRegistry meters = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME,
                config(new MutableClock(), 20, 10), Map.of("region", "eu"));
            enterRecovery(breaker);
            offerMixedAttempts(breaker, 100);

            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_ATTEMPTS).functionCounter().getId().getType())
                .isEqualTo(Meter.Type.COUNTER);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_PERMITTED_ATTEMPTS)
                .functionCounter()
                .getId()
                .getType())
                .isEqualTo(Meter.Type.COUNTER);
            String scrape = meters.scrape();

            assertThat(scrape)
                .containsPattern("# TYPE resilience4j_circuitbreaker_admission_attempts(_total)? counter");
            assertThat(scrape)
                .containsPattern("# TYPE resilience4j_circuitbreaker_admission_permitted_attempts(_total)? counter");
            assertThat(scrape).contains(
                "resilience4j_circuitbreaker_admission_attempts_total{name=\"observed\",region=\"eu\"} 100.0",
                "resilience4j_circuitbreaker_admission_permitted_attempts_total{name=\"observed\",region=\"eu\"} 20.0",
                "resilience4j_circuitbreaker_recovery_traffic_percentage{name=\"observed\",region=\"eu\"} 20.0");
        } finally {
            meters.close();
        }
    }

    private static CircuitBreakerRegistry registry(MeterRegistry meters, boolean publisher,
        CircuitBreakerMetricNames names) {
        if (publisher) {
            return CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(),
                new TaggedCircuitBreakerMetricsPublisher(names, meters));
        }
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(names, registry).bindTo(meters);
        return registry;
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
                    // Intentional admission rejection; no completion is recorded.
                }
            } else {
                breaker.tryAcquirePermission();
            }
        }
    }

    private static double attempts(MeterRegistry meters) {
        return meters.get(DEFAULT_CIRCUIT_BREAKER_ATTEMPTS).functionCounter().count();
    }

    private static double admitted(MeterRegistry meters) {
        return meters.get(DEFAULT_CIRCUIT_BREAKER_PERMITTED_ATTEMPTS).functionCounter().count();
    }

    private static void assertCounts(MeterRegistry meters, long attempts, long admitted) {
        assertThat(attempts(meters)).isEqualTo(attempts);
        assertThat(admitted(meters)).isEqualTo(admitted);
    }

    private static Set<Meter.Id> ids(MeterRegistry meters) {
        return meters.getMeters().stream().map(Meter::getId).collect(Collectors.toSet());
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
