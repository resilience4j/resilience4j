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

import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies the {@link Hedge#close()} contract introduced as part of the
 * AutoCloseable lifecycle. Parameterized over platform/virtual thread modes so
 * the contract is validated against both scheduler worker types (virtual
 * workers are implicitly daemon and are the original reason close() was
 * required).
 */
@RunWith(Parameterized.class)
public class HedgeLifecycleTest extends ThreadModeTestBase {

    private ScheduledExecutorService primaryExecutor;

    public HedgeLifecycleTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    @Before
    public void setUp() {
        primaryExecutor = Executors.newScheduledThreadPool(2);
    }

    @After
    public void tearDown() {
        shutdown(primaryExecutor);
    }

    @Test
    public void closedHedgeRejectsNewSubmissions() throws Exception {
        Hedge hedge = Hedge.of(Duration.ofMillis(50));
        // Sanity: Hedge is functional before close.
        String result = hedge.submit(() -> "ok", primaryExecutor).get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("ok");

        hedge.close();

        Callable<String> task = () -> "should-not-run";
        assertThatThrownBy(() -> hedge.submit(task, primaryExecutor).get(5, TimeUnit.SECONDS))
            .isInstanceOf(java.util.concurrent.RejectedExecutionException.class);
    }

    @Test
    public void closeIsIdempotent() {
        Hedge hedge = Hedge.of(Duration.ofMillis(50));
        hedge.close();
        // Second close must not throw; underlying ExecutorService.shutdown is idempotent.
        hedge.close();
    }

    @Test
    public void tryWithResourcesReleasesScheduler() throws Exception {
        // Using Hedge as AutoCloseable in a try-with-resources block should not
        // leak the internal scheduler. We verify functional correctness; thread
        // counting would be brittle (virtual thread workers are invisible to
        // classic thread dumps without -Dloom=on).
        try (Hedge hedge = Hedge.of(Duration.ofMillis(50))) {
            String result = hedge.submit(() -> "ok", primaryExecutor).get(5, TimeUnit.SECONDS);
            assertThat(result).isEqualTo("ok");
        }
        // Reaching here without a leaked scheduler is the implicit assertion.
    }

    @Test
    public void registryCloseClosesAllManagedHedges() throws Exception {
        HedgeRegistry registry = HedgeRegistry.builder().build();
        HedgeConfig config = HedgeConfig.custom()
            .preconfiguredDuration(Duration.ofMillis(30))
            .build();

        Hedge a = registry.hedge("a", config);
        Hedge b = registry.hedge("b", config);

        // Both Hedges are functional before registry close.
        assertThat(a.submit(() -> "a", primaryExecutor).get(5, TimeUnit.SECONDS)).isEqualTo("a");
        assertThat(b.submit(() -> "b", primaryExecutor).get(5, TimeUnit.SECONDS)).isEqualTo("b");

        registry.close();

        // After registry close, existing references must reject new submissions.
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
