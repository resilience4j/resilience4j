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
package io.github.resilience4j.bulkhead.adaptive.internal.percentile;

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
import io.github.resilience4j.bulkhead.adaptive.internal.config.PercentileConfig;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.core.lang.NonNull;

/**
 * limit adapter based into moving average for the windows checks
 */
public class PercentileLimitAdapter extends AbstractLimiterAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(PercentileLimitAdapter.class);
	private final double LOW_LATENCY_MUL;
	private final double initialMaxLatency;
	private final double desirableLatency;
	private final int percentile;
	private volatile double currentPercentileLatencyNanos;
	// measurement window collections. They are !!!NOT THREAD SAFE!!!
	private final PercentileWindow adaptationWindow;
	private final PercentileWindow reconfigurationWindow;

	public PercentileLimitAdapter(@NonNull AdaptiveBulkheadConfig<PercentileConfig> config, Consumer<BulkheadLimit> publishEventConsumer) {
		super(config.getConfiguration().getConcurrencyDropMultiplier(), min(config.getConfiguration().getDesirableOperationLatency() * 1.2d, config.getConfiguration().getMaxAcceptableRequestLatency()), publishEventConsumer);
		initialMaxLatency = config.getConfiguration().getMaxAcceptableRequestLatency();
		desirableLatency = config.getConfiguration().getDesirableOperationLatency();
		LOW_LATENCY_MUL = config.getConfiguration().getLowLatencyMultiplier();
		percentile = config.getConfiguration().getPercentile();
		int adaptationWindowSize = (int) ceil(config.getConfiguration().getWindowForAdaptation() * config.getConfiguration().getDesirableAverageThroughput());
		int reconfigurationWindowSize = config.getConfiguration().getWindowForReconfiguration() / config.getConfiguration().getWindowForAdaptation();
		long initialLatencyInNanos = (long) (config.getConfiguration().getDesirableOperationLatency() * NANO_SCALE);
		adaptationWindow = new PercentileWindow(adaptationWindowSize, initialLatencyInNanos);
		reconfigurationWindow = new PercentileWindow(reconfigurationWindowSize, initialLatencyInNanos);

	}

	@Override
	public void adaptLimitIfAny(@NonNull Bulkhead bulkhead, @NonNull Duration callTime) {
		boolean endOfAdaptationWindow = adaptationWindow.measure(callTime.toNanos());
		if (LOG.isDebugEnabled()) {
			LOG.debug("end of adoption window is {}", endOfAdaptationWindow);
		}
		if (endOfAdaptationWindow) {
			double percentileLatencyNanos = adaptConcurrencyLevel(bulkhead);
			if (LOG.isDebugEnabled()) {
				LOG.debug("new percentileLatencyNanos is {}", percentileLatencyNanos);
			}
			currentPercentileLatencyNanos = percentileLatencyNanos;
			boolean endOfReconfigurationWindow = reconfigurationWindow.measure(percentileLatencyNanos);
			if (endOfReconfigurationWindow) {
				adjustConfiguration(percentileLatencyNanos);
			}
		}
	}


	@Override
	public double getMeasuredLatencyMillis() {
		return currentPercentileLatencyNanos / 1e6;
	}


	/**
	 * adjust configuration widows
	 *
	 * @param percentileLatencyNanos the percentile latency for the service call
	 */
	@SuppressWarnings("NonAtomicOperationOnVolatileField")
	private void adjustConfiguration(double percentileLatencyNanos) {
		// we can change latency only between desirableLatency * LOW_LATENCY_MUL and initialMaxLatency
		currentMaxLatency = min((percentileLatencyNanos) / NANO_SCALE, initialMaxLatency);
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
		double percentileLatencyNanos = adaptationWindow.computePercentile(percentile);
		double percentileLatencySeconds = (percentileLatencyNanos) / NANO_SCALE;
		if (LOG.isDebugEnabled()) {
			LOG.debug("new percentileLatencySeconds is {}", percentileLatencySeconds);
		}
		long waitTimeMillis = (long) (max(0d, desirableLatency - percentileLatencySeconds) * MILLI_SCALE);
		if (LOG.isDebugEnabled()) {
			LOG.debug("new waitTimeMillis is {}", waitTimeMillis);
		}
		adoptLimit(bulkhead, percentileLatencySeconds, waitTimeMillis);
		return percentileLatencyNanos;
	}


}
