/*
 *
 *  Copyright 2019: Bohdan Storozhuk, Mahmoud Romeh
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
package io.github.resilience4j.bulkhead.adaptive.internal;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.time.Duration;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitAdapter;
import io.github.resilience4j.core.lang.NonNull;

/**
 * limit adapter based into moving average for the windows checks
 */
public class MovingAverageLimitAdapter implements LimitAdapter {
	private static final double NANO_SCALE = 1_000_000_000d;
	private static final double MILLI_SCALE = 1_000_000d;
	private final double LOW_LATENCY_MUL;
	private final double CONCURRENCY_DROP_MUL;
	private final double initialMaxLatency;
	private final double desirableLatency;
	private volatile double currentMaxLatency;
	private volatile double currentAverageLatencyNanos;
	// measurement window collections. They are !!!NOT THREAD SAFE!!!
	private MovingAverageWindow adaptationWindow;
	private MovingAverageWindow reconfigurationWindow;

	public MovingAverageLimitAdapter(@NonNull AdaptiveBulkheadConfig config) {
		initialMaxLatency = config.getMaxAcceptableRequestLatency();
		desirableLatency = config.getDesirableOperationLatency();
		LOW_LATENCY_MUL = config.getLowLatencyMultiplier();
		CONCURRENCY_DROP_MUL = config.getConcurrencyDropMultiplier();
		currentMaxLatency = min(config.getDesirableOperationLatency() * 1.2d, config.getMaxAcceptableRequestLatency());
		int adaptationWindowSize = (int) ceil(config.getWindowForAdaptation().getSeconds() * config.getDesirableAverageThroughput());
		int reconfigurationWindowSize = (int) ceil(config.getWindowForReconfiguration().getSeconds() / config.getWindowForAdaptation().getSeconds());
		long initialLatencyInNanos = (long) (config.getDesirableOperationLatency() * NANO_SCALE);
		adaptationWindow = new MovingAverageWindow(adaptationWindowSize, initialLatencyInNanos);
		reconfigurationWindow = new MovingAverageWindow(reconfigurationWindowSize, initialLatencyInNanos);
	}

	@Override
	public void adaptLimitIfAny(@NonNull Bulkhead bulkhead, @NonNull Duration callTime) {
		boolean endOfAdaptationWindow = adaptationWindow.measure(callTime.toNanos());
		if (endOfAdaptationWindow) {
			double averageLatencyNanos = adaptConcurrencyLevel(bulkhead);
			currentAverageLatencyNanos = averageLatencyNanos;
			boolean endOfReconfigurationWindow = reconfigurationWindow.measure(averageLatencyNanos);
			if (endOfReconfigurationWindow) {
				adjustConfiguration(averageLatencyNanos);
			}
		}
	}

	@Override
	public double getMaxLatencyMillis() {
		return currentMaxLatency * 1000;
	}

	@Override
	public double getAverageLatencyMillis() {
		return currentAverageLatencyNanos / 1e6;
	}

	/**
	 * adjust configuration widows
	 *
	 * @param averageLatencyNanos the average latency for the service call
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField")
	private void adjustConfiguration(double averageLatencyNanos) {
		double standardDeviationNanos = reconfigurationWindow.standardDeviation();
		// we can change latency only between desirableLatency * LOW_LATENCY_MUL and initialMaxLatency
		currentMaxLatency = min((averageLatencyNanos + standardDeviationNanos) / NANO_SCALE, initialMaxLatency);
		currentMaxLatency = max(currentMaxLatency, desirableLatency * LOW_LATENCY_MUL);
	}

	/**
	 * adjust concurrency limit throw adjust concurrency windows if any
	 *
	 * @param bulkhead the configured semaphore bulkhead inside {@link io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkhead}
	 * @return the new average Latency in Nanos
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField")
	private double adaptConcurrencyLevel(Bulkhead bulkhead) {
		double averageLatencyNanos = adaptationWindow.average();
		double averageLatencySeconds = (averageLatencyNanos) / NANO_SCALE;
		long waitTimeMillis = (long) (max(0d, desirableLatency - averageLatencySeconds) * MILLI_SCALE);
		if (averageLatencySeconds < currentMaxLatency) {
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(bulkhead.getBulkheadConfig().getMaxConcurrentCalls() + 1)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
		} else {
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(max((int) (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() * CONCURRENCY_DROP_MUL), 1))
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
		}
		return averageLatencyNanos;
	}
}
