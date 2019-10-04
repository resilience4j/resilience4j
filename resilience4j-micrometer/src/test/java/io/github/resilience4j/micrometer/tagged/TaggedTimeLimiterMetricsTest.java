/*
 * Copyright 2019 authors
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
package io.github.resilience4j.micrometer.tagged;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.AbstractTimeLimiterMetrics.MetricNames.DEFAULT_TIME_LIMITER_CALLS;
import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findCounterByKindAndNameTags;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedTimeLimiterMetricsTest {

    private MeterRegistry meterRegistry;
    private TimeLimiter timeLimiter;
    private TimeLimiterRegistry timeLimiterRegistry;
    private TaggedTimeLimiterMetrics taggedTimeLimiterMetrics;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();

        timeLimiter = timeLimiterRegistry.timeLimiter("backendA");
        taggedTimeLimiterMetrics = TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(timeLimiterRegistry);
        taggedTimeLimiterMetrics.bindTo(meterRegistry);
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedTimeLimiter() {
        TimeLimiter newTimeLimiter = timeLimiterRegistry.timeLimiter("backendB");
        newTimeLimiter.onSuccess();

        assertThat(taggedTimeLimiterMetrics.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedTimeLimiterMetrics.meterIdMap.get("backendA")).hasSize(3);
        assertThat(taggedTimeLimiterMetrics.meterIdMap.get("backendB")).hasSize(3);

        assertThat(meterRegistry.getMeters()).hasSize(6);

        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();

        Optional<Counter> successful = findCounterByKindAndNameTags(counters, "successful", newTimeLimiter.getName());
        assertThat(successful).map(Counter::count).contains(1d);
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        assertThat(meterRegistry.getMeters()).hasSize(3);

        assertThat(taggedTimeLimiterMetrics.meterIdMap).containsKeys("backendA");
        timeLimiterRegistry.remove("backendA");

        assertThat(taggedTimeLimiterMetrics.meterIdMap).isEmpty();

        assertThat(meterRegistry.getMeters()).isEmpty();
    }

    @Test
    public void shouldReplaceMetrics() {
        Counter before = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counter();
        assertThat(before).isNotNull();
        assertThat(before.count()).isEqualTo(0);
        assertThat(before.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());

        timeLimiterRegistry.replace(timeLimiter.getName(), TimeLimiter.ofDefaults());

        Counter after = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counter();
        assertThat(after).isNotNull();
        assertThat(after.count()).isEqualTo(0);
        assertThat(after.getId().getTag(TagNames.NAME)).isEqualTo(TimeLimiter.ofDefaults().getName());
    }

    @Test
    public void successfulCounterIsRegistered() {
        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        timeLimiter.onSuccess();

        Optional<Counter> successful = findCounterByKindAndNameTags(counters, "successful", timeLimiter.getName());
        assertThat(successful).map(Counter::count).contains(1d);
    }

    @Test
    public void failedCounterIsRegistered() {
        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        timeLimiter.onError(new RuntimeException());

        Optional<Counter> failed = findCounterByKindAndNameTags(counters, "failed", timeLimiter.getName());
        assertThat(failed).map(Counter::count).contains(1d);
    }

    @Test
    public void timoutCounterIsRegistered() {
        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        timeLimiter.onError(new TimeoutException());

        Optional<Counter> timeout = findCounterByKindAndNameTags(counters, "timeout", timeLimiter.getName());
        assertThat(timeout).map(Counter::count).contains(1d);
    }
    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiterRegistry.timeLimiter("backendA");
        TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(
                TaggedTimeLimiterMetrics.MetricNames.custom()
                        .callsMetricName("custom_calls")
                        .build(),
                timeLimiterRegistry
        ).bindTo(meterRegistry);

        Set<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Arrays.asList(
                "custom_calls"
        ));
    }
}
