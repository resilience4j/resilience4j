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

import io.github.resilience4j.core.lang.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.github.resilience4j.core.ClassUtils.instantiateClassDefConstructor;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * A {@link ThreadPoolBulkheadConfig} configures a {@link Bulkhead}
 */
public class ThreadPoolBulkheadConfig {

    public static final int DEFAULT_QUEUE_CAPACITY = 100;
    public static final Duration DEFAULT_KEEP_ALIVE_DURATION = Duration.ofMillis(20);
    public static final int DEFAULT_CORE_THREAD_POOL_SIZE =
        Runtime.getRuntime().availableProcessors() > 1 ? Runtime.getRuntime().availableProcessors()
            - 1 : 1;
    public static final int DEFAULT_MAX_THREAD_POOL_SIZE = Runtime.getRuntime()
        .availableProcessors();
    public static final boolean DEFAULT_WRITABLE_STACK_TRACE_ENABLED = true;

    private int maxThreadPoolSize = DEFAULT_MAX_THREAD_POOL_SIZE;
    private int coreThreadPoolSize = DEFAULT_CORE_THREAD_POOL_SIZE;
    private int queueCapacity = DEFAULT_QUEUE_CAPACITY;
    private Duration keepAliveDuration = DEFAULT_KEEP_ALIVE_DURATION;
    private boolean writableStackTraceEnabled = DEFAULT_WRITABLE_STACK_TRACE_ENABLED;
    private List<? extends ContextPropagator> contextPropagators = new ArrayList<>();

    private ThreadPoolBulkheadConfig() {
    }

    /**
     * Returns a builder to create a custom ThreadPoolBulkheadConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder custom() {
        return new Builder();
    }

    /**
     * Returns a builder to create a custom ThreadPoolBulkheadConfig.
     *
     * @return a {@link Builder}
     */
    public static Builder from(ThreadPoolBulkheadConfig threadPoolBulkheadConfig) {
        return new Builder(threadPoolBulkheadConfig);
    }

    /**
     * Creates a default Bulkhead configuration.
     *
     * @return a default Bulkhead configuration.
     */
    public static ThreadPoolBulkheadConfig ofDefaults() {
        return new Builder().build();
    }

    public Duration getKeepAliveDuration() {
        return keepAliveDuration;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getMaxThreadPoolSize() {
        return maxThreadPoolSize;
    }

    public int getCoreThreadPoolSize() { return coreThreadPoolSize; }

    public boolean isWritableStackTraceEnabled() {
        return writableStackTraceEnabled;
    }

    public List<? extends ContextPropagator> getContextPropagator() {
        return contextPropagators;
    }

    public static class Builder {
        private Class<? extends ContextPropagator>[] contextPropagatorClasses = new Class[0];
        private List<? extends ContextPropagator> contextPropagators = new ArrayList<>();
        private ThreadPoolBulkheadConfig config;

        public Builder(ThreadPoolBulkheadConfig bulkheadConfig) {
            this.config = bulkheadConfig;
        }

        public Builder() {
            config = new ThreadPoolBulkheadConfig();
        }

        /**
         * Configures the max thread pool size.
         *
         * @param maxThreadPoolSize max thread pool size
         * @return the BulkheadConfig.Builder
         */
        public Builder maxThreadPoolSize(int maxThreadPoolSize) {
            if (maxThreadPoolSize < 1) {
                throw new IllegalArgumentException(
                    "maxThreadPoolSize must be a positive integer value >= 1");
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
                throw new IllegalArgumentException(
                    "coreThreadPoolSize must be a positive integer value >= 1");
            }
            config.coreThreadPoolSize = coreThreadPoolSize;
            return this;
        }

        /**
         * Configures the context propagator class.
         *
         * @return the BulkheadConfig.Builder
         */
        public final Builder contextPropagator(
            @Nullable Class<? extends ContextPropagator>... contextPropagatorClasses) {
            this.contextPropagatorClasses = contextPropagatorClasses != null
                ? contextPropagatorClasses
                : new Class[0];
            return this;
        }

        public final Builder contextPropagator(ContextPropagator... contextPropagators) {
            this.contextPropagators = contextPropagators != null ?
                Arrays.stream(contextPropagators).collect(toList()) :
                new ArrayList<>();
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
                throw new IllegalArgumentException(
                    "queueCapacity must be a positive integer value >= 1");
            }
            config.queueCapacity = queueCapacity;
            return this;
        }

        /**
         * When the number of threads is greater than the core, this is the maximum time duration
         * that excess idle threads will wait for new tasks before terminating.
         *
         * @param keepAliveDuration maximum wait duration for bulkhead thread pool idle thread
         * @return the BulkheadConfig.Builder
         */
        public Builder keepAliveDuration(Duration keepAliveDuration) {
            if (keepAliveDuration.toMillis() < 0) {
                throw new IllegalArgumentException(
                    "keepAliveDuration must be a positive integer value >= 0");
            }
            config.keepAliveDuration = keepAliveDuration;
            return this;
        }

        /**
         * Enables writable stack traces. When set to false, {@link Exception#getStackTrace()}
         * returns a zero length array. This may be used to reduce log spam when the circuit breaker
         * is open as the cause of the exceptions is already known (the circuit breaker is
         * short-circuiting calls).
         *
         * @param writableStackTraceEnabled flag to control if stack trace is writable
         * @return the BulkheadConfig.Builder
         */
        public Builder writableStackTraceEnabled(boolean writableStackTraceEnabled) {
            config.writableStackTraceEnabled = writableStackTraceEnabled;
            return this;
        }

        /**
         * Builds a BulkheadConfig
         *
         * @return the BulkheadConfig
         */
        public ThreadPoolBulkheadConfig build() {
            if (config.maxThreadPoolSize < config.coreThreadPoolSize) {
                throw new IllegalArgumentException(
                    "maxThreadPoolSize must be a greater than or equals to coreThreadPoolSize");
            }
            if (contextPropagatorClasses.length > 0) {
                config.contextPropagators.addAll((List)stream(contextPropagatorClasses)
                    .map(c -> instantiateClassDefConstructor(c))
                    .collect(toList()));
            }
            //setting bean of type context propagator overrides the class type.
            if (contextPropagators.size() > 0){
                config.contextPropagators.addAll((List)this.contextPropagators);
            }

            return config;
        }
    }
}
