/*
 * Copyright 2017 Bohdan Storozhuk
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
package io.github.resilience4j.ratelimiter.configure;

public class RateLimiterConfigurationProperties extends io.github.resilience4j.common.ratelimiter.configuration.RateLimiterConfigurationProperties {
	// This property gives you control over RateLimiter aspect application order.
	// Integer.MAX_VALUE means that RateLimiter aspect will be last one applied to any decorated bean.
	// It also means that by default RateLimiter will be executed AFTER CircuitBreaker.
	// Be adjusting RateLimiterProperties.rateLimiterAspectOrder and CircuitBreakerProperties.circuitBreakerAspectOrder
	// you explicitly define aspects CircuitBreaker and RateLimiter execution sequence.
	private int rateLimiterAspectOrder = Integer.MAX_VALUE;

	public int getRateLimiterAspectOrder() {
		return rateLimiterAspectOrder;
	}

	public void setRateLimiterAspectOrder(int rateLimiterAspectOrder) {
		this.rateLimiterAspectOrder = rateLimiterAspectOrder;
	}
}
