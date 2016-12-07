/*
 *
 *  Copyright 2016 Robert Winkler and Bohdan Storozhuk
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
package io.github.robwin.ratelimiter;

import io.github.robwin.ratelimiter.internal.AtomicRateLimiter;
import javaslang.control.Try;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A RateLimiter distributes permits at a configurable rate. {@link #getPermission} blocks if necessary
 * until a permit is available, and then takes it. Once acquired, permits need not be released.
 */
public interface RateLimiter {

    /**
     * Creates a RateLimiter with a custom RateLimiter configuration.
     *
     * @param name              the name of the RateLimiter
     * @param rateLimiterConfig a custom RateLimiter configuration
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, RateLimiterConfig rateLimiterConfig) {
        return new AtomicRateLimiter(name, rateLimiterConfig);
    }

    /**
     * Creates a RateLimiter with a custom RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiter
     * @param rateLimiterConfigSupplier a supplier of a custom RateLimiterConfig configuration
     * @return The {@link RateLimiter}
     */
    static RateLimiter of(String name, Supplier<RateLimiterConfig> rateLimiterConfigSupplier) {
        return new AtomicRateLimiter(name, rateLimiterConfigSupplier.get());
    }

    /**
     * Creates a RateLimiter with a default RateLimiterConfig configuration.
     *
     * @param name                      the name of the RateLimiter
     * @return The {@link RateLimiter}
     */
    static RateLimiter ofDefaults(String name) {
        return new AtomicRateLimiter(name, RateLimiterConfig.ofDefaults());
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param supplier    the original supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> Try.CheckedSupplier<T> decorateCheckedSupplier(RateLimiter rateLimiter, Try.CheckedSupplier<T> supplier) {
        return () -> {
            waitForPermission(rateLimiter);
            return supplier.get();
        };
    }

    /**
     * Creates a runnable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a RateLimiter.
     */
    static Try.CheckedRunnable decorateCheckedRunnable(RateLimiter rateLimiter, Try.CheckedRunnable runnable) {

        return () -> {
            waitForPermission(rateLimiter);
            runnable.run();
        };
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param function    the original function
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> Try.CheckedFunction<T, R> decorateCheckedFunction(RateLimiter rateLimiter, Try.CheckedFunction<T, R> function) {
        return (T t) -> {
            waitForPermission(rateLimiter);
            return function.apply(t);
        };
    }

    /**
     * Creates a supplier which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param supplier    the original supplier
     * @return a supplier which is restricted by a RateLimiter.
     */
    static <T> Supplier<T> decorateSupplier(RateLimiter rateLimiter, Supplier<T> supplier) {
        return () -> {
            waitForPermission(rateLimiter);
            return supplier.get();
        };
    }

    /**
     * Creates a consumer which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param consumer    the original consumer
     * @return a consumer which is restricted by a RateLimiter.
     */
    static <T> Consumer<T> decorateConsumer(RateLimiter rateLimiter, Consumer<T> consumer) {
        return (T t) -> {
            waitForPermission(rateLimiter);
            consumer.accept(t);
        };
    }

    /**
     * Creates a runnable which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param runnable    the original runnable
     * @return a runnable which is restricted by a RateLimiter.
     */
    static Runnable decorateRunnable(RateLimiter rateLimiter, Runnable runnable) {
        return () -> {
            waitForPermission(rateLimiter);
            runnable.run();
        };
    }

    /**
     * Creates a function which is restricted by a RateLimiter.
     *
     * @param rateLimiter the RateLimiter
     * @param function    the original function
     * @return a function which is restricted by a RateLimiter.
     */
    static <T, R> Function<T, R> decorateFunction(RateLimiter rateLimiter, Function<T, R> function) {
        return (T t) -> {
            waitForPermission(rateLimiter);
            return function.apply(t);
        };
    }


    /**
     * Will wait for permission within default timeout duration.
     *
     * @param rateLimiter the RateLimiter to get permission from
     * @throws RequestNotPermitted if waiting time elapsed before a permit was acquired.
     * @throws IllegalStateException if thread was interrupted during permission wait
     */
    static void waitForPermission(final RateLimiter rateLimiter) throws IllegalStateException, RequestNotPermitted {
        RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        Duration timeoutDuration = rateLimiterConfig.getTimeoutDuration();
        boolean permission = rateLimiter.getPermission(timeoutDuration);
        if (Thread.interrupted()) {
            throw new IllegalStateException("Thread was interrupted during permission wait");
        }
        if (!permission) {
            throw new RequestNotPermitted("Request not permitted for limiter: " + rateLimiter.getName());
        }
    }

    /**
     * Acquires a permission from this rate limiter, blocking until one is
     * available.
     * <p>
     * <p>If the current thread is {@linkplain Thread#interrupt interrupted}
     * while waiting for a permit then it won't throw {@linkplain InterruptedException},
     * but its interrupt status will be set.
     *
     * @return {@code true} if a permit was acquired and {@code false}
     * if waiting time elapsed before a permit was acquired
     */
    boolean getPermission(Duration timeoutDuration);

    /**
     * Get the name of this RateLimiter
     *
     * @return the name of this RateLimiter
     */
    String getName();

    /**
     * Get the RateLimiterConfig of this RateLimiter.
     *
     * @return the RateLimiterConfig of this RateLimiter
     */
    RateLimiterConfig getRateLimiterConfig();

    /**
     * Get the Metrics of this RateLimiter.
     *
     * @return the Metrics of this RateLimiter
     */
    Metrics getMetrics();

    interface Metrics {
        /**
         * Returns an estimate of the number of threads waiting for permission
         * in this JVM process.
         * <p>This method is typically used for debugging and testing purposes.
         *
         * @return estimate of the number of threads waiting for permission.
         */
        int getNumberOfWaitingThreads();

        /**
         * Estimates count of available permissions.
         * Can be negative if some permissions where reserved.
         * <p>This method is typically used for debugging and testing purposes.
         *
         * @return estimated count of permissions
         */
        int getAvailablePermissions();
    }
}
