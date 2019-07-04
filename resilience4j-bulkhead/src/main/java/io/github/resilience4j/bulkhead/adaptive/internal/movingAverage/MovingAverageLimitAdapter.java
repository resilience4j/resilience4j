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
package io.github.resilience4j.bulkhead.adaptive.internal.movingAverage;

import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;

import java.time.Duration;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.AbstractLimiterAdapter;
import io.github.resilience4j.bulkhead.adaptive.internal.config.MovingAverageConfig;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.core.lang.NonNull;

/**
 * limit adapter based into moving average for the windows checks
 */
public class MovingAverageLimitAdapter extends AbstractLimiterAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(MovingAverageLimitAdapter.class);

	private final double LOW_LATENCY_MUL;
	private final double initialMaxLatency;
	private final double desirableLatency;
	private volatile double currentAverageLatencyNanos;
	// measurement window collections. They are !!!NOT THREAD SAFE!!!
	private final MovingAverageWindow adaptationWindow;
	private final MovingAverageWindow reconfigurationWindow;

	public MovingAverageLimitAdapter(@NonNull AdaptiveBulkheadConfig<MovingAverageConfig> config, Consumer<BulkheadLimit> publishEventConsumer) {
		super(config.getConfiguration().getConcurrencyDropMultiplier(), min(config.getConfiguration().getDesirableOperationLatency() * 1.2d, config.getConfiguration().getMaxAcceptableRequestLatency()), publishEventConsumer);
		initialMaxLatency = config.getConfiguration().getMaxAcceptableRequestLatency();
		desirableLatency = config.getConfiguration().getDesirableOperationLatency();
		LOW_LATENCY_MUL = config.getConfiguration().getLowLatencyMultiplier();
		currentMaxLatency = min(config.getConfiguration().getDesirableOperationLatency() * 1.2d, config.getConfiguration().getMaxAcceptableRequestLatency());
		int adaptationWindowSize = (int) ceil(config.getConfiguration().getWindowForAdaptation() * config.getConfiguration().getDesirableAverageThroughput());
		int reconfigurationWindowSize = (int) ceil(config.getConfiguration().getWindowForReconfiguration() / config.getConfiguration().getWindowForAdaptation());
		long initialLatencyInNanos = (long) (config.getConfiguration().getDesirableOperationLatency() * NANO_SCALE);
		adaptationWindow = new MovingAverageWindow(adaptationWindowSize, initialLatencyInNanos);
		reconfigurationWindow = new MovingAverageWindow(reconfigurationWindowSize, initialLatencyInNanos);
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
	public double getMeasuredLatencyMillis() {
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
}
