/*
 * Copyright 2018 Julien Hoarau
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
package io.github.resilience4j.micrometer;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class CircuitBreakerMetricsTest {

    private static final String CIRCUIT_BREAKER_NAME = "testCircuitBreaker";

    private MeterRegistry meterRegistry;

    private static boolean isAGauge(Meter meter) {

        return meter.getId().getType().equals(Meter.Type.GAUGE);
    }

    private static boolean isTaggedWithCircuitBreakerName(Meter meter) {

        return Objects.equals(meter.getId().getTag("name"), CircuitBreakerMetricsTest.CIRCUIT_BREAKER_NAME);
    }

    private List<Meter> getMetersWithNamesEndingIn(final String state) {

        return meterRegistry.getMeters()
                .stream()
                .filter(meter -> meter.getId().getName().endsWith(state))
                .collect(Collectors.toList());
    }

    private List<String> meterTagValues(final List<Meter> callMeters, String tagKey) {

        return callMeters
                .stream()
                .map(meter -> meter.getId().getTag(tagKey))
                .collect(Collectors.toList());
    }

    private void assertThatForAllMeters(Predicate<Meter> predicate) {

        assertThat(meterRegistry.getMeters().stream().allMatch(predicate)).isTrue();
    }

    @Before
    public void setUp() {

        meterRegistry = new SimpleMeterRegistry();
        CircuitBreakerRegistry circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();

        circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);

        CircuitBreakerMetrics circuitBreakerMetrics = CircuitBreakerMetrics.ofCircuitBreakerRegistry(circuitBreakerRegistry);
        circuitBreakerMetrics.bindTo(meterRegistry);
    }

    @Test
    public void shouldRegisterMetersWithDefaultPrefix() {

        assertThatForAllMeters(meter -> meter.getId().getName().startsWith("resilience4j.circuitbreaker"));
    }

    @Test
    public void shouldRegisterMetersAsGauge() {

        assertThatForAllMeters(CircuitBreakerMetricsTest::isAGauge);
    }

    @Test
    public void shouldRegisterMetersWithNameTag() {

        assertThatForAllMeters(CircuitBreakerMetricsTest::isTaggedWithCircuitBreakerName);
    }

    @Test
    public void shouldRegisterStateMeters() {

        final List<Meter> stateMeters = getMetersWithNamesEndingIn("state");

        assertThat(stateMeters.size()).isEqualTo(1);
        assertMeterNameIs(stateMeters.get(0), "resilience4j.circuitbreaker.state");
    }

    private AbstractCharSequenceAssert<?, String> assertMeterNameIs(Meter meter, String expectedName) {

        return assertThat(meter.getId().getName()).isEqualTo(expectedName);
    }

    @Test
    public void shouldRegisterCallsMeters() {

        final List<Meter> callMeters = getMetersWithNamesEndingIn("calls");
        final List<String> allCallResultValues = meterTagValues(callMeters, "call_result");

        assertThat(callMeters.size()).isEqualTo(3);
        assertThat(callMeters.stream().allMatch( meter -> meter.getId().getName().equals("resilience4j.circuitbreaker.calls"))).isTrue();
        assertThat(allCallResultValues)
                .containsExactly("successful", "failed", "not_permitted");
    }

    @Test
    public void shouldRegisterBufferMeter() {

        final List<Meter> bufferMeters = getMetersWithNamesEndingIn("buffer");

        assertThat(bufferMeters.size()).isEqualTo(1);
        assertMeterNameIs(bufferMeters.get(0),"resilience4j.circuitbreaker.buffer");
    }

    @Test
    public void shouldRegisterBufferConfigMaxMeter() {

        final List<Meter> bufferMeters =
                getMetersWithNamesEndingIn("config.buffer.max");

        assertThat(bufferMeters.size()).isEqualTo(1);
        assertMeterNameIs(bufferMeters.get(0),"resilience4j.circuitbreaker.config.buffer.max");
    }
}