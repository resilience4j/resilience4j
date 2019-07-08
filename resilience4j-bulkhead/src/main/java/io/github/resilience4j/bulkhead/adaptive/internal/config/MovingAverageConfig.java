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

import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveLimitBulkhead;

/**
 * A {@link MovingAverageConfig} configures a adaptation capabilities of  {@link AdaptiveLimitBulkhead}
 */
public class MovingAverageConfig extends AbstractConfig {


	private MovingAverageConfig() {
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link MovingAverageConfig.Builder}
	 */
	public static Builder from(MovingAverageConfig baseConfig) {
		return new MovingAverageConfig.Builder(baseConfig);
	}

	/**
	 * Creates a default Bulkhead configuration.
	 *
	 * @return a default Bulkhead configuration.
	 */
	public static MovingAverageConfig ofDefaults() {
		return new MovingAverageConfig.Builder().build();
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link MovingAverageConfig.Builder}
	 */
	public static Builder builder() {
		return new Builder();
	}


	public static class Builder extends AbstractConfig.Builder<MovingAverageConfig> {

		private Builder() {
			super();
			config = new MovingAverageConfig();
		}

		private Builder(MovingAverageConfig bulkheadConfig) {
			super(bulkheadConfig);
			this.config = bulkheadConfig;
		}

		@Override
		public MovingAverageConfig build() {
			return super.build();
		}
	}


}
