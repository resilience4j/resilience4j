/*
 *
 *  Copyright 2018 David Rusek
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

import static com.codahale.metrics.MetricRegistry.name;
import static io.github.resilience4j.retry.utils.MetricNames.DEFAULT_PREFIX_ASYNC;
import static io.github.resilience4j.retry.utils.MetricNames.FAILED_CALLS_WITHOUT_RETRY;
import static io.github.resilience4j.retry.utils.MetricNames.FAILED_CALLS_WITH_RETRY;
import static io.github.resilience4j.retry.utils.MetricNames.SUCCESSFUL_CALLS_WITHOUT_RETRY;
import static io.github.resilience4j.retry.utils.MetricNames.SUCCESSFUL_CALLS_WITH_RETRY;
import static java.util.Objects.requireNonNull;

import java.util.Map;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricSet;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.AsyncRetryRegistry;
import io.vavr.collection.Array;

/**
 * An adapter which exports {@link AsyncRetry.Metrics} as Dropwizard Metrics Gauges.
 */
public class AsyncRetryMetrics implements MetricSet {

	private final MetricRegistry metricRegistry = new MetricRegistry();

	private AsyncRetryMetrics(Iterable<AsyncRetry> retries) {
		this(DEFAULT_PREFIX_ASYNC, retries);
	}

	private AsyncRetryMetrics(String prefix, Iterable<AsyncRetry> retries) {
		requireNonNull(prefix);
		requireNonNull(retries);
		retries.forEach(retry -> {
			String name = retry.getName();

			metricRegistry.register(name(prefix, name, SUCCESSFUL_CALLS_WITHOUT_RETRY),
					(Gauge<Long>) () -> retry.getMetrics().getNumberOfSuccessfulCallsWithoutRetryAttempt());
			metricRegistry.register(name(prefix, name, SUCCESSFUL_CALLS_WITH_RETRY),
					(Gauge<Long>) () -> retry.getMetrics().getNumberOfSuccessfulCallsWithRetryAttempt());
			metricRegistry.register(name(prefix, name, FAILED_CALLS_WITHOUT_RETRY),
					(Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithoutRetryAttempt());
			metricRegistry.register(name(prefix, name, FAILED_CALLS_WITH_RETRY),
					(Gauge<Long>) () -> retry.getMetrics().getNumberOfFailedCallsWithRetryAttempt());
		});
	}

	public static AsyncRetryMetrics ofAsyncRetryRegistry(String prefix, AsyncRetryRegistry retryRegistry) {
		return new AsyncRetryMetrics(prefix, retryRegistry.getAllRetries());
	}

	public static AsyncRetryMetrics ofAsyncRetryRegistry(AsyncRetryRegistry retryRegistry) {
		return new AsyncRetryMetrics(retryRegistry.getAllRetries());
	}

	public static AsyncRetryMetrics ofIterable(String prefix, Iterable<AsyncRetry> retries) {
		return new AsyncRetryMetrics(prefix, retries);
	}

	public static AsyncRetryMetrics ofIterable(Iterable<AsyncRetry> retries) {
		return new AsyncRetryMetrics(retries);
	}

	public static AsyncRetryMetrics ofAsyncRetry(AsyncRetry retry) {
		return new AsyncRetryMetrics(Array.of(retry));
	}

	@Override
	public Map<String, Metric> getMetrics() {
		return metricRegistry.getMetrics();
	}
}
