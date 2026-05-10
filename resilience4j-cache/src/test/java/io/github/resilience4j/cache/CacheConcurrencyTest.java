package io.github.resilience4j.cache;

import io.github.resilience4j.cache.event.CacheEvent;
import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests for concurrent access to Cache, focusing on thread safety
 * of cache operations and event publishing.
 * Tests run in both platform and virtual thread modes.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
@ExtendWith(ThreadModeExtension.class)
class CacheConcurrencyTest {

    private static final Logger LOG = LoggerFactory.getLogger(CacheConcurrencyTest.class);

    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 50;

    private ExecutorService executorService;
    private javax.cache.Cache<String, String> jCache;
    private Cache<String, String> cache;
    private List<CacheEvent> events;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setup() {
        jCache = mock(javax.cache.Cache.class);

        java.util.Map<String, String> mockCacheStorage = new java.util.concurrent.ConcurrentHashMap<>();

        given(jCache.invoke(any(), any())).willAnswer(invocation -> {
            String key = invocation.getArgument(0);
            EntryProcessor<String, String, String> processor = invocation.getArgument(1);

            MutableEntry<String, String> entry = mock(MutableEntry.class);
            given(entry.getKey()).willReturn(key);

            String existingValue = mockCacheStorage.get(key);
            given(entry.exists()).willReturn(existingValue != null);
            given(entry.getValue()).willReturn(existingValue);

            willAnswer(setValue -> {
                mockCacheStorage.put(key, (String) setValue.getArgument(0));
                return null;
            }).given(entry).setValue(any());

            return processor.process(entry);
        });

        events = Collections.synchronizedList(new ArrayList<>());
        cache = Cache.of(jCache);
        cache.getEventPublisher().onCacheHit(events::add);
        cache.getEventPublisher().onCacheMiss(events::add);
    }

    @AfterEach
    void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (jCache != null) {
            jCache.close();
        }

        if (events != null) {
            events.clear();
        }
    }

    @TestTemplate
    void shouldHandleConcurrentCacheOperations(ThreadType threadType) throws Exception {
        LOG.info("Testing cache concurrency with {}", threadType);

        final int numThreads = THREAD_COUNT;
        final int operationsPerThread = OPERATIONS_PER_THREAD;
        executorService = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicReference<Exception> firstException = new AtomicReference<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        String value = "value-" + threadId + "-" + j;

                        Function<String, String> cachedFunction = Cache.decorateSupplier(cache, () -> value);

                        String result1 = cachedFunction.apply(key);
                        assertThat(result1).isEqualTo(value);

                        String result2 = cachedFunction.apply(key);
                        assertThat(result2).isEqualTo(value);

                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completeLatch.await(30, TimeUnit.SECONDS)).isTrue();

        if (firstException.get() != null) {
            throw new AssertionError("Concurrency test failed", firstException.get());
        }

        assertThat(successCount.get()).isEqualTo(numThreads * operationsPerThread);
        assertThat(events).isNotEmpty();

        LOG.info("Cache concurrency test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleConcurrentDecoratorUsage(ThreadType threadType) throws Exception {
        LOG.info("Testing concurrent decorator usage with {}", threadType);

        final int numThreads = THREAD_COUNT;
        final int operationsPerThread = OPERATIONS_PER_THREAD;
        executorService = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicInteger callCount = new AtomicInteger(0);
        final AtomicInteger supplierCallCount = new AtomicInteger(0);
        final AtomicReference<Exception> firstException = new AtomicReference<>();

        String sharedKey = "shared-test-key";

        Supplier<String> expensiveOperation = () -> {
            supplierCallCount.incrementAndGet();
            return "expensive-result-" + System.nanoTime();
        };

        Function<String, String> cachedFunction = Cache.decorateSupplier(cache, expensiveOperation);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < operationsPerThread; j++) {
                        String result = cachedFunction.apply(sharedKey);
                        assertThat(result).isNotNull();
                        assertThat(result).startsWith("expensive-result-");
                        callCount.incrementAndGet();
                    }

                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completeLatch.await(30, TimeUnit.SECONDS)).isTrue();

        if (firstException.get() != null) {
            throw new AssertionError("Concurrent decorator test failed", firstException.get());
        }

        assertThat(callCount.get()).isEqualTo(numThreads * operationsPerThread);
        assertThat(supplierCallCount.get()).isLessThan(callCount.get());

        LOG.info("Concurrent decorator usage test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleRaceConditions(ThreadType threadType) throws Exception {
        final int numThreads = THREAD_COUNT;
        executorService = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicInteger supplierCallCount = new AtomicInteger(0);
        final AtomicReference<Exception> firstException = new AtomicReference<>();
        final List<String> results = Collections.synchronizedList(new ArrayList<>());

        String raceKey = "race-condition-key";

        Supplier<String> expensiveSupplier = () -> "result-" + supplierCallCount.incrementAndGet();
        Function<String, String> cachedFunction = Cache.decorateSupplier(cache, expensiveSupplier);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    results.add(cachedFunction.apply(raceKey));
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completeLatch.await(15, TimeUnit.SECONDS)).isTrue();

        if (firstException.get() != null) {
            throw new AssertionError("Race condition test failed", firstException.get());
        }

        assertThat(results).hasSize(numThreads);

        LOG.info("Race condition test passed with {}", threadType);
    }
}
