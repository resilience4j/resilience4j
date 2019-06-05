/*
 * Copyright 2019 lespinsideg
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
package io.github.resilience4j.bulkhead.configure;

public class BulkheadConfigurationProperties extends io.github.resilience4j.common.bulkhead.configuration.BulkheadConfigurationProperties {
	/*
	This property gives you control over Bulkhead aspect application order.
	By default Bulkhead will be executed BEFORE CircuitBreaker and RateLimiter.
	By adjusting each aspect order from ConfigurationProperties
	you explicitly define aspects execution sequence.
	*/
	private int bulkheadAspectOrder = Integer.MAX_VALUE - 2;

	public int getBulkheadAspectOrder() {
		return bulkheadAspectOrder;
	}

	public void setBulkheadAspectOrder(int bulkheadAspectOrder) {
		this.bulkheadAspectOrder = bulkheadAspectOrder;
	}

}
