/*
 * Copyright 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.core;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central factory responsible for creating {@link ScheduledExecutorService} and other
 * {@link java.util.concurrent.ExecutorService} instances used internally by Resilience4j.
 * <p>
 * The factory supports choosing between a classic platform thread based executor and
 * a JDK <strong>Virtual Thread</strong> based executor (Project Loom, JDK 21+).
 * <p>
 * Selection rule:
 * <ol>
 *     <li>If the JVM system property {@code resilience4j.thread.type} is set to
 *     {@code \"virtual\"} or {@code \"platform\"} that value wins.</li>
 *     <li>Otherwise, the factory falls back to the value of the environment variable
 *     {@code RESILIENCE4J_THREAD_TYPE}.</li>
 *     <li>If still not specified, the default is {@code platform}.</li>
 * </ol>
 * <p>
 * Spring Boot 3 integration modules may override this factory by registering a
 * {@code ExecutorServiceFactoryCustomizer} Bean in order to read {@code application.yml}.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
public final class ExecutorServiceFactory {

    private static final String SYS_PROP_KEY     = "resilience4j.thread.type";
    private static final String ENV_VAR_KEY      = "RESILIENCE4J_THREAD_TYPE";

    private static final AtomicInteger POOL_ID   = new AtomicInteger();

    private ExecutorServiceFactory() { }

    /**
     * Returns the configured thread type.
     * 
     * @return the thread type based on system property, environment variable, or default
     */
    public static ThreadType getThreadType() {
        String value = System.getProperty(SYS_PROP_KEY);
        if (value == null) {
            value = System.getenv(ENV_VAR_KEY);
        }
        return ThreadType.fromStringOrDefault(value, ThreadType.getDefault());
    }


    /**
     * Create a single–thread {@link ScheduledExecutorService}.
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String poolName) {
        return newSingleThreadScheduledExecutor(poolName, getThreadType());
    }

    /**
     * Create a single–thread {@link ScheduledExecutorService} with the specified thread type.
     */
    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String poolName, ThreadType threadType) {
        if (threadType == ThreadType.VIRTUAL) {
            /*
             * The JDK currently does not ship a dedicated virtual-thread scheduler factory
             * (java.util.concurrent.Executors API is still incubating).
             * We therefore create a scheduled executor backed by a virtual-thread per task
             * executor.  Implementation detail: use {@code Runnable::run} as the thread
             * factory and rely on virtual threads for actual concurrency.
             */
            return Executors.newSingleThreadScheduledExecutor(newVirtualThreadFactory(poolName));
        } else {
            return Executors.newSingleThreadScheduledExecutor(newPlatformThreadFactory(poolName));
        }
    }

    /**
     * Create a {@link ScheduledExecutorService} with the given pool size.
     */
    public static ScheduledExecutorService newScheduledThreadPool(int size, String poolName) {
        return newScheduledThreadPool(size, poolName, getThreadType());
    }

    /**
     * Create a {@link ScheduledExecutorService} with the given pool size and thread type.
     */
    public static ScheduledExecutorService newScheduledThreadPool(int size, String poolName, ThreadType threadType) {
        if (threadType == ThreadType.VIRTUAL) {
            return Executors.newScheduledThreadPool(size, newVirtualThreadFactory(poolName));
        } else {
            return Executors.newScheduledThreadPool(size, newPlatformThreadFactory(poolName));
        }
    }

    /* ──────────────────────────────
     *  ThreadFactory helpers
     * ────────────────────────────── */
    private static ThreadFactory newVirtualThreadFactory(String poolName) {
        // JDK 21 virtual thread builder API
        return Thread.ofVirtual()
                     .name(poolName + "-vthread-",  /* start index */0)
                     .factory();
    }

    private static ThreadFactory newPlatformThreadFactory(String poolName) {
        return Thread.ofPlatform()
                    .name(poolName + "-thread-",  /* start index */0)
                    .daemon(true)
                    .factory();
    }

    /* ──────────────────────────────
     *  Utility
     * ────────────────────────────── */

    /**
     * Convenience helper that sleeps using the chosen executor so tests can
     * verify behaviour irrespective of thread type.
     */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
