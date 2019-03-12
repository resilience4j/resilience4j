/*
 * Copyright 201o Mahmoud Romeh
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import io.github.resilience4j.retry.AsyncRetryRegistry;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class AsyncRetryMetricsTest {

	private MeterRegistry meterRegistry;

	@Before
	public void setUp() throws Exception {
		meterRegistry = new SimpleMeterRegistry();
	}

	@Test
	public void shouldRegisterMetrics() {
		AsyncRetryRegistry retryRegistry = AsyncRetryRegistry.ofDefaults();
		retryRegistry.retry("testName");

		AsyncRetryMetrics retryMetrics = AsyncRetryMetrics.ofRetryRegistry(retryRegistry);
		retryMetrics.bindTo(meterRegistry);

		final List<String> metricNames = meterRegistry.getMeters()
				.stream()
				.map(Meter::getId)
				.map(Meter.Id::getName)
				.collect(Collectors.toList());

		final List<String> expectedMetrics = newArrayList(
				"resilience4j.asyncRetry.testName.successful_calls_with_retry",
				"resilience4j.asyncRetry.testName.failed_calls_with_retry",
				"resilience4j.asyncRetry.testName.successful_calls_without_retry",
				"resilience4j.asyncRetry.testName.failed_calls_without_retry");
		assertThat(metricNames).hasSameElementsAs(expectedMetrics);

	}


}