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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitAdapter;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;
import io.github.resilience4j.core.lang.NonNull;

/**
 * limit adapter based into moving average for the windows checks
 */
class MovingAverageLimitAdapter implements LimitAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(MovingAverageLimitAdapter.class);
	private static final double NANO_SCALE = 1_000_000_000d;
	private static final double MILLI_SCALE = 1_000_000d;
	private final double LOW_LATENCY_MUL;
	private final double CONCURRENCY_DROP_MUL;
	private final double initialMaxLatency;
	private final double desirableLatency;
	private volatile double currentMaxLatency;
	private volatile double currentAverageLatencyNanos;
	private final Consumer<BulkheadLimit> publishEventConsumer;
	// measurement window collections. They are !!!NOT THREAD SAFE!!!
	private final MovingAverageWindow adaptationWindow;
	private final MovingAverageWindow reconfigurationWindow;

	public MovingAverageLimitAdapter(@NonNull AdaptiveBulkheadConfig config, Consumer<BulkheadLimit> publishEventConsumer) {
		initialMaxLatency = config.getMaxAcceptableRequestLatency();
		desirableLatency = config.getDesirableOperationLatency();
		LOW_LATENCY_MUL = config.getLowLatencyMultiplier();
		CONCURRENCY_DROP_MUL = config.getConcurrencyDropMultiplier();
		currentMaxLatency = min(config.getDesirableOperationLatency() * 1.2d, config.getMaxAcceptableRequestLatency());
		int adaptationWindowSize = (int) ceil(config.getWindowForAdaptation().getSeconds() * config.getDesirableAverageThroughput());
		int reconfigurationWindowSize = (int) ceil((double) config.getWindowForReconfiguration().getSeconds() / config.getWindowForAdaptation().getSeconds());
		long initialLatencyInNanos = (long) (config.getDesirableOperationLatency() * NANO_SCALE);
		adaptationWindow = new MovingAverageWindow(adaptationWindowSize, initialLatencyInNanos);
		reconfigurationWindow = new MovingAverageWindow(reconfigurationWindowSize, initialLatencyInNanos);
		this.publishEventConsumer = publishEventConsumer;
	}

	@Override
	public void adaptLimitIfAny(@NonNull Bulkhead bulkhead, @NonNull Duration callTime) {
		boolean endOfAdaptationWindow = adaptationWindow.measure(callTime.toNanos());
		if (LOG.isDebugEnabled()) {
			LOG.debug("end of adoption window is {}", endOfAdaptationWindow);
		}
		if (endOfAdaptationWindow) {
			double averageLatencyNanos = adaptConcurrencyLevel(bulkhead);
			if (LOG.isDebugEnabled()) {
				LOG.debug("new averageLatencyNanos is {}", averageLatencyNanos);
			}
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

	@Override
	public Consumer<BulkheadLimit> bulkheadLimitConsumer() {
		return publishEventConsumer;
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
		if (LOG.isDebugEnabled()) {
			LOG.debug("new currentMaxLatency is {}", currentMaxLatency);
		}
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
		if (LOG.isDebugEnabled()) {
			LOG.debug("new averageLatencySeconds is {}", averageLatencySeconds);
		}
		long waitTimeMillis = (long) (max(0d, desirableLatency - averageLatencySeconds) * MILLI_SCALE);
		if (LOG.isDebugEnabled()) {
			LOG.debug("new waitTimeMillis is {}", waitTimeMillis);
		}
		adoptLimit(bulkhead, averageLatencySeconds, waitTimeMillis);
		return averageLatencyNanos;
	}

	/**
	 * adopt the limit based into the new calculated average
	 *
	 * @param bulkhead              the target semaphore bulkhead
	 * @param averageLatencySeconds calculated average latency
	 * @param waitTimeMillis        new wait time
	 */
	private void adoptLimit(Bulkhead bulkhead, double averageLatencySeconds, long waitTimeMillis) {
		if (averageLatencySeconds < currentMaxLatency) {
			final int newMaxConcurrentCalls = bulkhead.getBulkheadConfig().getMaxConcurrentCalls() + 1;
			if (LOG.isDebugEnabled()) {
				LOG.debug("increasing bulkhead limit by increasing the max concurrent calls for {}", newMaxConcurrentCalls);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(newMaxConcurrentCalls)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			bulkheadLimitConsumer().accept(new BulkheadOnLimitIncreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(averageLatencySeconds, waitTimeMillis, newMaxConcurrentCalls)));
		} else {
			final int newMaxConcurrentCalls = max((int) (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() * CONCURRENCY_DROP_MUL), 1);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Dropping the bulkhead limit with new max concurrent calls {}", newMaxConcurrentCalls);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(newMaxConcurrentCalls)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			bulkheadLimitConsumer().accept(new BulkheadOnLimitDecreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(averageLatencySeconds, waitTimeMillis, newMaxConcurrentCalls)));
		}
	}


	private Map<String, String> eventData(double averageLatencySeconds, long waitTimeMillis, int newMaxConcurrentCalls) {
		Map<String, String> eventData = new HashMap<>();
		eventData.put("averageLatencySeconds", String.valueOf(averageLatencySeconds));
		eventData.put("newMaxConcurrentCalls", String.valueOf(newMaxConcurrentCalls));
		eventData.put("newWaitTimeMillis", String.valueOf(waitTimeMillis));
		eventData.put("currentMaxLatency", String.valueOf(currentMaxLatency));
		return eventData;
	}

}
