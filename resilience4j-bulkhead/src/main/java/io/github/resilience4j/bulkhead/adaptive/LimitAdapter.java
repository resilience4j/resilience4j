/*
 *
 *  Copyright 2019: Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive;

import java.time.Duration;
import java.util.function.Consumer;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.core.lang.NonNull;


/**
 * Limit adopter interface for adoptive bullhead {@link AdaptiveBulkhead}
 *
 * @author romeh
 */
public interface LimitAdapter {

	/**
	 * adapt the concurrency limit of the bulkhead based into the implemented logic
	 * by updating the bulkhead concurrent limit through {@link Bulkhead#changeConfig(BulkheadConfig)} ()} if the limiter algorithm trigger a need for an update
	 *
	 * @param bulkhead the created semaphore bullhead with contains its own configuration via {@link Bulkhead#getBulkheadConfig()}
	 * @param callTime the protected service by bulkhead call total execution time
	 */
	void adaptLimitIfAny(@NonNull Bulkhead bulkhead, @NonNull Duration callTime);

	/**
	 * @return max latency time in milliseconds
	 */
	double getMaxLatencyMillis();

	/**
	 * @return average latency in milliseconds
	 */
	double getAverageLatencyMillis();

	/**
	 * @return the BulkheadLimit consumer
	 */
	Consumer<BulkheadLimit> bulkheadLimitConsumer();
}
