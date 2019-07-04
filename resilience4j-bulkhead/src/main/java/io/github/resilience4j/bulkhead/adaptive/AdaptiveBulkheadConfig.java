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
package io.github.resilience4j.bulkhead.adaptive;

import java.util.function.Predicate;

import io.github.resilience4j.bulkhead.adaptive.internal.AdaptiveLimitBulkhead;
import io.github.resilience4j.bulkhead.adaptive.internal.config.MovingAverageConfig;
import io.github.resilience4j.core.lang.NonNull;
import io.github.resilience4j.core.lang.Nullable;

/**
 * A {@link AdaptiveBulkheadConfig} configures a adaptation capabilities of  {@link AdaptiveLimitBulkhead}
 */
public class AdaptiveBulkheadConfig<T> {
	@NonNull
	private T config;
	@Nullable
	private Predicate<Exception> adaptIfError;
	private int initialConcurrency = 1;

	private AdaptiveBulkheadConfig() {
	}

	public int getInitialConcurrency() {
		return initialConcurrency;
	}

	@NonNull
	public T getConfiguration() {
		return config;
	}

	@Nullable
	public Predicate<Exception> getAdaptIfError() {
		return adaptIfError;
	}
	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AdaptiveBulkheadConfig.Builder}
	 */
	public static <T> Builder<T> from(AdaptiveBulkheadConfig<T> baseConfig) {
		return AdaptiveBulkheadConfig.builder(baseConfig);
	}

	/**
	 * Creates a default Bulkhead configuration.
	 *
	 * @return a default Bulkhead configuration.
	 */
	public static AdaptiveBulkheadConfig<MovingAverageConfig> ofDefaults() {
		return AdaptiveBulkheadConfig.<MovingAverageConfig>builder().config(MovingAverageConfig.builder().build()).build();
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AdaptiveBulkheadConfig.Builder}
	 */
	public static <T> Builder<T> builder() {
		return new Builder<>();
	}

	/**
	 * Returns a builder to create a custom AdaptiveBulkheadConfig.
	 *
	 * @return a {@link AdaptiveBulkheadConfig.Builder}
	 */
	public static <T> Builder<T> builder(AdaptiveBulkheadConfig<T> bulkheadConfig) {
		return new Builder<>(bulkheadConfig);
	}

	public static class Builder<T> {
		private AdaptiveBulkheadConfig<T> adaptiveBulkheadConfig;

		private Builder() {
			adaptiveBulkheadConfig = new AdaptiveBulkheadConfig<>();
		}

		private Builder(AdaptiveBulkheadConfig<T> adaptiveBulkheadConfig) {
			this.adaptiveBulkheadConfig = adaptiveBulkheadConfig;
		}

		public Builder<T> adaptIfError(Predicate<Exception> adaptIfError) {
			adaptiveBulkheadConfig.adaptIfError = adaptIfError;
			return this;
		}

		public Builder<T> config(T config) {
			adaptiveBulkheadConfig.config = config;
			return this;
		}

		public Builder<T> config(int initialConcurrency) {
			adaptiveBulkheadConfig.initialConcurrency = initialConcurrency;
			return this;
		}

		public AdaptiveBulkheadConfig<T> build() {
			if (adaptiveBulkheadConfig.getConfiguration() != null) {
				return adaptiveBulkheadConfig;
			} else {
				throw new IllegalArgumentException("target config object can not be NULL");
			}

		}


	}

}
