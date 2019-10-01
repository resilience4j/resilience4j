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
package io.github.resilience4j.bulkhead.adaptive.internal.config;

import java.time.Duration;
import java.util.Objects;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.core.lang.NonNull;

/**
 * abstract common configuration
 */
public class AbstractConfig {
	private static final SlidingWindow DEFAULT_SLIDING_WINDOW_TYPE = SlidingWindow.COUNT_BASED;
	private static final float DEFAULT_FAILURE_RATE_THRESHOLD = 50.0f; // Percentage
	private static final float DEFAULT_SLOW_CALL_RATE_THRESHOLD = 50.0f; // Percentage
	private static final int DEFAULT_SLIDING_WINDOW_SIZE = 100;
	private static final long DEFAULT_SLOW_CALL_DURATION_THRESHOLD = 5; // Seconds
	private static final int DEFAULT_SLIDING_WIN_TIME = 10; // Seconds

	float failureRateThreshold = DEFAULT_FAILURE_RATE_THRESHOLD;
	int slidingWindowSize = DEFAULT_SLIDING_WINDOW_SIZE;
	SlidingWindow slidingWindowType = DEFAULT_SLIDING_WINDOW_TYPE;
	float slowCallRateThreshold = DEFAULT_SLOW_CALL_RATE_THRESHOLD;
	Duration desirableLatency = Duration.ofSeconds(DEFAULT_SLOW_CALL_DURATION_THRESHOLD);
	int slidingWindowTime = DEFAULT_SLIDING_WIN_TIME;

	@NonNull
	public Duration getDesirableLatency() {
		return desirableLatency;
	}

	@NonNull
	public int getSlidingWindowTime() {
		return slidingWindowTime;
	}

	@NonNull
	public float getFailureRateThreshold() {
		return failureRateThreshold;
	}

	@NonNull
	public int getSlidingWindowSize() {
		return slidingWindowSize;
	}

	@NonNull
	public SlidingWindow getSlidingWindowType() {
		return slidingWindowType;
	}

	@NonNull
	public float getSlowCallRateThreshold() {
		return slowCallRateThreshold;
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AbstractConfig that = (AbstractConfig) o;
		return Double.compare(that.failureRateThreshold, failureRateThreshold) == 0 &&
				slidingWindowSize == that.slidingWindowSize &&
				Double.compare(that.slowCallRateThreshold, slowCallRateThreshold) == 0 &&
				slidingWindowTime == that.slidingWindowTime &&
				slidingWindowType == that.slidingWindowType &&
				Objects.equals(desirableLatency, that.desirableLatency);
	}

	@Override
	public int hashCode() {
		return Objects.hash(failureRateThreshold, slidingWindowSize, slidingWindowType, slowCallRateThreshold, desirableLatency, slidingWindowTime);
	}

	@Override
	public String toString() {
		return "AbstractConfig{" +
				"failureRateThreshold=" + failureRateThreshold +
				", slidingWindowSize=" + slidingWindowSize +
				", slidingWindowType=" + slidingWindowType +
				", slowCallRateThreshold=" + slowCallRateThreshold +
				", desirableLatency=" + desirableLatency +
				", slidingWindowTime=" + slidingWindowTime +
				'}';
	}

	public static class Builder<T extends AbstractConfig> {
		T config;

		Builder() {
		}

		Builder(T bulkheadConfig) {
			this.config = bulkheadConfig;
		}


		/**
		 * Desirable operation latency in millis/operation.
		 * This is our foothold that we will circling around.
		 * System will constantly measure actual average latency and compare it with "slowCallDurationThreshold".
		 * If you actual latency will be lower than "slowCallDurationThreshold",
		 * will calculate the difference and use it as {@link BulkheadConfig}.maxWaitTime
		 *
		 * @param desirableOperationLatency - in sec/op
		 * @return a {@link Builder}
		 */
		public Builder<T> slowCallDurationThreshold(long desirableOperationLatency) {
			if (desirableOperationLatency <= 0.0) {
				throw new IllegalArgumentException("slowCallDurationThreshold must be a positive value greater than zero");
			}
			config.desirableLatency = Duration.ofMillis(desirableOperationLatency);
			return this;
		}

		/**
		 * @param slidingWindowSize to be defined if u want to use {@link io.github.resilience4j.core.metrics.FixedSizeSlidingWindowMetrics}
		 * @return a {@link Builder}
		 */
		public Builder<T> slidingWindowSize(int slidingWindowSize) {
			if (slidingWindowSize <= 0.0) {
				throw new IllegalArgumentException("slidingWindowSize must be a positive value greater than zero");
			}
			config.slidingWindowSize = slidingWindowSize;
			return this;
		}


		/**
		 * @param slidingWindowTime to be defined if u want to use {@link io.github.resilience4j.core.metrics.SlidingTimeWindowMetrics}
		 * @return a {@link Builder}
		 */
		public Builder<T> slidingWindowTime(int slidingWindowTime) {
			if (slidingWindowTime <= 0.0) {
				throw new IllegalArgumentException("slidingWindowTime must be a positive value greater than zero");
			}
			config.slidingWindowTime = slidingWindowTime;
			config.slidingWindowType = SlidingWindow.TIME_BASED;
			return this;
		}


		/**
		 * @param failureRateThreshold failure calls rate percentage
		 * @return a {@link Builder}
		 */
		public Builder<T> failureRateThreshold(float failureRateThreshold) {
			if (failureRateThreshold <= 0.0 || failureRateThreshold > 100.0) {
				throw new IllegalArgumentException("failureRateThreshold must be a positive value greater than zero and less than 100");
			}
			config.failureRateThreshold = failureRateThreshold;
			return this;
		}

		/**
		 * @param slowCallRateThreshold slow call rate percentage
		 * @return a {@link Builder}
		 */
		public Builder<T> slowCallRateThreshold(float slowCallRateThreshold) {
			if (slowCallRateThreshold <= 0.0 || slowCallRateThreshold > 100.0) {
				throw new IllegalArgumentException("slowCallRateThreshold must be a positive value greater than zero and less than 100");
			}
			config.slowCallRateThreshold = slowCallRateThreshold;
			return this;
		}


		/**
		 * @param slidingWindow define the sliding window Type , please check {@link SlidingWindow}
		 * @return a {@link Builder}
		 */
		public Builder<T> slidingWindowType(SlidingWindow slidingWindow) {
			config.slidingWindowType = slidingWindow;
			return this;
		}

		/**
		 * Builds a AdaptiveBulkheadConfig
		 *
		 * @return the AdaptiveBulkheadConfig
		 */
		public T build() {

			return config;
		}
	}

	public enum SlidingWindow {
		TIME_BASED, COUNT_BASED
	}
}


