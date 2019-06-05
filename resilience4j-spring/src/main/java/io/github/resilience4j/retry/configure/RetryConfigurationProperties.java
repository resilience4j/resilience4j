package io.github.resilience4j.retry.configure;
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

/**
 * Main spring properties for retry configuration
 */
public class RetryConfigurationProperties extends io.github.resilience4j.common.retry.configuration.RetryConfigurationProperties {
	/*  This property gives you control over Retry aspect application order.
		By default Retry will be executed BEFORE Circuit breaker, rateLimiter and bulkhead.
		By adjusting each aspect order from ConfigurationProperties
		you explicitly define aspects execution sequence.
	*/
	private int retryAspectOrder = Integer.MAX_VALUE - 3;

	/**
	 * @return spring aspect apply order
	 */
	public int getRetryAspectOrder() {
		return retryAspectOrder;
	}

	/**
	 * @param retryAspectOrder spring the aspect apply order
	 */
	public void setRetryAspectOrder(int retryAspectOrder) {
		this.retryAspectOrder = retryAspectOrder;
	}

}
