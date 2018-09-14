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

import static io.github.resilience4j.micrometer.MetricUtils.getName;
import static io.github.resilience4j.retry.utils.MetricNames.DEFAULT_PREFIX;
import static io.github.resilience4j.retry.utils.MetricNames.FAILED_CALLS_WITHOUT_RETRY;
import static io.github.resilience4j.retry.utils.MetricNames.FAILED_CALLS_WITH_RETRY;
import static io.github.resilience4j.retry.utils.MetricNames.SUCCESSFUL_CALLS_WITHOUT_RETRY;
import static io.github.resilience4j.retry.utils.MetricNames.SUCCESSFUL_CALLS_WITH_RETRY;
import static java.util.Objects.requireNonNull;

import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

public class RetryMetrics implements MeterBinder {

    private final Iterable<Retry> retries;
    private final String prefix;

    private RetryMetrics(Iterable<Retry> retries) {
        this(retries, DEFAULT_PREFIX);
    }

    private RetryMetrics(Iterable<Retry> retries, String prefix) {
        this.retries = requireNonNull(retries);
        this.prefix = requireNonNull(prefix);
    }

    /**
     * Creates a new instance RetryMetrics {@link RetryMetrics} with
     * a {@link RateLimiterRegistry} as a source.
     *
     * @param retryRegistry the registry of retries
     *
     * @return a new RetryMetrics instance
     */
    public static RetryMetrics ofRetryRegistry(RetryRegistry retryRegistry) {
        return new RetryMetrics(retryRegistry.getAllRetries());
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Retry retry : retries) {
            final String name = retry.getName();
            Gauge.builder(getName(prefix, name, SUCCESSFUL_CALLS_WITHOUT_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt())
                    .register(registry);
            Gauge.builder(getName(prefix, name, SUCCESSFUL_CALLS_WITH_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt())
                    .register(registry);
            Gauge.builder(getName(prefix, name, FAILED_CALLS_WITHOUT_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt())
                    .register(registry);
            Gauge.builder(getName(prefix, name, FAILED_CALLS_WITH_RETRY), retry, (cb) -> cb.getMetrics().getNumberOfFailedCallsWithRetryAttempt())
                    .register(registry);
        }
    }
}
