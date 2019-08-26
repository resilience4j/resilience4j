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
package io.github.resilience4j.bulkhead.adaptive.internal.amid;

import static java.lang.Math.max;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitPolicy;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AIMDConfig;
import io.github.resilience4j.bulkhead.event.BulkheadLimit;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitDecreasedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnLimitIncreasedEvent;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics;
import io.github.resilience4j.core.metrics.Snapshot;

/**
 * limit adapter based sliding window metrics and AMID algorithm
 */
public class AIMDLimiter implements LimitPolicy<Bulkhead> {
	private static final Logger LOG = LoggerFactory.getLogger(AIMDLimiter.class);
	private static final long MILLI_SCALE = 1_000_000L;
	public static final String DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS = "Dropping the limit with new max concurrent calls {}";
	private final AtomicInteger currentMaxLimit;
	private final Consumer<BulkheadLimit> publishEventConsumer;
	private final Metrics metrics;
	private final AdaptiveBulkheadConfig<AIMDConfig> amidConfigAdaptiveBulkheadConfig;
	private final long desirableLatency;


	public AIMDLimiter(@NonNull AdaptiveBulkheadConfig<AIMDConfig> config, Consumer<BulkheadLimit> publishEventConsumer) {
		this.amidConfigAdaptiveBulkheadConfig = config;
		if (amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlidingWindowType() == AIMDConfig.SlidingWindow.COUNT_BASED) {
			this.metrics = new FixedSizeSlidingWindowMetrics(config.getConfiguration().getSlidingWindowSize());
		} else {
			this.metrics = new SlidingTimeWindowMetrics(config.getConfiguration().getSlidingWindowTime());
		}
		this.publishEventConsumer = publishEventConsumer;
		this.currentMaxLimit = new AtomicInteger(amidConfigAdaptiveBulkheadConfig.getConfiguration().getMinLimit());
		desirableLatency = amidConfigAdaptiveBulkheadConfig.getConfiguration().getDesirableLatency().toNanos();
	}

	@Override
	public void adaptLimitIfAny(@NonNull Bulkhead bulkhead, @NonNull Duration callTime, boolean isSuccess, int inFlight) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("starting the adation of the limit for callTime :{} , isSuccess: {}, inFlight: {}", callTime.toMillis(), isSuccess, inFlight);
		}
		Snapshot snapshot;
		if (isSuccess) {
			if (callTime.toNanos() > desirableLatency) {
				snapshot = metrics.record(callTime.toNanos(), TimeUnit.NANOSECONDS, Metrics.Outcome.SLOW_SUCCESS);
			} else {
				snapshot = metrics.record(callTime.toNanos(), TimeUnit.NANOSECONDS, Metrics.Outcome.SUCCESS);
			}
		} else {
			if (callTime.toNanos() > desirableLatency) {
				snapshot = metrics.record(callTime.toNanos(), TimeUnit.NANOSECONDS, Metrics.Outcome.SLOW_ERROR);
			} else {
				snapshot = metrics.record(callTime.toNanos(), TimeUnit.NANOSECONDS, Metrics.Outcome.ERROR);
			}
		}
		checkIfThresholdsExceeded(bulkhead, snapshot, inFlight);
	}

	@Override
	@NonNull
	public Consumer<BulkheadLimit> bulkheadLimitConsumer() {
		return publishEventConsumer;
	}

	@Override
	@NonNull
	public Metrics getMetrics() {
		return metrics;
	}

	@Override
	public int getCurrentLimit() {
		return currentMaxLimit.get();
	}


	/**
	 * Checks if the following :
	 * 1- if slow call rate and failure rate =-1 , do nothing
	 * 2- if failure rate is > the failure threshold -> drop limit by drop multiplier
	 * 3- if slow call rate > the slow threshold ->  drop limit by drop multiplier
	 * 4- if none , then increase the limit +1 and check at the end if the new limit is > the max limit , divide it by 2 to maintain the limit within min and max limit range
	 */
	private void checkIfThresholdsExceeded(Bulkhead bulkhead, Snapshot snapshot, int inFlight) {
		float failureRateInPercentage = getFailureRate(snapshot);
		final Duration averageLatencySeconds = snapshot.getAverageDuration();
		final float slowCallRate = getSlowCallRate(snapshot);
		Long waitTimeMillis = null;
		if (failureRateInPercentage == -1 && slowCallRate == -1) {
			// do nothing
		} else if (failureRateInPercentage >= amidConfigAdaptiveBulkheadConfig.getConfiguration().getFailureRateThreshold()) {
			waitTimeMillis = handleDropLimit(averageLatencySeconds);
		} else if (getSlowCallRate(snapshot) >= amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlowCallRateThreshold()) {
			waitTimeMillis = handleDropLimit(averageLatencySeconds);
		} else {
			if (inFlight * amidConfigAdaptiveBulkheadConfig.getConfiguration().getLimitIncrementInflightFactor() >= getCurrentLimit()) {
				final int limit = currentMaxLimit.incrementAndGet();
				if (LOG.isDebugEnabled()) {
					LOG.debug("increasing the limit by increasing the max concurrent calls for {}", limit);
				}
			}
		}
		if (getCurrentLimit() > amidConfigAdaptiveBulkheadConfig.getConfiguration().getMaxLimit()) {
			final int currentMaxLimitUpdated = currentMaxLimit.updateAndGet(limit -> limit / 2);
			if (LOG.isDebugEnabled()) {
				LOG.debug(DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS, currentMaxLimitUpdated);
			}
		}
		final int updatedLimit = currentMaxLimit.updateAndGet(currLimit -> Math.min(amidConfigAdaptiveBulkheadConfig.getConfiguration().getMaxLimit(), max(amidConfigAdaptiveBulkheadConfig.getConfiguration().getMinLimit(), currLimit)));
		adoptLimit(bulkhead, updatedLimit, waitTimeMillis != null ? waitTimeMillis : 0);
	}

	private Long handleDropLimit(Duration averageLatencySeconds) {
		Long waitTimeMillis;
		waitTimeMillis = (max(0L, desirableLatency - averageLatencySeconds.toNanos()) * MILLI_SCALE);
		final int currentMaxLimitUpdated = currentMaxLimit.updateAndGet(limit -> max((int) (limit * amidConfigAdaptiveBulkheadConfig.getConfiguration().getConcurrencyDropMultiplier()), 1));
		if (LOG.isDebugEnabled()) {
			LOG.debug(DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS, currentMaxLimitUpdated);
		}
		return waitTimeMillis;
	}

	/**
	 * adopt the limit based into the new calculated average
	 *
	 * @param bulkhead       the target semaphore bulkhead
	 * @param updatedLimit   calculated new limit
	 * @param waitTimeMillis new wait time
	 */
	private void adoptLimit(Bulkhead bulkhead, int updatedLimit, long waitTimeMillis) {
		if (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() < updatedLimit) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("increasing bulkhead limit by increasing the max concurrent calls for {}", updatedLimit);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(updatedLimit)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			bulkheadLimitConsumer().accept(new BulkheadOnLimitIncreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(waitTimeMillis, updatedLimit)));
		} else if (bulkhead.getBulkheadConfig().getMaxConcurrentCalls() == updatedLimit) {
			// do nothing
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Dropping the bulkhead limit with new max concurrent calls {}", updatedLimit);
			}
			final BulkheadConfig updatedConfig = BulkheadConfig.custom()
					.maxConcurrentCalls(updatedLimit)
					.maxWaitDuration(Duration.ofMillis(waitTimeMillis))
					.build();
			bulkhead.changeConfig(updatedConfig);
			bulkheadLimitConsumer().accept(new BulkheadOnLimitDecreasedEvent(bulkhead.getName().substring(0, bulkhead.getName().indexOf('-')),
					eventData(waitTimeMillis, updatedLimit)));
		}
	}


	private Map<String, String> eventData(long waitTimeMillis, int newMaxConcurrentCalls) {
		Map<String, String> eventData = new HashMap<>();
		eventData.put("newMaxConcurrentCalls", String.valueOf(newMaxConcurrentCalls));
		eventData.put("newWaitTimeMillis", String.valueOf(waitTimeMillis));
		return eventData;
	}

	private float getSlowCallRate(Snapshot snapshot) {
		int bufferedCalls = snapshot.getTotalNumberOfCalls();
		if (bufferedCalls == 0 || bufferedCalls < amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlidingWindowSize()) {
			return -1.0f;
		}
		return snapshot.getSlowCallRate();
	}

	private float getFailureRate(Snapshot snapshot) {
		int bufferedCalls = snapshot.getTotalNumberOfCalls();
		if (bufferedCalls == 0 || bufferedCalls < amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlidingWindowSize()) {
			return -1.0f;
		}
		return snapshot.getFailureRate();
	}


}
