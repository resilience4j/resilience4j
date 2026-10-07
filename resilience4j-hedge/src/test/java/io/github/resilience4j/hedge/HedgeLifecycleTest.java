/*
 * Copyright 2026 kanghyun.yang
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
package io.github.resilience4j.hedge;

import io.github.resilience4j.core.ThreadModeExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the {@link Hedge#close()} contract introduced as part of the
 * AutoCloseable lifecycle. Parameterized over platform/virtual thread modes so
 * the contract is validated against both scheduler worker types.
 */
@ExtendWith(ThreadModeExtension.class)
class HedgeLifecycleTest {

    private ScheduledExecutorService primaryExecutor;

    @BeforeEach
    void setUp() {
        primaryExecutor = Executors.newScheduledThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        shutdown(primaryExecutor);
    }

    @TestTemplate
    void closedHedgeRejectsNewSubmissions() throws Exception {
        Hedge hedge = Hedge.of(Duration.ofMillis(50));
        String result = hedge.submit(() -> "ok", primaryExecutor).get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("ok");

        hedge.close();

        Callable<String> task = () -> "should-not-run";
        assertThatThrownBy(() -> hedge.submit(task, primaryExecutor).get(5, TimeUnit.SECONDS))
            .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
    }

    @TestTemplate
    void closeCancelsPendingTriggerWithoutFailingInFlightPrimary() throws Exception {
        Hedge hedge = Hedge.of(Duration.ofMillis(100));
        CountDownLatch primaryStarted = new CountDownLatch(1);
        CountDownLatch allowPrimaryToComplete = new CountDownLatch(1);
        AtomicInteger invocations = new AtomicInteger();

        CompletableFuture<String> future = hedge.submit(() -> {
            invocations.incrementAndGet();
            primaryStarted.countDown();
            if (!allowPrimaryToComplete.await(5, TimeUnit.SECONDS)) {
                throw new TimeoutException("Primary was not released");
            }
            return "primary";
        }, primaryExecutor);

        try {
            assertThat(primaryStarted.await(5, TimeUnit.SECONDS)).isTrue();

            hedge.close();
            Thread.sleep(250);

            assertThat(future.isDone()).isFalse();

            allowPrimaryToComplete.countDown();
            assertThat(future.get(5, TimeUnit.SECONDS)).isEqualTo("primary");
            assertThat(invocations.get()).isEqualTo(1);
        } finally {
            allowPrimaryToComplete.countDown();
            hedge.close();
        }
    }

    @TestTemplate
    void closeIsIdempotent() {
        Hedge hedge = Hedge.of(Duration.ofMillis(50));
        hedge.close();
        hedge.close();
    }

    @TestTemplate
    void tryWithResourcesReleasesScheduler() throws Exception {
        try (Hedge hedge = Hedge.of(Duration.ofMillis(50))) {
            String result = hedge.submit(() -> "ok", primaryExecutor).get(5, TimeUnit.SECONDS);
            assertThat(result).isEqualTo("ok");
        }
    }

    @TestTemplate
    void registryCloseClosesAllManagedHedges() throws Exception {
        HedgeRegistry registry = HedgeRegistry.builder().build();
        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredDuration(Duration.ofMillis(30))
            .build();

        Hedge a = registry.hedge("a", config);
        Hedge b = registry.hedge("b", config);

        assertThat(a.submit(() -> "a", primaryExecutor).get(5, TimeUnit.SECONDS)).isEqualTo("a");
        assertThat(b.submit(() -> "b", primaryExecutor).get(5, TimeUnit.SECONDS)).isEqualTo("b");

        registry.close();

        assertThatThrownBy(() -> a.submit(() -> "x", primaryExecutor).get(5, TimeUnit.SECONDS))
            .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
        assertThatThrownBy(() -> b.submit(() -> "y", primaryExecutor).get(5, TimeUnit.SECONDS))
            .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
    }

    private static void shutdown(ExecutorService executor) {
        if (executor == null || executor.isShutdown()) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
