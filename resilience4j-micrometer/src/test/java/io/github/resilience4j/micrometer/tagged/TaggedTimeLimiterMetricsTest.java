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

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findCounterByNamesTag;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedTimeLimiterMetricsTest {

    private static final String DEFAULT_PREFIX = "resilience4j.timelimiter";
    private static final String SUCCESSFUL = DEFAULT_PREFIX + ".successful";
    private static final String FAILED = DEFAULT_PREFIX + ".failed";
    private static final String TIMEOUT = DEFAULT_PREFIX + ".timeout";

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

        assertThat(taggedTimeLimiterMetrics.meterIdMap).containsKeys("backendA", "backendB");
        assertThat(taggedTimeLimiterMetrics.meterIdMap.get("backendA")).hasSize(3);
        assertThat(taggedTimeLimiterMetrics.meterIdMap.get("backendB")).hasSize(3);

        assertThat(meterRegistry.getMeters()).hasSize(6);

        Collection<Counter> counters = meterRegistry.get(SUCCESSFUL).counters();

        Optional<Counter> successful = findCounterByNamesTag(counters, newTimeLimiter.getName());
        assertThat(successful).map(Counter::count).contains(0d);
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
        Counter before = meterRegistry.get(SUCCESSFUL).counter();
        assertThat(before).isNotNull();
        assertThat(before.count()).isEqualTo(0);
        assertThat(before.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());

        timeLimiterRegistry.replace(timeLimiter.getName(), TimeLimiter.ofDefaults());

        Counter after = meterRegistry.get(SUCCESSFUL).counter();
        assertThat(after).isNotNull();
        assertThat(after.count()).isEqualTo(0);
        assertThat(after.getId().getTag(TagNames.NAME)).isEqualTo(TimeLimiter.ofDefaults().getName());
    }

    @Test
    public void successfulCounterIsRegistered() {
        Counter successful = meterRegistry.get(SUCCESSFUL).counter();

        assertThat(successful).isNotNull();
        assertThat(successful.count()).isEqualTo(0);
        assertThat(successful.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());
    }

    @Test
    public void failedCounterIsRegistered() {
        Counter failed = meterRegistry.get(FAILED).counter();

        assertThat(failed).isNotNull();
        assertThat(failed.count()).isEqualTo(0);
        assertThat(failed.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());
    }

    @Test
    public void timoutCounterIsRegistered() {
        Counter timout = meterRegistry.get(TIMEOUT).counter();

        assertThat(timout).isNotNull();
        assertThat(timout.count()).isEqualTo(0);
        assertThat(timout.getId().getTag(TagNames.NAME)).isEqualTo(timeLimiter.getName());
    }

    @Test
    public void successfulCounterIsIncremented() throws Exception {
        Callable<String> decoratedSupplier = timeLimiter.decorateFutureSupplier(() ->
                CompletableFuture.completedFuture("Hello world"));

        decoratedSupplier.call();
        decoratedSupplier.call();

        assertThat(meterRegistry.get(SUCCESSFUL).counter().count()).isEqualTo(2);
        assertThat(meterRegistry.get(FAILED).counter().count()).isEqualTo(0);
        assertThat(meterRegistry.get(TIMEOUT).counter().count()).isEqualTo(0);
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry.ofDefaults();
        timeLimiterRegistry.timeLimiter("backendA");
        TaggedTimeLimiterMetrics.ofTimeLimiterRegistry(
                TaggedTimeLimiterMetrics.MetricNames.custom()
                        .successfulMetricName("custom_successful")
                        .failedMetricName("custom_failed")
                        .timeoutMetricName("custom_timeout")
                        .build(),
                timeLimiterRegistry
        ).bindTo(meterRegistry);

        Set<String> metricNames = meterRegistry.getMeters()
                .stream()
                .map(Meter::getId)
                .map(Meter.Id::getName)
                .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Arrays.asList(
                "custom_successful",
                "custom_failed",
                "custom_timeout"
        ));
    }
}
