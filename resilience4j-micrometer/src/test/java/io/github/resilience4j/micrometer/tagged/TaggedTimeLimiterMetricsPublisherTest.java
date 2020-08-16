/*
 * Copyright 2019 Ingyu Hwang
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
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static io.github.resilience4j.micrometer.tagged.MetricsTestHelper.findMeterByKindAndNameTags;
import static io.github.resilience4j.micrometer.tagged.TimeLimiterMetricNames.DEFAULT_TIME_LIMITER_CALLS;
import static org.assertj.core.api.Assertions.assertThat;

public class TaggedTimeLimiterMetricsPublisherTest {

    private MeterRegistry meterRegistry;
    private TimeLimiter timeLimiter;
    private TimeLimiterRegistry timeLimiterRegistry;
    private TaggedTimeLimiterMetricsPublisher taggedTimeLimiterMetricsPublisher;

    @Before
    public void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        taggedTimeLimiterMetricsPublisher = new TaggedTimeLimiterMetricsPublisher(meterRegistry);
        timeLimiterRegistry = TimeLimiterRegistry
            .of(TimeLimiterConfig.ofDefaults(), taggedTimeLimiterMetricsPublisher);

        timeLimiter = timeLimiterRegistry.timeLimiter("backendA");
    }

    @Test
    public void shouldAddMetricsForANewlyCreatedTimeLimiter() {
        TimeLimiter newTimeLimiter = timeLimiterRegistry.timeLimiter("backendB");
        newTimeLimiter.onSuccess();

        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap)
            .containsKeys("backendA", "backendB");
        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap.get("backendA")).hasSize(3);
        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap.get("backendB")).hasSize(3);

        assertThat(meterRegistry.getMeters()).hasSize(6);

        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();

        Optional<Counter> successful = findMeterByKindAndNameTags(counters, "successful",
            newTimeLimiter.getName());
        assertThat(successful).map(Counter::count).contains(1d);
    }

    @Test
    public void shouldAddCustomTags() {
        TimeLimiter timeLimiterF = timeLimiterRegistry.timeLimiter("backendF", Map.of("key1", "value1"));
        timeLimiterF.onSuccess();
        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap).containsKeys("backendA", "backendF");
        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap.get("backendF")).hasSize(3);

        assertThat(meterRegistry.getMeters()).hasSize(6);
        RequiredSearch match = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).tags("key1", "value1");
        assertThat(match).isNotNull();
    }

    @Test
    public void shouldRemovedMetricsForRemovedRetry() {
        assertThat(meterRegistry.getMeters()).hasSize(3);

        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap).containsKeys("backendA");
        timeLimiterRegistry.remove("backendA");

        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap).isEmpty();

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
        assertThat(after.getId().getTag(TagNames.NAME))
            .isEqualTo(TimeLimiter.ofDefaults().getName());
    }

    @Test
    public void successfulCounterIsRegistered() {
        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        timeLimiter.onSuccess();

        Optional<Counter> successful = findMeterByKindAndNameTags(counters, "successful",
            timeLimiter.getName());
        assertThat(successful).map(Counter::count).contains(1d);
    }

    @Test
    public void failedCounterIsRegistered() {
        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        timeLimiter.onError(new RuntimeException());

        Optional<Counter> failed = findMeterByKindAndNameTags(counters, "failed",
            timeLimiter.getName());
        assertThat(failed).map(Counter::count).contains(1d);
    }

    @Test
    public void timoutCounterIsRegistered() {
        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        timeLimiter.onError(new TimeoutException());

        Optional<Counter> timeout = findMeterByKindAndNameTags(counters, "timeout",
            timeLimiter.getName());
        assertThat(timeout).map(Counter::count).contains(1d);
    }

    @Test
    public void customMetricNamesGetApplied() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        TaggedTimeLimiterMetricsPublisher taggedTimeLimiterMetricsPublisher = new TaggedTimeLimiterMetricsPublisher(
            TimeLimiterMetricNames.custom()
                .callsMetricName("custom_calls")
                .build(), meterRegistry);

        TimeLimiterRegistry timeLimiterRegistry = TimeLimiterRegistry
            .of(TimeLimiterConfig.ofDefaults(), taggedTimeLimiterMetricsPublisher);
        timeLimiterRegistry.timeLimiter("backendA");

        Set<String> metricNames = meterRegistry.getMeters()
            .stream()
            .map(Meter::getId)
            .map(Meter.Id::getName)
            .collect(Collectors.toSet());

        assertThat(metricNames).hasSameElementsAs(Arrays.asList(
            "custom_calls"
        ));
    }

    @Test
    public void testReplaceNewMeter(){
        TimeLimiter oldOne = TimeLimiter.of("backendC", TimeLimiterConfig.ofDefaults());
        // add meters of old
        taggedTimeLimiterMetricsPublisher.addMetrics(meterRegistry, oldOne);
        // one success call
        oldOne.onSuccess();

        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap.get("backendC")).hasSize(3);
        Collection<Counter> counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        Optional<Counter> successful = findMeterByKindAndNameTags(counters, "successful",
            oldOne.getName());
        assertThat(successful).map(Counter::count).contains(1d);

        TimeLimiter newOne = TimeLimiter.of("backendC", TimeLimiterConfig.ofDefaults());

        // add meters of new
        taggedTimeLimiterMetricsPublisher.addMetrics(meterRegistry, newOne);
        // three success call
        newOne.onSuccess();
        newOne.onSuccess();
        newOne.onSuccess();

        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap).containsKeys("backendC");
        assertThat(taggedTimeLimiterMetricsPublisher.meterIdMap.get("backendC")).hasSize(3);
        counters = meterRegistry.get(DEFAULT_TIME_LIMITER_CALLS).counters();
        successful = findMeterByKindAndNameTags(counters, "successful",
            newOne.getName());
        assertThat(successful).map(Counter::count).contains(3d);
    }

}