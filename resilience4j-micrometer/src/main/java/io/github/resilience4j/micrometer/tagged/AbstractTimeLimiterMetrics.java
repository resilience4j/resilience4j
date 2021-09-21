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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static java.util.Objects.requireNonNull;

abstract class AbstractTimeLimiterMetrics extends AbstractMetrics {

    private static final String KIND_FAILED = "failed";
    private static final String KIND_SUCCESSFUL = "successful";
    private static final String KIND_TIMEOUT = "timeout";

    protected final TimeLimiterMetricNames names;

    protected AbstractTimeLimiterMetrics(TimeLimiterMetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, TimeLimiter timeLimiter) {
        List<Tag> customTags = mapToTagsList(timeLimiter.getTags());
        registerMetrics(meterRegistry, timeLimiter, customTags);
    }

    protected void registerMetrics(MeterRegistry meterRegistry, TimeLimiter timeLimiter, List<Tag> customTags) {
        // Remove previous meters before register
        removeMetrics(meterRegistry, timeLimiter.getName());

        Counter successes = Counter.builder(names.getCallsMetricName())
            .description("The number of successful calls")
            .tag(TagNames.NAME, timeLimiter.getName())
            .tag(TagNames.KIND, KIND_SUCCESSFUL)
            .tags(customTags)
            .register(meterRegistry);
        Counter failures = Counter.builder(names.getCallsMetricName())
            .description("The number of failed calls")
            .tag(TagNames.NAME, timeLimiter.getName())
            .tag(TagNames.KIND, KIND_FAILED)
            .tags(customTags)
            .register(meterRegistry);
        Counter timeouts = Counter.builder(names.getCallsMetricName())
            .description("The number of timed out calls")
            .tag(TagNames.NAME, timeLimiter.getName())
            .tag(TagNames.KIND, KIND_TIMEOUT)
            .tags(customTags)
            .register(meterRegistry);

        timeLimiter.getEventPublisher()
            .onSuccess(event -> successes.increment())
            .onError(event -> failures.increment())
            .onTimeout(event -> timeouts.increment());

        List<Meter.Id> ids = Arrays.asList(successes.getId(), failures.getId(), timeouts.getId());
        meterIdMap.put(timeLimiter.getName(), new HashSet<>(ids));
    }

}
