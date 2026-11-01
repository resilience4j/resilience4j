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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.HalfOpenRecoveryConfig;
import io.github.resilience4j.metrics.publisher.CircuitBreakerMetricsPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PhasedCircuitBreakerMetricsTest {

    private static final String PREFIX = "custom";
    private static final String NAME = "backend";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void exportsNumericTrafficTargetAndPreservesLegacyStateGauge(boolean publisher) {
        MutableClock clock = new MutableClock();
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = registry(meters, publisher);
        CircuitBreaker breaker = registry.circuitBreaker(NAME, phasedConfig(clock));
        if (!publisher) {
            meters.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry(PREFIX, registry));
        }
        Set<String> names = Set.copyOf(meters.getNames());

        assertThat(names).hasSize(14);
        assertGauge(meters, "state", 0);
        assertGauge(meters, "recovery.phase", 0);
        assertGauge(meters, "recovery.traffic.percentage", 0);
        breaker.transitionToOpenState();
        assertGauge(meters, "state", 1);
        breaker.transitionToHalfOpenState();
        assertGauge(meters, "state", 2);
        assertGauge(meters, "recovery.phase", 1);
        assertGauge(meters, "recovery.traffic.percentage", 50);

        completeAcceptedCalls(breaker, 2);
        assertGauge(meters, "recovery.phase", 1);
        clock.advance(Duration.ofSeconds(10));
        completeAcceptedCalls(breaker, 1);
        assertGauge(meters, "state", 2);
        assertGauge(meters, "recovery.phase", 2);
        assertGauge(meters, "recovery.traffic.percentage", 100);

        assertThat(meters.getNames()).containsExactlyInAnyOrderElementsOf(names);

        breaker.reset();
        assertGauge(meters, "state", 0);
        assertGauge(meters, "recovery.phase", 0);
        assertGauge(meters, "recovery.traffic.percentage", 0);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ordinaryBreakersKeepTheExistingTenMetrics(boolean publisher) {
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = registry(meters, publisher);
        CircuitBreaker breaker = registry.circuitBreaker(NAME);
        if (!publisher) {
            meters.registerAll(CircuitBreakerMetrics.ofCircuitBreakerRegistry(PREFIX, registry));
        }

        assertThat(meters.getMetrics()).hasSize(10);
        assertThat(meters.getNames()).noneMatch(name -> name.contains(".recovery."));
        breaker.transitionToDisabledState();
        assertGauge(meters, "state", 3);
        breaker.transitionToForcedOpenState();
        assertGauge(meters, "state", 4);
        breaker.transitionToMetricsOnlyState();
        assertGauge(meters, "state", 5);
    }

    @Test
    void publisherRemovesRecoveryGaugesWhenReplacingOrRemovingAnInstance() {
        MutableClock clock = new MutableClock();
        MetricRegistry meters = new MetricRegistry();
        CircuitBreakerRegistry registry = registry(meters, true);
        CircuitBreaker original = registry.circuitBreaker(NAME, phasedConfig(clock));
        original.transitionToOpenState();
        original.transitionToHalfOpenState();
        assertGauge(meters, "recovery.traffic.percentage", 50);

        CircuitBreaker ordinaryReplacement = CircuitBreaker.ofDefaults(NAME);

        assertThat(registry.replace(NAME, ordinaryReplacement)).contains(original);
        assertThat(meters.getMetrics()).hasSize(10);
        assertThat(meters.getNames()).noneMatch(name -> name.contains(".recovery."));
        assertGauge(meters, "state", 0);

        CircuitBreaker phasedReplacement = CircuitBreaker.of(NAME, phasedConfig(clock));
        registry.replace(NAME, phasedReplacement);

        assertThat(meters.getMetrics()).hasSize(14);
        assertGauge(meters, "recovery.phase", 0);
        phasedReplacement.transitionToOpenState();
        phasedReplacement.transitionToHalfOpenState();
        assertGauge(meters, "recovery.traffic.percentage", 50);
        registry.remove(NAME);

        assertThat(meters.getMetrics()).isEmpty();
    }

    private static CircuitBreakerRegistry registry(MetricRegistry meters, boolean publisher) {
        return publisher ? CircuitBreakerRegistry.of(CircuitBreakerConfig.ofDefaults(),
            new CircuitBreakerMetricsPublisher(PREFIX, meters)) : CircuitBreakerRegistry.ofDefaults();
    }

    private static CircuitBreakerConfig phasedConfig(Clock clock) {
        HalfOpenRecoveryConfig recovery = HalfOpenRecoveryConfig.custom()
            .initialTrafficPercentage(50).trafficPercentageStep(50)
            .minimumPhaseDuration(Duration.ofSeconds(10)).minimumNumberOfCalls(2).build();
        return CircuitBreakerConfig.custom().clock(clock).halfOpenRecoveryConfig(recovery).build();
    }

    private static void assertGauge(MetricRegistry meters, String suffix, int expected) {
        assertThat(meters.getGauges().get(PREFIX + "." + NAME + "." + suffix).getValue())
            .isEqualTo(expected);
    }

    private static void completeAcceptedCalls(CircuitBreaker breaker, int expected) {
        int completed = 0;
        for (int attempt = 0; attempt < 1000 && completed < expected; attempt++) {
            if (breaker.tryAcquirePermission()) {
                breaker.onSuccess(5, TimeUnit.MILLISECONDS);
                completed++;
            }
        }

        assertThat(completed).isEqualTo(expected);
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
