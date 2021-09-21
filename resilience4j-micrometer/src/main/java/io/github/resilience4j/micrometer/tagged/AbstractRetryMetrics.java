/*
 * Copyright 2019 Ingyu Hwang, Mahmoud Romeh
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

import io.github.resilience4j.retry.Retry;
import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

abstract class AbstractRetryMetrics extends AbstractMetrics {

    protected final RetryMetricNames names;

    protected AbstractRetryMetrics(RetryMetricNames names) {
        this.names = requireNonNull(names);
    }

    protected void addMetrics(MeterRegistry meterRegistry, Retry retry) {
        List<Tag> customTags = mapToTagsList(retry.getTags());
        registerMetrics(meterRegistry, retry, customTags);
    }

    private void registerMetrics(MeterRegistry meterRegistry, Retry retry, List<Tag> customTags) {
        // Remove previous meters before register
        removeMetrics(meterRegistry, retry.getName());

        Set<Meter.Id> idSet = new HashSet<>();
        idSet.add(FunctionCounter.builder(names.getCallsMetricName(), retry,
            rt -> rt.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
            .description("The number of successful calls without a retry attempt")
            .tag(TagNames.NAME, retry.getName())
            .tag(TagNames.KIND, "successful_without_retry")
            .tags(customTags)
            .register(meterRegistry).getId());
        idSet.add(FunctionCounter.builder(names.getCallsMetricName(), retry,
            rt -> rt.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt())
            .description("The number of successful calls after a retry attempt")
            .tag(TagNames.NAME, retry.getName())
            .tag(TagNames.KIND, "successful_with_retry")
            .tags(customTags)
            .register(meterRegistry).getId());
        idSet.add(FunctionCounter.builder(names.getCallsMetricName(), retry,
            rt -> rt.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
            .description("The number of failed calls without a retry attempt")
            .tag(TagNames.NAME, retry.getName())
            .tag(TagNames.KIND, "failed_without_retry")
            .tags(customTags)
            .register(meterRegistry).getId());
        idSet.add(FunctionCounter.builder(names.getCallsMetricName(), retry,
            rt -> rt.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
            .description("The number of failed calls after a retry attempt")
            .tag(TagNames.NAME, retry.getName())
            .tag(TagNames.KIND, "failed_with_retry")
            .tags(customTags)
            .register(meterRegistry).getId());
        meterIdMap.put(retry.getName(), idSet);
    }

}
