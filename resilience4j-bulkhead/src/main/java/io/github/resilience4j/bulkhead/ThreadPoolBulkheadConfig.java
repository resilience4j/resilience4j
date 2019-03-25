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
 * A {@link ThreadPoolBulkheadConfig} configures a {@link Bulkhead}
 */
public class ThreadPoolBulkheadConfig {

    public static final int DEFAULT_QUEUE_CAPACITY = 100;
    public static final long DEFAULT_MAX_WAIT_TIME = 0L;
    public static final int DEFAULT_CORE_THREAD_POOL_SIZE = 2;
    public static final int DEFAULT_MAX_THREAD_POOL_SIZE = 25;

    private int maxThreadPoolSize = DEFAULT_MAX_THREAD_POOL_SIZE;
    private int coreThreadPoolSize = DEFAULT_CORE_THREAD_POOL_SIZE;
    private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
    private long maxWaitTime = DEFAULT_MAX_WAIT_TIME;

    private ThreadPoolBulkheadConfig() { }

    public long getMaxWaitTime() {
        return maxWaitTime;
    }

    /**
     * Returns a builder to create a custom BulkheadConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder custom(){
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

        private ThreadPoolBulkheadConfig config = new ThreadPoolBulkheadConfig();

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
         * Configures a maximum amount of time in ms the calling thread will wait to enter the bulkhead. If bulkhead has space available, entry
         * is guaranteed and immediate. If bulkhead is full, calling threads will contest for space, if it becomes available. maxWaitTime can be set to 0.
         *
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
        public ThreadPoolBulkheadConfig build() {
            if (config.maxThreadPoolSize < config.coreThreadPoolSize) {
                throw new IllegalArgumentException("maxThreadPoolSize must be a greater than or equals to coreThreadPoolSize");
            }
            return config;
        }
    }
}
