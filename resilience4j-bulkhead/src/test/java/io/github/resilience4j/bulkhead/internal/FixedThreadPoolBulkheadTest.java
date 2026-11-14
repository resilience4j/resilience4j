/*
 *
 *  Copyright 2026 Robert Winkler
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
package io.github.resilience4j.bulkhead.internal;


import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkheadConfig;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.InMemoryRegistryStore;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.test.TestContextPropagators;
import io.github.resilience4j.test.TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.TestThreadLocalContextHolder;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixedThreadPoolBulkheadTest {

    private ThreadPoolBulkhead bulkhead;
    private FixedThreadPoolBulkhead fixedThreadPoolBulkhead;

    @BeforeEach
    void setUp() {
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(2)
            .coreThreadPoolSize(1)
            .queueCapacity(10)
            .contextPropagator(TestContextPropagators.TestThreadLocalContextPropagatorWithHolder.class)
            .keepAliveDuration(Duration.ofMillis(10))
            .build();
        bulkhead = ThreadPoolBulkhead.of("test", config);
        fixedThreadPoolBulkhead = new FixedThreadPoolBulkhead("testPool", config);
    }

    @Test
    void supplierThreadLocalContextPropagator() {

        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");

        CompletableFuture<Object> future = fixedThreadPoolBulkhead
            .submit(() -> TestThreadLocalContextHolder.get().orElse(null));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(future).isCompletedWithValue("ValueShouldCrossThreadBoundary"));
    }

    @Test
    void runnableThreadLocalContextPropagator() {

        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");
        AtomicReference<String> reference = new AtomicReference<>();

        fixedThreadPoolBulkhead
            .submit(() -> reference.set((String) TestThreadLocalContextHolder.get().orElse(null)));

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(reference).hasValue("ValueShouldCrossThreadBoundary"));
    }

    @Test
    void testToString() {
        String result = bulkhead.toString();

        assertThat(result).isEqualTo("FixedThreadPoolBulkhead 'test'");
    }

    @Test
    void customSettings() {
        assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize()).isEqualTo(2);
        assertThat(bulkhead.getBulkheadConfig().getQueueCapacity()).isEqualTo(10);
        assertThat(bulkhead.getBulkheadConfig().getCoreThreadPoolSize()).isOne();
        assertThat(bulkhead.getBulkheadConfig().getKeepAliveDuration())
            .isEqualTo(Duration.ofMillis(10));
    }

    @Test
    void createWithDefaults() {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.ofDefaults("test");

        assertThat(bulkhead).isNotNull();
        assertThat(bulkhead.getBulkheadConfig()).isNotNull();
        assertThat(bulkhead.getBulkheadConfig().getMaxThreadPoolSize())
            .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_MAX_THREAD_POOL_SIZE);
        assertThat(bulkhead.getBulkheadConfig().getCoreThreadPoolSize())
            .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_CORE_THREAD_POOL_SIZE);
        assertThat(bulkhead.getBulkheadConfig().getQueueCapacity())
            .isEqualTo(ThreadPoolBulkheadConfig.DEFAULT_QUEUE_CAPACITY);
    }

    @Test
    void shouldCreateThreadPoolBulkheadRegistryWithRegistryStore() {
        RegistryEventConsumer<ThreadPoolBulkhead> registryEventConsumer = getNoOpsRegistryEventConsumer();
        List<RegistryEventConsumer<ThreadPoolBulkhead>> registryEventConsumers = new ArrayList<>();
        registryEventConsumers.add(registryEventConsumer);
        Map<String, ThreadPoolBulkheadConfig> configs = new HashMap<>();
        final ThreadPoolBulkheadConfig defaultConfig = ThreadPoolBulkheadConfig.ofDefaults();
        configs.put("default", defaultConfig);
        final InMemoryThreadPoolBulkheadRegistry inMemoryThreadPoolBulkheadRegistry =
            new InMemoryThreadPoolBulkheadRegistry(configs, registryEventConsumers,
                Map.of("Tag1", "Tag1Value"), new InMemoryRegistryStore<>());

        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry).isNotNull();
        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry.getDefaultConfig()).isEqualTo(defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry.getConfiguration("testNotFound")).isEmpty();
        inMemoryThreadPoolBulkheadRegistry.addConfiguration("testConfig", defaultConfig);
        AssertionsForClassTypes.assertThat(inMemoryThreadPoolBulkheadRegistry.getConfiguration("testConfig")).isNotNull();
    }

    @Test
    void shouldWrapInternalThreadPoolExecutorWithDecoratorAndPreserveMetrics() throws Exception {
        AtomicInteger decoratedExecutions = new AtomicInteger();
        ThreadLocal<String> threadLocal = new ThreadLocal<>();

        ThreadPoolBulkheadConfig decoratedConfig = ThreadPoolBulkheadConfig.custom()
            .coreThreadPoolSize(2)
            .maxThreadPoolSize(4)
            .queueCapacity(10)
            .executorServiceDecorator(delegate -> new RecordingContextExecutorService(
                delegate, decoratedExecutions, threadLocal))
            .build();

        try (ThreadPoolBulkhead decoratedBulkhead = ThreadPoolBulkhead.of("decorated", decoratedConfig)) {
            threadLocal.set("propagated-context");
            try {
                String result = decoratedBulkhead.submit(() -> threadLocal.get())
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
                assertThat(result).isEqualTo("propagated-context");

                AtomicReference<String> runnableResult = new AtomicReference<>();
                decoratedBulkhead.submit(() -> runnableResult.set(threadLocal.get()))
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
                assertThat(runnableResult).hasValue("propagated-context");
            } finally {
                threadLocal.remove();
            }

            assertThat(decoratedExecutions.get()).isEqualTo(2);

            ThreadPoolBulkhead.Metrics metrics = decoratedBulkhead.getMetrics();
            assertThat(metrics.getCoreThreadPoolSize()).isEqualTo(2);
            assertThat(metrics.getMaximumThreadPoolSize()).isEqualTo(4);
            assertThat(metrics.getQueueCapacity()).isEqualTo(10);
            assertThat(metrics.getRemainingQueueCapacity()).isEqualTo(10);
            assertThat(metrics.getThreadPoolSize()).isGreaterThanOrEqualTo(1);
            assertThat(metrics.getActiveThreadCount()).isGreaterThanOrEqualTo(0);
            assertThat(metrics.getAvailableThreadCount()).isBetween(0, 4);
        }
    }

    @Test
    void shouldUseCustomNonThreadPoolExecutorServiceAndReturnUnavailableMetrics() throws Exception {
        AtomicInteger customExecutions = new AtomicInteger();
        ExecutorService backingExecutor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "custom-non-tpe-worker"));
        ExecutorService nonThreadPoolExecutor = new DelegatingNonThreadPoolExecutorService(
            backingExecutor, customExecutions);

        ThreadPoolBulkheadConfig customConfig = ThreadPoolBulkheadConfig.custom()
            .executorService(nonThreadPoolExecutor)
            .build();

        List<BulkheadEvent.Type> events = new ArrayList<>();
        try (ThreadPoolBulkhead customBulkhead = ThreadPoolBulkhead.of("customExecutor", customConfig)) {
            customBulkhead.getEventPublisher().onCallPermitted(e -> events.add(e.getEventType()));
            customBulkhead.getEventPublisher().onCallFinished(e -> events.add(e.getEventType()));
            customBulkhead.getEventPublisher().onCallRejected(e -> events.add(e.getEventType()));

            String threadName = customBulkhead.submit(() -> Thread.currentThread().getName())
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
            assertThat(threadName).isEqualTo("custom-non-tpe-worker");

            AtomicReference<String> runnableThreadName = new AtomicReference<>();
            customBulkhead.submit(() -> runnableThreadName.set(Thread.currentThread().getName()))
                .toCompletableFuture()
                .get(5, TimeUnit.SECONDS);
            assertThat(runnableThreadName).hasValue("custom-non-tpe-worker");

            assertThat(customExecutions.get()).isEqualTo(2);
            assertThat(events).containsExactly(
                BulkheadEvent.Type.CALL_PERMITTED,
                BulkheadEvent.Type.CALL_FINISHED,
                BulkheadEvent.Type.CALL_PERMITTED,
                BulkheadEvent.Type.CALL_FINISHED);

            ThreadPoolBulkhead.Metrics metrics = customBulkhead.getMetrics();
            assertThat(metrics.getCoreThreadPoolSize()).isEqualTo(-1);
            assertThat(metrics.getThreadPoolSize()).isEqualTo(-1);
            assertThat(metrics.getMaximumThreadPoolSize()).isEqualTo(-1);
            assertThat(metrics.getQueueDepth()).isEqualTo(-1);
            assertThat(metrics.getRemainingQueueCapacity()).isEqualTo(-1);
            assertThat(metrics.getQueueCapacity()).isEqualTo(-1);
            assertThat(metrics.getActiveThreadCount()).isEqualTo(-1);
            assertThat(metrics.getAvailableThreadCount()).isEqualTo(-1);

            nonThreadPoolExecutor.shutdown();
            assertThatThrownBy(() -> customBulkhead.submit(() -> "rejected"))
                .isInstanceOf(BulkheadFullException.class);
            assertThat(events).endsWith(BulkheadEvent.Type.CALL_REJECTED);
        }
    }

    @Test
    void shouldApplyDecoratorOnTopOfCustomExecutorService() throws Exception {
        AtomicInteger decoratedExecutions = new AtomicInteger();
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        ExecutorService backingExecutor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "custom-decorated-worker"));

        ThreadPoolBulkheadConfig customConfig = ThreadPoolBulkheadConfig.custom()
            .executorService(new DelegatingNonThreadPoolExecutorService(backingExecutor, new AtomicInteger()))
            .executorServiceDecorator(delegate -> new RecordingContextExecutorService(
                delegate, decoratedExecutions, threadLocal))
            .build();

        try (ThreadPoolBulkhead customBulkhead = ThreadPoolBulkhead.of("customDecorated", customConfig)) {
            threadLocal.set("custom-context");
            try {
                String value = customBulkhead.submit(() -> threadLocal.get())
                    .toCompletableFuture()
                    .get(5, TimeUnit.SECONDS);
                assertThat(value).isEqualTo("custom-context");
            } finally {
                threadLocal.remove();
            }
            assertThat(decoratedExecutions.get()).isEqualTo(1);
            assertThat(customBulkhead.getMetrics().getCoreThreadPoolSize()).isEqualTo(-1);
        }
    }

    private static class DelegatingNonThreadPoolExecutorService implements ExecutorService {

        protected final ExecutorService delegate;
        private final AtomicInteger executionCounter;

        private DelegatingNonThreadPoolExecutorService(ExecutorService delegate,
            AtomicInteger executionCounter) {
            this.delegate = delegate;
            this.executionCounter = executionCounter;
        }

        @Override
        public void execute(Runnable command) {
            executionCounter.incrementAndGet();
            delegate.execute(command);
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
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout,
            TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }
    }

    private static final class RecordingContextExecutorService
        extends DelegatingNonThreadPoolExecutorService {

        private final ThreadLocal<String> threadLocal;

        private RecordingContextExecutorService(ExecutorService delegate,
            AtomicInteger executionCounter, ThreadLocal<String> threadLocal) {
            super(delegate, executionCounter);
            this.threadLocal = threadLocal;
        }

        @Override
        public void execute(Runnable command) {
            String captured = threadLocal.get();
            super.execute(() -> {
                String previous = threadLocal.get();
                threadLocal.set(captured);
                try {
                    command.run();
                } finally {
                    if (previous == null) {
                        threadLocal.remove();
                    } else {
                        threadLocal.set(previous);
                    }
                }
            });
        }
    }

    private RegistryEventConsumer<ThreadPoolBulkhead> getNoOpsRegistryEventConsumer() {
        return new RegistryEventConsumer<ThreadPoolBulkhead>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<ThreadPoolBulkhead> entryAddedEvent) {
            }
            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<ThreadPoolBulkhead> entryRemoveEvent) {
            }
            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<ThreadPoolBulkhead> entryReplacedEvent) {
            }
        };
    }
}
