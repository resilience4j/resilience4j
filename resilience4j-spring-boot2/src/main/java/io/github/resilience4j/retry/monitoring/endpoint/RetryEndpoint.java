/*
 * Copyright 2019 Mahmoud Romeh
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
package io.github.resilience4j.retry.monitoring.endpoint;


import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.github.resilience4j.retry.AsyncRetry;
import io.github.resilience4j.retry.AsyncRetryRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;


/**
 * REST API endpoint to retrieve all configured retries
 */
@Endpoint(id = "retries")
public class RetryEndpoint {

	private final RetryRegistry retryRegistry;
	private final AsyncRetryRegistry asyncRetryRegistry;

	public RetryEndpoint(RetryRegistry retryRegistry, AsyncRetryRegistry asyncRetryRegistry) {
		this.retryRegistry = retryRegistry;
		this.asyncRetryRegistry = asyncRetryRegistry;
	}

	@ReadOperation
	public RetryEndpointResponse getAllRetries() {
		Set<String> retriesSet = retryRegistry.getAllRetries()
				.map(Retry::getName).sorted().toJavaSet();
		retriesSet.addAll(asyncRetryRegistry.getAllRetries()
				.map(AsyncRetry::getName).sorted().toJavaSet());

		List<String> retries = new ArrayList<>();
		retries.addAll(retriesSet);
		return new RetryEndpointResponse(retries);
	}
}
