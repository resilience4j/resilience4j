/*
 * Copyright 2017 Robert Winkler
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
package io.github.resilience4j.circuitbreaker.configure;


import org.springframework.context.annotation.Configuration;

@Configuration
public class CircuitBreakerConfigurationProperties extends io.github.resilience4j.common.circuitbreaker.configuration.CircuitBreakerConfigurationProperties {
	// This property gives you control over CircuitBreaker aspect application order.
	// By default CircuitBreaker will be executed BEFORE RateLimiter.
	// By adjusting RateLimiterProperties.rateLimiterAspectOrder and CircuitBreakerProperties.circuitBreakerAspectOrder
	// you explicitly define aspects CircuitBreaker and RateLimiter execution sequence.
	private int circuitBreakerAspectOrder = Integer.MAX_VALUE - 1;

	public int getCircuitBreakerAspectOrder() {
		return circuitBreakerAspectOrder;
	}

	public void setCircuitBreakerAspectOrder(int circuitBreakerAspectOrder) {
		this.circuitBreakerAspectOrder = circuitBreakerAspectOrder;
	}



}
