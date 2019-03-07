/*
 * Copyright 209 Mahmoud Romeh
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
package io.github.resilience4j.retry.monitoring.health;

import java.util.Optional;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import io.github.resilience4j.retry.Retry;

/**
 * A Spring Boot health indicators which adds the state of a Retry and it's metrics to the health endpoints
 */
public class RetryHealthIndicator implements HealthIndicator {

	private static final String FAILED_CALLS_WITH_RETRY = "failedCallsWithRetry";
	private static final String FAILED_CALLS_WITHOUT_RETRY = "failedCallsWithoutRetry";
	private static final String SUCCESS_CALLS = "successCalls";
	private static final String SUCCESS_CALLS_WITH_RETRY = "successCallsWithRetry";

	private final Retry retry;

	public RetryHealthIndicator(Retry retry) {
		this.retry = retry;
	}

	@Override
	public Health health() {
		return Optional.of(retry)
				.map(this::mapBackendMonitorState)
				.orElse(Health.up().build());
	}

	private Health mapBackendMonitorState(Retry retry) {
		Retry.Metrics metrics = retry.getMetrics();
		return Health.up().withDetail(FAILED_CALLS_WITHOUT_RETRY, metrics.getNumberOfFailedCallsWithoutRetryAttempt() + "%")
				.withDetail(FAILED_CALLS_WITH_RETRY, metrics.getNumberOfFailedCallsWithRetryAttempt())
				.withDetail(SUCCESS_CALLS, metrics.getNumberOfSuccessfulCallsWithoutRetryAttempt())
				.withDetail(SUCCESS_CALLS_WITH_RETRY, metrics.getNumberOfSuccessfulCallsWithRetryAttempt()).build();
	}
}
