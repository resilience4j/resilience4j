/*
 *
 *  Copyright 2026: Matthew Sandoz
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
package io.github.resilience4j.hedge;

import io.github.resilience4j.hedge.internal.HedgeImpl;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.then;

class HedgeTest {

    private static final String TEST_HEDGE = "TEST_HEDGE";

    @Test
    void shouldInitializeOfDefaults() {
        Hedge hedge = Hedge.ofDefaults();

        then(hedge.getHedgeConfig().toString()).isEqualTo(HedgeConfig.ofDefaults().toString());
        then(hedge.getName()).isEqualTo(HedgeImpl.DEFAULT_NAME);
    }

    @Test
    void shouldInitializeOfName() {
        Hedge hedge = Hedge.ofDefaults(TEST_HEDGE);

        then(hedge.getHedgeConfig().toString()).isEqualTo(HedgeConfig.ofDefaults().toString());
        then(hedge.getName()).isEqualTo(TEST_HEDGE);
    }

    @Test
    void shouldInitializeOfNameAndDefaults() {
        Hedge hedge = Hedge.of(TEST_HEDGE, HedgeConfig.ofDefaults());

        then(hedge.getName()).isEqualTo(TEST_HEDGE);
    }

    @Test
    void shouldInitializeConfig() {
        HedgeConfig config = HedgeConfig.ofDefaults();
        Hedge hedge = Hedge.of(config);

        then(hedge.getHedgeConfig()).isEqualTo(config);
    }

    @Test
    void shouldUseCustomScheduledThreadPoolExecutorFromRegistry() throws Exception {
        ScheduledThreadPoolExecutor customHedgeExecutor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "custom-hedge-pool-thread");
            thread.setDaemon(true);
            return thread;
        });
        ExecutorService primaryExecutor = Executors.newSingleThreadExecutor();
        try {
            HedgeConfig config = HedgeConfig.custom()
                .preconfiguredDuration(Duration.ofMillis(20))
                .withExecutorService(customHedgeExecutor)
                .build();
            HedgeRegistry registry = HedgeRegistry.builder().withDefaultConfig(config).build();
            Hedge hedge = registry.hedge("custom-executor-hedge");

            assertThat(hedge.getMetrics().getSecondaryPoolActiveCount()).isGreaterThanOrEqualTo(0);

            CountDownLatch primaryRelease = new CountDownLatch(1);
            AtomicInteger callCount = new AtomicInteger(0);
            AtomicReference<String> secondaryThreadName = new AtomicReference<>();

            CompletableFuture<String> future = hedge.submit(() -> {
                if (callCount.getAndIncrement() == 0) {
                    primaryRelease.await(2, TimeUnit.SECONDS);
                    return "primary";
                }
                secondaryThreadName.set(Thread.currentThread().getName());
                return "hedged";
            }, primaryExecutor);

            assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo("hedged");
            assertThat(secondaryThreadName.get()).isEqualTo("custom-hedge-pool-thread");
            assertThat(hedge.getMetrics().getSecondarySuccessCount()).isOne();
            primaryRelease.countDown();
        } finally {
            primaryExecutor.shutdownNow();
            customHedgeExecutor.shutdownNow();
        }
    }

    @Test
    void shouldUseWrappedScheduledExecutorServiceAndReturnMinusOneActiveCount() throws Exception {
        ScheduledExecutorService delegate = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger scheduleCalls = new AtomicInteger(0);
        AtomicInteger executeCalls = new AtomicInteger(0);
        ScheduledExecutorService wrappedExecutor = new DelegatingScheduledExecutorService(
            delegate, scheduleCalls, executeCalls);
        ExecutorService primaryExecutor = Executors.newSingleThreadExecutor();
        try {
            Hedge hedgeFromDuration = Hedge.of(Duration.ofMillis(20), wrappedExecutor);
            assertThat(hedgeFromDuration.getHedgeConfig().getExecutorService()).isSameAs(wrappedExecutor);
            assertThat(hedgeFromDuration.getMetrics().getSecondaryPoolActiveCount()).isEqualTo(-1);

            Hedge hedgeFromFactory = Hedge.of(
                "wrapped-hedge",
                HedgeConfig.custom().preconfiguredDuration(Duration.ofMillis(20)).build(),
                Collections.emptyMap(),
                wrappedExecutor
            );
            assertThat(hedgeFromFactory.getMetrics().getSecondaryPoolActiveCount()).isEqualTo(-1);

            CountDownLatch primaryRelease = new CountDownLatch(1);
            AtomicInteger callCount = new AtomicInteger(0);
            CompletableFuture<String> future = hedgeFromFactory.submit(() -> {
                if (callCount.getAndIncrement() == 0) {
                    primaryRelease.await(2, TimeUnit.SECONDS);
                    return "primary";
                }
                return "hedged";
            }, primaryExecutor);

            assertThat(future.get(2, TimeUnit.SECONDS)).isEqualTo("hedged");
            assertThat(scheduleCalls.get()).isGreaterThanOrEqualTo(1);
            assertThat(executeCalls.get()).isGreaterThanOrEqualTo(1);
            primaryRelease.countDown();
        } finally {
            primaryExecutor.shutdownNow();
            delegate.shutdownNow();
        }
    }

    @Test
    void shouldWrapCheckedExceptionInCompletionExceptionOnPrimaryFailure() {
        Hedge hedge = Hedge.of(Duration.ofMillis(200));
        Supplier<CompletionStage<String>> supplier = () -> CompletableFuture.failedFuture(new IOException("primary checked"));

        CompletableFuture<String> result = hedge.decorateCompletionStage(supplier).get().toCompletableFuture();

        assertThatThrownBy(() -> result.get(2, TimeUnit.SECONDS))
            .isInstanceOf(ExecutionException.class)
            .hasCauseInstanceOf(IOException.class)
            .hasRootCauseMessage("primary checked");
        assertThat(hedge.getMetrics().getPrimaryFailureCount()).isOne();
    }

    @Test
    void shouldWrapCheckedExceptionInCompletionExceptionOnSecondaryFailure() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            Hedge hedge = Hedge.of(Duration.ofMillis(20), executor);
            AtomicInteger callCount = new AtomicInteger(0);
            Supplier<CompletionStage<String>> supplier = () -> {
                if (callCount.getAndIncrement() == 0) {
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                        return "slow-primary";
                    });
                }
                return CompletableFuture.failedFuture(new IOException("secondary checked"));
            };

            CompletableFuture<String> result = hedge.decorateCompletionStage(supplier).get().toCompletableFuture();

            assertThatThrownBy(() -> result.get(2, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasRootCauseMessage("secondary checked");
            assertThat(hedge.getMetrics().getSecondaryFailureCount()).isOne();
        } finally {
            executor.shutdownNow();
        }
    }

    private static final class DelegatingScheduledExecutorService implements ScheduledExecutorService {

        private final ScheduledExecutorService delegate;
        private final AtomicInteger scheduleCalls;
        private final AtomicInteger executeCalls;

        private DelegatingScheduledExecutorService(ScheduledExecutorService delegate,
                                                   AtomicInteger scheduleCalls,
                                                   AtomicInteger executeCalls) {
            this.delegate = delegate;
            this.scheduleCalls = scheduleCalls;
            this.executeCalls = executeCalls;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            scheduleCalls.incrementAndGet();
            return delegate.schedule(command, delay, unit);
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            scheduleCalls.incrementAndGet();
            return delegate.schedule(callable, delay, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return delegate.scheduleAtFixedRate(command, initialDelay, period, unit);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
            return delegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return delegate.submit(task);
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return delegate.submit(task, result);
        }

        @Override
        public Future<?> submit(Runnable task) {
            return delegate.submit(task);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        @Override
        public void execute(Runnable command) {
            executeCalls.incrementAndGet();
            delegate.execute(command);
        }
    }

}
