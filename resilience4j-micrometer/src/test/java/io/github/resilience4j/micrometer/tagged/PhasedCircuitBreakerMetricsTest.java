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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.github.resilience4j.micrometer.tagged.CircuitBreakerMetricNames.*;
import static org.assertj.core.api.Assertions.assertThat;

class PhasedCircuitBreakerMetricsTest {

    private static final String NAME = "phased";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void tracksTrafficTargetsWithoutResettingCumulativeCallsOrCreatingPhaseSeries(boolean publisher) {
        MutableClock clock = new MutableClock();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME, phasedConfig(clock, 5));
            Set<Meter.Id> ids = ids(meters);

            assertThat(ids).hasSize(20);
            assertRecovery(meters, 0, 0);
            breaker.transitionToOpenState();
            breaker.transitionToHalfOpenState();

            assertRecovery(meters, 1, 5);
            assertHalfOpen(meters);

            completeAcceptedCalls(breaker, 10, false);

            assertRecovery(meters, 1, 5);
            assertThat(successCount(meters)).isEqualTo(10);
            clock.advance(Duration.ofSeconds(10));
            completeAcceptedCalls(breaker, 1, false);

            assertRecovery(meters, 2, 10);
            assertHalfOpen(meters);
            assertThat(successCount(meters)).isEqualTo(11);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_CALLS).tag("kind", "successful")
                .timer().totalTime(TimeUnit.MILLISECONDS)).isEqualTo(55);
            assertThat(ids(meters)).containsExactlyInAnyOrderElementsOf(ids);
            assertThat(meters.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags()).noneSatisfy(tag ->
                    assertThat(tag.getKey()).isIn("phase", "attempt", "percentage")));
        } finally {
            meters.close();
        }
    }

    @Test
    void hundredPercentAdmissionDoesNotExhaustAFiniteCallQuota() {
        MutableClock clock = new MutableClock();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, false, CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME, phasedConfig(clock, 100));
            breaker.transitionToOpenState();
            breaker.transitionToHalfOpenState();

            for (int i = 0; i < 1000; i++) {
                assertThat(breaker.tryAcquirePermission()).as("Admission %s at 100%%", i).isTrue();
                breaker.onSuccess(5, TimeUnit.MILLISECONDS);
            }

            assertRecovery(meters, 1, 100);
            assertHalfOpen(meters);
            assertThat(successCount(meters)).isEqualTo(1000);
            clock.advance(Duration.ofSeconds(10));

            assertThat(breaker.tryAcquirePermission()).isTrue();
            breaker.onSuccess(5, TimeUnit.MILLISECONDS);

            assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
            assertRecovery(meters, 0, 0);
            assertThat(successCount(meters)).isEqualTo(1001);
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 50, 100})
    void admitsTheConfiguredPercentageOfEveryHundredAttempts(int percentage) {
        MutableClock clock = new MutableClock();
        CircuitBreaker breaker = CircuitBreaker.of(NAME, phasedConfig(clock, percentage));
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();

        for (int block = 0; block < 3; block++) {
            int admitted = 0;
            for (int i = 0; i < 100; i++) {
                if (breaker.tryAcquirePermission()) {
                    admitted++;
                    breaker.onSuccess(5, TimeUnit.MILLISECONDS);
                }
            }

            assertThat(admitted).isEqualTo(percentage);
        }

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reportsStepBackAndPreservesFailureTimer(boolean publisher) {
        MutableClock clock = new MutableClock();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME, phasedConfig(clock, 5));
            breaker.transitionToOpenState();
            breaker.transitionToHalfOpenState();
            completeAcceptedCalls(breaker, 10, false);
            clock.advance(Duration.ofSeconds(10));
            completeAcceptedCalls(breaker, 1, false);
            completeAcceptedCalls(breaker, 10, false);
            clock.advance(Duration.ofSeconds(10));
            completeAcceptedCalls(breaker, 1, false);

            assertRecovery(meters, 3, 15);

            clock.advance(Duration.ofSeconds(10));
            completeAcceptedCalls(breaker, 10, true);

            assertRecovery(meters, 2, 10);
            assertHalfOpen(meters);
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_CALLS).tag("kind", "failed").timer().count())
                .isEqualTo(10);
            assertThat(successCount(meters)).isEqualTo(22);
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deniedAttemptsRemainCountedAfterPhaseAdvance(boolean publisher) {
        MutableClock clock = new MutableClock();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME, phasedConfig(clock, 5));
            breaker.transitionToOpenState();
            breaker.transitionToHalfOpenState();
            int denied = completeAcceptedCalls(breaker, 10, false);
            clock.advance(Duration.ofSeconds(10));
            denied += completeAcceptedCalls(breaker, 1, false);

            assertRecovery(meters, 2, 10);
            assertThat(denied).isPositive();
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_NOT_PERMITTED_CALLS)
                .counter().count()).isEqualTo(denied);
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ordinaryBreakersKeepTheExistingMetricSet(boolean publisher) {
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            registry.circuitBreaker(NAME);

            assertThat(meters.getMeters()).hasSize(16);
            assertThat(meters.find(DEFAULT_CIRCUIT_BREAKER_RECOVERY_PHASE).gauge()).isNull();
            assertThat(meters.find(DEFAULT_CIRCUIT_BREAKER_RECOVERY_TRAFFIC_PERCENTAGE).gauge()).isNull();
            assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_STATE).gauges()).extracting(
                gauge -> gauge.getId().getTag("state"))
                .containsExactlyInAnyOrder("closed", "open", "half_open",
                    "disabled", "forced_open", "metrics_only");
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void customNamesTagsAndLifecycleApplyToRecoveryMetrics(boolean publisher) {
        MutableClock clock = new MutableClock();
        CircuitBreakerMetricNames names = CircuitBreakerMetricNames.custom()
            .recoveryPhaseMetricName("custom.recovery.phase")
            .recoveryTrafficPercentageMetricName("custom.recovery.traffic.percentage").build();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            CircuitBreakerRegistry registry = registry(meters, publisher, names);
            CircuitBreaker old = registry.circuitBreaker(NAME,
                phasedConfig(clock, 5), Map.of("region", "eu"));
            old.transitionToOpenState();
            old.transitionToHalfOpenState();

            assertThat(meters.get("custom.recovery.phase").tags("name", NAME, "region", "eu")
                .gauge().value()).isEqualTo(1);
            assertThat(meters.get("custom.recovery.traffic.percentage").tags("name", NAME, "region", "eu")
                .gauge().value()).isEqualTo(5);
            registry.remove(NAME);

            assertThat(meters.getMeters()).isEmpty();

            CircuitBreaker replacement = registry.circuitBreaker(NAME,
                phasedConfig(clock, 5), Map.of("region", "eu"));

            assertThat(replacement).isNotSameAs(old);
            assertThat(meters.get("custom.recovery.phase").gauge().value()).isZero();
            assertThat(meters.getMeters()).hasSize(20);
        } finally {
            meters.close();
        }
    }

    @Test
    void bindsRecoveryGaugesForAnExistingBreaker() {
        MutableClock clock = new MutableClock();
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();
        CircuitBreaker breaker = registry.circuitBreaker(NAME, phasedConfig(clock, 5));
        breaker.transitionToOpenState();
        breaker.transitionToHalfOpenState();
        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        try {
            TaggedCircuitBreakerMetrics.ofCircuitBreakerRegistry(registry).bindTo(meters);

            assertRecovery(meters, 1, 5);
            assertHalfOpen(meters);
        } finally {
            meters.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void prometheusExposesTrafficTargetAsGaugeValueWithStableLabels(boolean publisher) {
        MutableClock clock = new MutableClock();
        PrometheusMeterRegistry meters = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        try {
            List<String> registrationFailures = new ArrayList<>();
            meters.config().onMeterRegistrationFailed((id, reason) -> registrationFailures.add(reason));
            CircuitBreakerRegistry registry = registry(meters, publisher,
                CircuitBreakerMetricNames.ofDefaults());
            CircuitBreaker breaker = registry.circuitBreaker(NAME,
                phasedConfig(clock, 5), Map.of("region", "eu"));
            breaker.transitionToOpenState();
            breaker.transitionToHalfOpenState();

            assertThat(meters.scrape()).contains(
                "resilience4j_circuitbreaker_recovery_phase{name=\"phased\",region=\"eu\"} 1.0",
                "resilience4j_circuitbreaker_recovery_traffic_percentage{name=\"phased\",region=\"eu\"} 5.0");
            Set<Meter.Id> before = ids(meters);
            completeAcceptedCalls(breaker, 10, false);
            clock.advance(Duration.ofSeconds(10));
            completeAcceptedCalls(breaker, 1, false);

            assertThat(meters.scrape()).contains(
                "resilience4j_circuitbreaker_recovery_phase{name=\"phased\",region=\"eu\"} 2.0",
                "resilience4j_circuitbreaker_recovery_traffic_percentage{name=\"phased\",region=\"eu\"} 10.0");

            assertThat(ids(meters)).containsExactlyInAnyOrderElementsOf(before);
            assertThat(registrationFailures).isEmpty();
        } finally {
            meters.close();
        }
    }

    private static CircuitBreakerConfig phasedConfig(Clock clock, int initialPercentage) {
        HalfOpenRecoveryConfig recovery = HalfOpenRecoveryConfig.custom()
            .initialTrafficPercentage(initialPercentage).trafficPercentageStep(5)
            .minimumPhaseDuration(Duration.ofSeconds(10)).minimumNumberOfCalls(10)
            .failureStrategy(HalfOpenRecoveryConfig.FailureStrategy.STEP_BACK).build();
        return CircuitBreakerConfig.custom().clock(clock).halfOpenRecoveryConfig(recovery).build();
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

    private static int completeAcceptedCalls(CircuitBreaker breaker, int count, boolean failure) {
        int completed = 0;
        int denied = 0;
        for (int attempt = 0; attempt < 10000 && completed < count; attempt++) {
            if (breaker.tryAcquirePermission()) {
                if (failure) {
                    breaker.onError(5, TimeUnit.MILLISECONDS, new IllegalStateException("unavailable"));
                } else {
                    breaker.onSuccess(5, TimeUnit.MILLISECONDS);
                }
                completed++;
            } else {
                denied++;
            }
        }

        assertThat(completed).isEqualTo(count);
        return denied;
    }

    private static Set<Meter.Id> ids(MeterRegistry meters) {
        return meters.getMeters().stream().map(Meter::getId).collect(Collectors.toSet());
    }

    private static long successCount(MeterRegistry meters) {
        return meters.get(DEFAULT_CIRCUIT_BREAKER_CALLS).tag("kind", "successful").timer().count();
    }

    private static void assertRecovery(MeterRegistry meters, int phase, int percentage) {
        assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_RECOVERY_PHASE).gauge().value()).isEqualTo(phase);
        assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_RECOVERY_TRAFFIC_PERCENTAGE).gauge().value())
            .isEqualTo(percentage);
    }

    private static void assertHalfOpen(MeterRegistry meters) {
        assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_STATE).tag("state", "half_open")
            .gauge().value()).isEqualTo(1);
        assertThat(meters.get(DEFAULT_CIRCUIT_BREAKER_STATE).gauges().stream()
            .mapToDouble(gauge -> gauge.value()).sum()).isEqualTo(1);
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
