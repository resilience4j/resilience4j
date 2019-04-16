/*
 *
 *  Copyright 2016 Robert Winkler, Lucas Lech
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
package io.github.resilience4j.bulkhead;

/**
 * A {@link BulkheadConfig} configures a {@link Bulkhead}
 */
public class BulkheadConfig {

	public static final int DEFAULT_MAX_CONCURRENT_CALLS = 25;
	public static final long DEFAULT_MAX_WAIT_TIME = 0L;

	private int maxConcurrentCalls = DEFAULT_MAX_CONCURRENT_CALLS;
	private long maxWaitTime = DEFAULT_MAX_WAIT_TIME;

	private BulkheadConfig() {
	}

	/**
	 * Returns a builder to create a custom BulkheadConfig.
	 *
	 * @return a {@link Builder}
	 */
	public static Builder custom() {
		return new Builder();
	}

	/**
	 * Creates a default Bulkhead configuration.
	 *
	 * @return a default Bulkhead configuration.
	 */
	public static BulkheadConfig ofDefaults() {
		return new Builder().build();
	}

	public int getMaxConcurrentCalls() {
		return maxConcurrentCalls;
	}

	public long getMaxWaitTime() {
		return maxWaitTime;
	}

	public static class Builder {

		private BulkheadConfig config = new BulkheadConfig();

		/**
		 * Configures the max amount of concurrent calls the bulkhead will support.
		 *
		 * @param maxConcurrentCalls max concurrent calls
		 * @return the BulkheadConfig.Builder
		 */
		public Builder maxConcurrentCalls(int maxConcurrentCalls) {
			if (maxConcurrentCalls < 1) {
				throw new IllegalArgumentException("maxConcurrentCalls must be a positive integer value >= 1");
			}
			config.maxConcurrentCalls = maxConcurrentCalls;
			return this;
		}

		/**
		 * Configures a maximum amount of time in ms the calling thread will wait to enter the bulkhead. If bulkhead has space available, entry
		 * is guaranteed and immediate. If bulkhead is full, calling threads will contest for space, if it becomes available. maxWaitTime can be set to 0.
		 * <p>
		 * Note: for threads running on an event-loop or equivalent (rx computation pool, etc), setting maxWaitTime to 0 is highly recommended. Blocking
		 * an event-loop thread will most likely have a negative effect on application throughput.
		 *
		 * @param maxWaitTime maximum wait time for bulkhead entry
		 * @return the BulkheadConfig.Builder
		 */
		public Builder maxWaitTime(long maxWaitTime) {
			if (maxWaitTime < 0) {
				throw new IllegalArgumentException("maxWaitTime must be a positive integer value >= 0");
			}
			config.maxWaitTime = maxWaitTime;
			return this;
		}

		/**
		 * Builds a BulkheadConfig
		 *
		 * @return the BulkheadConfig
		 */
		public BulkheadConfig build() {
			return config;
		}
	}
}
