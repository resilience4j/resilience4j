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
package io.github.resilience4j.bulkhead.adaptive.internal.config;

import java.util.Objects;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveLimitBulkhead;

/**
 * A {@link PercentileConfig} configures a adaptation capabilities of  {@link AdaptiveLimitBulkhead}
 */
public class PercentileConfig extends AbstractConfig {

	private int percentile = 50;

	public int getPercentile() {
		return percentile;
	}

	private PercentileConfig() {
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link PercentileConfig.Builder}
	 */
	public static Builder from(PercentileConfig baseConfig) {
		return new PercentileConfig.Builder(baseConfig);
	}

	/**
	 * Creates a default Bulkhead configuration.
	 *
	 * @return a default Bulkhead configuration.
	 */
	public static PercentileConfig ofDefaults() {
		return new PercentileConfig.Builder().build();
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link PercentileConfig.Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}


	public static class Builder {
		private PercentileConfig config = new PercentileConfig();

		private Builder() {
		}

		private Builder(PercentileConfig bulkheadConfig) {
			this.config = bulkheadConfig;
		}

		/**
		 * Desirable average throughput in op/second.
		 * This param will provide us with initial configuration.
		 * The closer it to the real value - faster we can figure out real concurrency limits.
		 *
		 * @param desirableAverageThroughput - in op/sec
		 * @return a {@link Builder}
		 */
		public Builder desirableAverageThroughput(double desirableAverageThroughput) {
			if (desirableAverageThroughput <= 0.0) {
				throw new IllegalArgumentException("desirableAverageThroughput must be a positive value greater than zero");
			}
			config.desirableAverageThroughput = desirableAverageThroughput;
			return this;
		}

		/**
		 * Desirable operation latency in seconds/operation.
		 * This is our foothold that we will circling around.
		 * System will constantly measure actual average latency and compare it with "desirableOperationLatency".
		 * If you actual latency will be lower than "desirableOperationLatency",
		 * will calculate the difference and use it as {@link BulkheadConfig}.maxWaitTime
		 * If you actual latency will be higher than "desirableOperationLatency" TODO: describe behaviour.
		 *
		 * @param desirableOperationLatency - in sec/op
		 * @return a {@link Builder}
		 */
		public Builder desirableOperationLatency(double desirableOperationLatency) {
			if (desirableOperationLatency <= 0.0) {
				throw new IllegalArgumentException("desirableOperationLatency must be a positive value greater than zero");
			}
			config.desirableOperationLatency = desirableOperationLatency;
			return this;
		}

		/**
		 * Maximum acceptable operation latency in seconds/operation.
		 * This number should be set wisely, because it can eliminate all adaptive capabilities,
		 * system will do its best to never reach such latency,
		 * so you can set it 20-30 % higher than your usual average latency.
		 * If you actual latency will be higher than "maxAcceptableRequestLatency" TODO: describe behaviour.
		 * <p>
		 * Default value is {@link PercentileConfig}.desirableOperationLatency * 1.3
		 *
		 * @param maxAcceptableRequestLatency - in sec/op
		 * @return a {@link Builder}
		 */
		public Builder maxAcceptableRequestLatency(double maxAcceptableRequestLatency) {
			if (maxAcceptableRequestLatency <= 0.0) {
				throw new IllegalArgumentException("maxAcceptableRequestLatency must be a positive value greater than zero");
			}
			config.maxAcceptableRequestLatency = maxAcceptableRequestLatency;
			return this;
		}


		/**
		 * @param lowLatencyMultiplier low latency multiplier factor
		 * @return a {@link Builder}
		 */
		public Builder lowLatencyMultiplier(double lowLatencyMultiplier) {
			if (lowLatencyMultiplier <= 0.0) {
				throw new IllegalArgumentException("lowLatencyMultiplier must be a positive value greater than zero");
			}
			config.lowLatencyMultiplier = lowLatencyMultiplier;
			return this;
		}


		/**
		 * @param concurrencyDropMultiplier concurrency drop multiplier
		 * @return a {@link Builder}
		 */
		public Builder concurrencyDropMultiplier(double concurrencyDropMultiplier) {
			if (concurrencyDropMultiplier <= 0.0) {
				throw new IllegalArgumentException("concurrencyDropMultiplier must be a positive value greater than zero");
			}
			config.concurrencyDropMultiplier = concurrencyDropMultiplier;
			return this;
		}

		/**
		 * Window size for adaptation.
		 * After each cycle with the size of "windowForAdaptation" will calculate current average latency
		 * from adaptation window and will try to adapt concurrency level.
		 *
		 * @param windowForAdaptation - duration
		 * @return a {@link Builder}
		 */
		public Builder windowForAdaptation(int windowForAdaptation) {
			config.windowForAdaptation = windowForAdaptation;
			return this;
		}

		/**
		 * the percentile value to be used with the percentile limiter default is 50%
		 *
		 * @param percentile the percentile value if percentile limiter to be used
		 * @return a {@link Builder}
		 */
		public Builder percentile(int percentile) {
			config.percentile = percentile;
			return this;
		}


		/**
		 * Window size for reconfiguration.
		 * After each cycle with the size  of "windowForReconfiguration" will calculate standard deviation of latencies
		 * after different adaptations and will recalculate local "maxAcceptableRequestLatency" for the next cycle.
		 * This will help us handle daily latency changes gracefully without reaching "maxAcceptableRequestLatency" often.
		 *
		 * @param windowForReconfiguration - duration
		 * @return a {@link Builder}
		 */
		public Builder windowForReconfiguration(int windowForReconfiguration) {
			config.windowForReconfiguration = windowForReconfiguration;
			return this;
		}

		/**
		 * Builds a AdaptiveBulkheadConfig
		 *
		 * @return the AdaptiveBulkheadConfig
		 */
		public PercentileConfig build() {
			if (config.maxAcceptableRequestLatency < config.desirableOperationLatency) {
				throw new IllegalArgumentException("maxAcceptableRequestLatency can't be less" +
						" than desirableOperationLatency");
			}
			if (config.windowForAdaptation <= (long) (config.desirableAverageThroughput * 15)) {
				throw new IllegalArgumentException("windowForAdaptation is too small. " +
						"We wan't be able to make at least 15 measurements during this window.");
			}
			if (15 >= (config.windowForReconfiguration / config.windowForAdaptation)) {
				throw new IllegalArgumentException("windowForReconfiguration is too small. " +
						"windowForReconfiguration should be at least 15 times bigger than windowForAdaptation.");
			}

			return config;
		}
	}

	@Override
	public String toString() {
		return "PercentileConfig{" +
				"percentile=" + percentile +
				super.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PercentileConfig that = (PercentileConfig) o;
		return super.equals(that) &&
				Objects.equals(percentile, that.percentile);
	}

	@Override
	public int hashCode() {
		return Objects.hash(percentile, super.hashCode());
	}
}
