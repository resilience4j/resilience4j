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

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

public class BulkheadMetricsTest {

    private MeterRegistry meterRegistry;

    @Before
    public void setUp() throws Exception {
        meterRegistry = new SimpleMeterRegistry();
    }

    @Test
    public void shouldRegisterMetrics() {
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        bulkheadRegistry.bulkhead("testName");

        BulkheadMetrics bulkheadMetrics = BulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry);
        bulkheadMetrics.bindTo(meterRegistry);

        final List<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toList());

        final List<String> expectedMetrics = newArrayList(
                "resilience4j.bulkhead.testName.available_concurrent_calls",
                "resilience4j.bulkhead.testName.max_allowed_concurrent_calls"
        );
        assertThat(metricNames).hasSameElementsAs(expectedMetrics);

    }

    @Test
    public void maxConcurrentCallsValueCorrespondsToTheConfigValue() {
        // Given
        BulkheadRegistry bulkheadRegistry = BulkheadRegistry.ofDefaults();
        Bulkhead bulkhead = bulkheadRegistry.bulkhead("testName");
        BulkheadMetrics.ofBulkheadRegistry(bulkheadRegistry).bindTo(meterRegistry);

        // When getting the corresponding gauge
        Gauge gauge = meterRegistry.get("resilience4j.bulkhead.testName.max_allowed_concurrent_calls").gauge();

        // Then the value is the same as configured one
        assertThat(gauge.value()).isEqualTo(bulkhead.getBulkheadConfig().getMaxConcurrentCalls());
    }
}