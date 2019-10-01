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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitPolicy;
import io.github.resilience4j.bulkhead.adaptive.LimitResult;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AIMDConfig;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics;
import io.github.resilience4j.core.metrics.Snapshot;

/**
 * limit adapter based sliding window metrics and AMID algorithm
 */
public class AIMDLimiter implements LimitPolicy {
	private static final Logger LOG = LoggerFactory.getLogger(AIMDLimiter.class);
	private static final long MILLI_SCALE = 1_000_000L;
	public static final String DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS = "Dropping the limit with new max concurrent calls {}";
	private final AtomicInteger currentMaxLimit;
	private final Metrics metrics;
	private final AdaptiveBulkheadConfig<AIMDConfig> amidConfigAdaptiveBulkheadConfig;
	private final long desirableLatency;


	public AIMDLimiter(@NonNull AdaptiveBulkheadConfig<AIMDConfig> config) {
		this.amidConfigAdaptiveBulkheadConfig = config;
		if (amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlidingWindowType() == AIMDConfig.SlidingWindow.COUNT_BASED) {
			this.metrics = new FixedSizeSlidingWindowMetrics(config.getConfiguration().getSlidingWindowSize());
		} else {
			this.metrics = new SlidingTimeWindowMetrics(config.getConfiguration().getSlidingWindowTime());
		}
		this.currentMaxLimit = new AtomicInteger(amidConfigAdaptiveBulkheadConfig.getConfiguration().getMinLimit());
		desirableLatency = amidConfigAdaptiveBulkheadConfig.getConfiguration().getDesirableLatency().toNanos();
	}

	@Override
	public LimitResult adaptLimitIfAny(@NonNull long callTime, boolean isSuccess, int inFlight) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("starting the adation of the limit for callTime :{} , isSuccess: {}, inFlight: {}", callTime, isSuccess, inFlight);
		}
		Snapshot snapshot;
		final long callTimeNanos = TimeUnit.MILLISECONDS.toNanos(callTime);
		if (isSuccess) {
			if (callTimeNanos > desirableLatency) {
				snapshot = metrics.record(callTimeNanos, TimeUnit.NANOSECONDS, Metrics.Outcome.SLOW_SUCCESS);
			} else {
				snapshot = metrics.record(callTimeNanos, TimeUnit.NANOSECONDS, Metrics.Outcome.SUCCESS);
			}
		} else {
			if (callTimeNanos > desirableLatency) {
				snapshot = metrics.record(callTimeNanos, TimeUnit.NANOSECONDS, Metrics.Outcome.SLOW_ERROR);
			} else {
				snapshot = metrics.record(callTimeNanos, TimeUnit.NANOSECONDS, Metrics.Outcome.ERROR);
			}
		}
		return checkIfThresholdsExceeded(snapshot, inFlight);
	}


	@Override
	@NonNull
	public Metrics getMetrics() {
		return metrics;
	}


	private int getCurrentLimit() {
		return currentMaxLimit.get();
	}


	/**
	 * Checks if the following :
	 * 1- if slow call rate and failure rate =-1 , do nothing
	 * 2- if failure rate is > the failure threshold -> drop limit by drop multiplier
	 * 3- if slow call rate > the slow threshold ->  drop limit by drop multiplier
	 * 4- if none , then increase the limit +1 and check at the end if the new limit is > the max limit , divide it by 2 to maintain the limit within min and max limit range
	 */
	private LimitResult checkIfThresholdsExceeded(Snapshot snapshot, int inFlight) {
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
		return new LimitResult(updatedLimit, waitTimeMillis != null ? waitTimeMillis : 0);
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
