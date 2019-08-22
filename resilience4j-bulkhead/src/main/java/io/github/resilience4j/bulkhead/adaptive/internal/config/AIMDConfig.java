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

import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveLimitBulkhead;
import io.github.resilience4j.core.lang.NonNull;

/**
 * A {@link AIMDConfig} configures a adaptation capabilities of  {@link AdaptiveLimitBulkhead}
 */
public class AIMDConfig extends AbstractConfig {
	int minConcurrentRequestLimit = 5;
	int maxConcurrentRequestLimit = 200;
	double concurrencyDropMultiplier = 0.87d;
	// LimitIncrementInflightFactor will increment the limit only if inflight * LimitIncrementInflightFactor > limit
	int limitIncrementInflightFactor = 2;

	private AIMDConfig() {
	}

	@NonNull
	public int getMaxLimit() {
		return maxConcurrentRequestLimit;
	}

	@NonNull
	public int getMinLimit() {
		return minConcurrentRequestLimit;
	}

	@NonNull
	public double getConcurrencyDropMultiplier() {
		return concurrencyDropMultiplier;
	}

	public int getLimitIncrementInflightFactor() {
		return limitIncrementInflightFactor;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;
		AIMDConfig that = (AIMDConfig) o;
		return minConcurrentRequestLimit == that.minConcurrentRequestLimit && limitIncrementInflightFactor == that.limitIncrementInflightFactor &&
				maxConcurrentRequestLimit == that.maxConcurrentRequestLimit &&
				Double.compare(that.concurrencyDropMultiplier, concurrencyDropMultiplier) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), minConcurrentRequestLimit, maxConcurrentRequestLimit, concurrencyDropMultiplier, limitIncrementInflightFactor);
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AIMDConfig.Builder}
	 */
	public static Builder from(AIMDConfig baseConfig) {
		return new AIMDConfig.Builder(baseConfig);
	}

	/**
	 * Creates a default Bulkhead configuration.
	 *
	 * @return a default Bulkhead configuration.
	 */
	public static AIMDConfig ofDefaults() {
		return new AIMDConfig.Builder().build();
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AIMDConfig.Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}


	public static class Builder extends AbstractConfig.Builder<AIMDConfig> {

		private Builder() {
			super();
			config = new AIMDConfig();
		}

		/**
		 * @param concurrencyDropMultiplier concurrency drop multiplier
		 * @return a {@link AbstractConfig.Builder}
		 */
		public Builder concurrencyDropMultiplier(double concurrencyDropMultiplier) {
			if (concurrencyDropMultiplier < 0.5 || concurrencyDropMultiplier > 1) {
				config.concurrencyDropMultiplier = 0.85d;
			} else {
				config.concurrencyDropMultiplier = concurrencyDropMultiplier;
			}
			return this;
		}


		/**
		 * @param minConcurrentRequestLimit min limit value
		 * @return a {@link AbstractConfig.Builder}
		 */
		public Builder minConcurrentRequestsLimit(int minConcurrentRequestLimit) {
			if (minConcurrentRequestLimit <= 0.0) {
				throw new IllegalArgumentException("minConcurrentRequestsLimit must be a positive value greater than zero");
			}
			config.minConcurrentRequestLimit = minConcurrentRequestLimit;
			return this;
		}


		/**
		 * @param limitIncrementInflightFactor the inFlight requests multiplier
		 * @return a {@link AbstractConfig.Builder}
		 */
		public Builder limitIncrementInflightFactor(int limitIncrementInflightFactor) {
			if (limitIncrementInflightFactor <= 0.0) {
				throw new IllegalArgumentException("limitIncrementInflightFactor must be a positive value greater than zero");
			}
			config.limitIncrementInflightFactor = limitIncrementInflightFactor;
			return this;
		}

		/**
		 * @param maxConcurrentRequestLimit max limit
		 * @return a {@link AbstractConfig.Builder}
		 */
		public Builder maxConcurrentRequestsLimit(int maxConcurrentRequestLimit) {
			if (maxConcurrentRequestLimit <= 0.0) {
				throw new IllegalArgumentException("maxConcurrentRequestsLimit must be a positive value greater than zero");
			}
			config.maxConcurrentRequestLimit = maxConcurrentRequestLimit;
			return this;
		}


		private Builder(AIMDConfig bulkheadConfig) {
			super(bulkheadConfig);
			this.config = bulkheadConfig;
		}

		@Override
		public AIMDConfig build() {
			return super.build();
		}
	}


}

