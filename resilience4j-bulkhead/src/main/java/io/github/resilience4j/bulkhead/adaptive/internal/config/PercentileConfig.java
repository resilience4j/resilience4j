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

import java.util.Objects;

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


	public static class Builder extends AbstractConfig.Builder<PercentileConfig> {

		private Builder() {
			super();
			config = new PercentileConfig();
		}

		private Builder(PercentileConfig bulkheadConfig) {
			super(bulkheadConfig);
			this.config = bulkheadConfig;
		}

		/**
		 * the percentile value to be used with the percentile limiter default is 50%
		 *
		 * @param percentile the percentile value if percentile limiter to be used
		 * @return a {@link Builder}
		 */
		public Builder percentile(int percentile) {
			if (percentile <= 0) {
				throw new IllegalArgumentException("percentile must be a positive value greater than zero");
			}
			config.percentile = percentile;
			return this;
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
