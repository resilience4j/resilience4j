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

package io.github.resilience4j.metrics.publisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import io.github.resilience4j.retry.Retry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.retry.utils.MetricNames.*;
import static java.util.Objects.requireNonNull;

public class RetryMetricsPublisher extends AbstractMetricsPublisher<Retry> {

    private final String prefix;

    public RetryMetricsPublisher() {
        this(DEFAULT_PREFIX, new MetricRegistry());
    }

    public RetryMetricsPublisher(MetricRegistry metricRegistry) {
        this(DEFAULT_PREFIX, metricRegistry);
    }

    public RetryMetricsPublisher(String prefix, MetricRegistry metricRegistry) {
        super(metricRegistry);
        this.prefix = requireNonNull(prefix);
    }

    @Override
    public void publishMetrics(Retry retry) {
        String name = retry.getName();

        String successfulWithoutRetry = name(prefix, name, SUCCESSFUL_CALLS_WITHOUT_RETRY);
        String successfulWithRetry = name(prefix, name, SUCCESSFUL_CALLS_WITH_RETRY);
        String failedWithoutRetry = name(prefix, name, FAILED_CALLS_WITHOUT_RETRY);
        String failedWithRetry = name(prefix, name, FAILED_CALLS_WITH_RETRY);

        metricRegistry.register(successfulWithoutRetry,
                (Gauge<Long>) () -> retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
        metricRegistry.register(successfulWithRetry,
                (Gauge<Long>) () -> retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
        metricRegistry.register(failedWithoutRetry,
                (Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
        metricRegistry.register(failedWithRetry,
                (Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());

        List<String> metricNames = Arrays.asList(successfulWithoutRetry, successfulWithRetry, failedWithoutRetry, failedWithRetry);
        metricsNameMap.put(name, new HashSet<>(metricNames));
    }

    @Override
    public void removeMetrics(Retry retry) {
        removeMetrics(retry.getName());
    }
}
