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
import static java.lang.Math.min;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitPolicy;
import io.github.resilience4j.bulkhead.adaptive.LimitResult;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.metrics.Snapshot;

/**
 * limit adapter based sliding window metrics and AMID algorithm
 */
@Deprecated
public class AimdLimiter implements LimitPolicy {
	private static final Logger LOG = LoggerFactory.getLogger(AimdLimiter.class);
	private static final long MILLI_SCALE = 1_000_000L;
	public static final String DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS = "Dropping the limit with new max concurrent calls {}";
	private final AtomicInteger currentMaxLimit;
	private final AdaptiveBulkheadConfig amidConfigAdaptiveBulkheadConfig;
	private final long desirableLatency;

	public AimdLimiter(@NonNull AdaptiveBulkheadConfig config) {
		this.amidConfigAdaptiveBulkheadConfig = config;
		this.currentMaxLimit = new AtomicInteger(config.getMinLimit());
// TODO
//      this.desirableLatency = amidConfigAdaptiveBulkheadConfig.getDesirableLatency().toNanos();
        this.desirableLatency = config.getMaxWaitDuration().toNanos();
	}

	@NonNull
	@Override
	public LimitResult adaptLimitIfAny(@NonNull Snapshot snapshot, int inFlight) {
		return checkIfThresholdsExceeded(snapshot, inFlight);
	}

	/**
	 * Checks the following:
	 * when slow call rate and failure rate =-1, then do nothing
	 * when failure rate is > the failure threshold, then drop limit by drop multiplier
	 * when slow call rate > the slow threshold, then  drop limit by drop multiplier
	 * when none, then increase the limit +1 and check at the end if the new limit is > the max limit, divide it by 2 to maintain the limit within min and max limit range
	 */
	private LimitResult checkIfThresholdsExceeded(Snapshot snapshot, int inFlight) {
		float failureRateInPercentage = getFailureRate(snapshot);
		final Duration averageLatencySeconds = snapshot.getAverageDuration();
		final float slowCallRate = getSlowCallRate(snapshot);
		Long waitTimeMillis = null;
		if (failureRateInPercentage == -1 && slowCallRate == -1) {
			// do nothing
		} else if (failureRateInPercentage >= amidConfigAdaptiveBulkheadConfig.getFailureRateThreshold()) {
			waitTimeMillis = handleDropLimit(averageLatencySeconds);
		} else if (getSlowCallRate(snapshot) >= amidConfigAdaptiveBulkheadConfig.getSlowCallRateThreshold()) {
			waitTimeMillis = handleDropLimit(averageLatencySeconds);
		} else {
		    // TODO getLimitIncrementInflightFactor?
//			if (inFlight * amidConfigAdaptiveBulkheadConfig.getLimitIncrementInflightFactor() >= getCurrentLimit()) {
//				final int limit = currentMaxLimit.incrementAndGet();
//				if (LOG.isDebugEnabled()) {
//					LOG.debug("increasing the limit by increasing the max concurrent calls for {}", limit);
//				}
//			}
		}
        if (currentMaxLimit.get() > amidConfigAdaptiveBulkheadConfig.getMaxLimit()) {
			final int currentMaxLimitUpdated = currentMaxLimit.updateAndGet(limit -> limit / 2);
			if (LOG.isDebugEnabled()) {
				LOG.debug(DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS, currentMaxLimitUpdated);
			}
		}
		final int updatedLimit = currentMaxLimit.updateAndGet(currLimit -> min(
		    amidConfigAdaptiveBulkheadConfig.getMaxLimit(), 
            max(amidConfigAdaptiveBulkheadConfig.getMinLimit(), currLimit)));
		return new LimitResult(updatedLimit, waitTimeMillis != null ? waitTimeMillis : 0);
	}

    private Long handleDropLimit(Duration averageLatencySeconds) {
		long waitTimeMillis;
		waitTimeMillis = (max(0, desirableLatency - averageLatencySeconds.toNanos()) * MILLI_SCALE);
		final int currentMaxLimitUpdated = currentMaxLimit.updateAndGet(limit -> 
            max(1, (int) (limit * amidConfigAdaptiveBulkheadConfig.getConcurrencyDropMultiplier())));
		if (LOG.isDebugEnabled()) {
			LOG.debug(DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS, currentMaxLimitUpdated);
		}
		return waitTimeMillis;
	}

	private float getSlowCallRate(Snapshot snapshot) {
		int bufferedCalls = snapshot.getTotalNumberOfCalls();
		if (bufferedCalls == 0 || bufferedCalls < amidConfigAdaptiveBulkheadConfig.getSlidingWindowSize()) {
			return -1.0f;
		}
		return snapshot.getSlowCallRate();
	}

	private float getFailureRate(Snapshot snapshot) {
		int bufferedCalls = snapshot.getTotalNumberOfCalls();
		if (bufferedCalls == 0 || bufferedCalls < amidConfigAdaptiveBulkheadConfig.getSlidingWindowSize()) {
			return -1.0f;
		}
		return snapshot.getFailureRate();
	}

}
