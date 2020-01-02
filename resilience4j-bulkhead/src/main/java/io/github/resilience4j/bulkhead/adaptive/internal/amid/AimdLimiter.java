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

import io.github.resilience4j.bulkhead.adaptive.AdaptiveBulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.LimitPolicy;
import io.github.resilience4j.bulkhead.adaptive.LimitResult;
import io.github.resilience4j.bulkhead.adaptive.internal.config.AimdConfig;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.metrics.Snapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Math.max;

/**
 * limit adapter based into sliding window metrics and AMID algorithm
 */
public class AimdLimiter implements LimitPolicy {
	private static final Logger LOG = LoggerFactory.getLogger(AimdLimiter.class);
	private static final long MILLI_SCALE = 1_000_000L;
    private static final String DROPPING_THE_LIMIT_WITH_NEW_MAX_CONCURRENT_CALLS = "Dropping the limit with new max concurrent calls {}";
	private final AtomicInteger currentMaxLimit;
	private final AdaptiveBulkheadConfig<AimdConfig> amidConfigAdaptiveBulkheadConfig;
	private final long desirableLatency;


	public AimdLimiter(@NonNull AdaptiveBulkheadConfig<AimdConfig> config) {
		this.amidConfigAdaptiveBulkheadConfig = config;
		this.currentMaxLimit = new AtomicInteger(amidConfigAdaptiveBulkheadConfig.getConfiguration().getMinLimit());
		desirableLatency = amidConfigAdaptiveBulkheadConfig.getConfiguration().getDesirableLatency().toNanos();
	}

    /**
     * Adopt the internal bulkhead max concurrent calls limit and wait duration of permission
     * acquiring based into AIMD algorithm
     *
     * @param snapshot the metrics snapshot
     * @param inFlight concurrent in flight calls
     * @return the updated max limit and wait duration
     */
    @Override
    public LimitResult adaptLimitIfAny(@NonNull Snapshot snapshot, int inFlight) {
        return checkIfThresholdsExceeded(snapshot, inFlight);
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
        final Duration snapshotAverageDuration = snapshot.getAverageDuration();
		final float slowCallRate = getSlowCallRate(snapshot);
		Long waitTimeMillis = null;
		if (failureRateInPercentage == -1 && slowCallRate == -1) {
			// do nothing
		} else if (failureRateInPercentage >= amidConfigAdaptiveBulkheadConfig.getConfiguration().getFailureRateThreshold()) {
            waitTimeMillis = handleDropLimit(snapshotAverageDuration);
		} else if (getSlowCallRate(snapshot) >= amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlowCallRateThreshold()) {
            waitTimeMillis = handleDropLimit(snapshotAverageDuration);
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

    private int getCurrentLimit() {
        return currentMaxLimit.get();
    }

    /**
     * Calculate the new wait duration and set the new reduced max concurrent limit
     * - New wait duration --> max of desirable wait duration and the average max calculated latency
     * - New reduced max limit --> max of current limit * concurrency drop multiplier and 1
     * @param averageLatencySeconds new calculated average latency in milliseconds
     * @return the updated waite duration if any
     */
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
     * @param snapshot the current metrics snapshot
     * @return the slow call rate within the the sliding window size if any
     */
    private float getSlowCallRate(Snapshot snapshot) {
        int bufferedCalls = snapshot.getTotalNumberOfCalls();
        if (bufferedCalls == 0 || bufferedCalls < amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlidingWindowSize()) {
            return -1.0f;
        }
        return snapshot.getSlowCallRate();
    }

    /**
     * @param snapshot the current metrics snapshot
     * @return the failure call rate within the the sliding window size if any
     */
    private float getFailureRate(Snapshot snapshot) {
        int bufferedCalls = snapshot.getTotalNumberOfCalls();
        if (bufferedCalls == 0 || bufferedCalls < amidConfigAdaptiveBulkheadConfig.getConfiguration().getSlidingWindowSize()) {
            return -1.0f;
        }
        return snapshot.getFailureRate();
    }


}
