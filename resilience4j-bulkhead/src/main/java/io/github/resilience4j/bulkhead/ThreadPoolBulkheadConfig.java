/*
 *
 *  Copyright 2016 Robert Winkler, Lucas Lech, Mahmoud Romeh
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
 * A {@link ThreadPoolBulkheadConfig} configures a {@link Bulkhead}
 */
public class ThreadPoolBulkheadConfig {

	public static final int DEFAULT_QUEUE_CAPACITY = 100;
	public static final long DEFAULT_KEEP_ALIVE_TIME = 20L;
	public static final int DEFAULT_CORE_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() > 1 ? Runtime.getRuntime().availableProcessors() - 1 : 1;
	public static final int DEFAULT_MAX_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();

	private int maxThreadPoolSize = DEFAULT_MAX_THREAD_POOL_SIZE;
	private int coreThreadPoolSize = DEFAULT_CORE_THREAD_POOL_SIZE;
	private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
	private long keepAliveTime = DEFAULT_KEEP_ALIVE_TIME;

	private ThreadPoolBulkheadConfig() {
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
	public static ThreadPoolBulkheadConfig ofDefaults() {
		return new Builder().build();
	}

	public long getKeepAliveTime() {
		return keepAliveTime;
	}

	public int getQueueCapacity() {
		return queueCapacity;
	}

	public int getMaxThreadPoolSize() {
		return maxThreadPoolSize;
	}

	public int getCoreThreadPoolSize() {
		return coreThreadPoolSize;
	}

	public static class Builder {

		private final ThreadPoolBulkheadConfig config = new ThreadPoolBulkheadConfig();

		/**
		 * Configures the max thread pool size.
		 *
		 * @param maxThreadPoolSize max thread pool size
		 * @return the BulkheadConfig.Builder
		 */
		public Builder maxThreadPoolSize(int maxThreadPoolSize) {
			if (maxThreadPoolSize < 1) {
				throw new IllegalArgumentException("maxThreadPoolSize must be a positive integer value >= 1");
			}
			config.maxThreadPoolSize = maxThreadPoolSize;
			return this;
		}

		/**
		 * Configures the core thread pool size.
		 *
		 * @param coreThreadPoolSize core thread pool size
		 * @return the BulkheadConfig.Builder
		 */
		public Builder coreThreadPoolSize(int coreThreadPoolSize) {
			if (coreThreadPoolSize < 1) {
				throw new IllegalArgumentException("coreThreadPoolSize must be a positive integer value >= 1");
			}
			config.coreThreadPoolSize = coreThreadPoolSize;
			return this;
		}

		/**
		 * Configures the capacity of the queue.
		 *
		 * @param queueCapacity max concurrent calls
		 * @return the BulkheadConfig.Builder
		 */
		public Builder queueCapacity(int queueCapacity) {
			if (queueCapacity < 1) {
				throw new IllegalArgumentException("queueCapacity must be a positive integer value >= 1");
			}
			config.queueCapacity = queueCapacity;
			return this;
		}

		/**
		 * when the number of threads is greater than
		 * the core, this is the maximum time that excess idle threads
		 * will wait for new tasks before terminating.
		 *
		 * @param keepAliveTime maximum wait time for bulkhead thread pool idle thread
		 * @return the BulkheadConfig.Builder
		 */
		public Builder keepAliveTime(long keepAliveTime) {
			if (keepAliveTime < 0) {
				throw new IllegalArgumentException("keepAliveTime must be a positive integer value >= 0");
			}
			config.keepAliveTime = keepAliveTime;
			return this;
		}

		/**
		 * Builds a BulkheadConfig
		 *
		 * @return the BulkheadConfig
		 */
		public ThreadPoolBulkheadConfig build() {
			if (config.maxThreadPoolSize < config.coreThreadPoolSize) {
				throw new IllegalArgumentException("maxThreadPoolSize must be a greater than or equals to coreThreadPoolSize");
			}
			return config;
		}
	}
}
