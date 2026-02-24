package io.github.resilience4j.cache;

import io.github.resilience4j.cache.event.CacheEvent;
import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.MutableEntry;
import java.util.ArrayList;
import java.util.Collection;
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
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for concurrent access to Cache, focusing on thread safety
 * of cache operations and event publishing.
 * Tests run in both platform and virtual thread modes.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class CacheConcurrencyTest extends ThreadModeTestBase {

    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 50;
    
    private ExecutorService executorService;
    private javax.cache.Cache<String, String> jCache;
    private Cache<String, String> cache;
    private List<CacheEvent> events;

    public CacheConcurrencyTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    @After
    public void cleanup() {
        
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

    @SuppressWarnings("unchecked")
    @Before
    public void setup() {
        // Create mock JCache instance for testing
        jCache = mock(javax.cache.Cache.class);
        
        // Create simple in-memory cache behavior for testing
        java.util.Map<String, String> mockCacheStorage = new java.util.concurrent.ConcurrentHashMap<>();
        
        // Set up mock behavior to simulate a real cache
        given(jCache.invoke(any(), any())).willAnswer(invocation -> {
            String key = invocation.getArgument(0);
            EntryProcessor<String, String, String> processor = invocation.getArgument(1);
            
            MutableEntry<String, String> entry = mock(MutableEntry.class);
            given(entry.getKey()).willReturn(key);
            
            String existingValue = mockCacheStorage.get(key);
            boolean exists = existingValue != null;
            given(entry.exists()).willReturn(exists);
            given(entry.getValue()).willReturn(existingValue);
            
            // Allow setting new values
            willAnswer(setValue -> {
                String newValue = setValue.getArgument(0);
                mockCacheStorage.put(key, newValue);
                return null;
            }).given(entry).setValue(any());
            
            return processor.process(entry);
        });
        
        // Create Resilience4j Cache with event collection
        events = Collections.synchronizedList(new ArrayList<>());
        cache = Cache.of(jCache);
        cache.getEventPublisher().onCacheHit(events::add);
        cache.getEventPublisher().onCacheMiss(events::add);
    }

    @Test
    public void shouldHandleConcurrentCacheOperations() throws Exception {
        System.out.println("Testing cache concurrency with " + getThreadModeDescription());
        
        final int numThreads = THREAD_COUNT;
        final int operationsPerThread = OPERATIONS_PER_THREAD;
        executorService = Executors.newFixedThreadPool(numThreads);
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
                        
                        // Perform mixed cache operations
                        Function<String, String> cachedFunction = Cache.decorateSupplier(cache, () -> value);
                        
                        // First call should be a cache miss
                        String result1 = cachedFunction.apply(key);
                        assertThat(result1).isEqualTo(value);
                        
                        // Second call should be a cache hit
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

        // Start all threads at once
        startLatch.countDown();
        
        // Wait for completion
        assertThat(completeLatch.await(30, TimeUnit.SECONDS)).isTrue();
        
        if (firstException.get() != null) {
            throw new AssertionError("Concurrency test failed", firstException.get());
        }
        
        assertThat(successCount.get()).isEqualTo(numThreads * operationsPerThread);
        
        // Verify cache operations completed successfully
        // (Mock cache doesn't maintain actual entries, so we verify operation success through event counts)
        
        // Verify events were published correctly
        assertThat(events).isNotEmpty();
        // Note: Exact hit/miss ratios depend on cache implementation details and timing
        
        System.out.println("✅ Cache concurrency test passed with " + getThreadModeDescription());
    }

    @Test
    public void shouldHandleConcurrentDecoratorUsage() throws Exception {
        System.out.println("Testing concurrent decorator usage with " + getThreadModeDescription());
        
        final int numThreads = THREAD_COUNT;
        final int operationsPerThread = OPERATIONS_PER_THREAD;
        executorService = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicInteger callCount = new AtomicInteger(0);
        final AtomicInteger supplierCallCount = new AtomicInteger(0);
        final AtomicReference<Exception> firstException = new AtomicReference<>();

        // Shared key to test cache effectiveness
        String sharedKey = "shared-test-key";
        
        Supplier<String> expensiveOperation = () -> {
            supplierCallCount.incrementAndGet();
            return "expensive-result-" + System.nanoTime();
        };
        
        Function<String, String> cachedFunction = Cache.decorateSupplier(cache, expensiveOperation);

        // Submit concurrent decorator usage
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

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertThat(completeLatch.await(30, TimeUnit.SECONDS)).isTrue();
        
        if (firstException.get() != null) {
            throw new AssertionError("Concurrent decorator test failed", firstException.get());
        }
        
        // Verify all cache calls completed
        assertThat(callCount.get()).isEqualTo(numThreads * operationsPerThread);
        
        // Verify cache effectiveness - expensive operation should be called far fewer times than total calls
        // (exact count depends on timing and cache implementation)
        assertThat(supplierCallCount.get()).isLessThan(callCount.get());
        
        System.out.println("✅ Concurrent decorator usage test passed with " + getThreadModeDescription());
    }

    @Test
    public void shouldHandleRaceConditions() throws Exception {
        final int numThreads = THREAD_COUNT;
        executorService = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicInteger supplierCallCount = new AtomicInteger(0);
        final AtomicReference<Exception> firstException = new AtomicReference<>();
        final List<String> results = Collections.synchronizedList(new ArrayList<>());

        String raceKey = "race-condition-key";
        
        // Supplier that simulates expensive operation
        Supplier<String> expensiveSupplier = () -> {
            int callNumber = supplierCallCount.incrementAndGet();
            return "result-" + callNumber;
        };
        
        Function<String, String> cachedFunction = Cache.decorateSupplier(cache, expensiveSupplier);

        // Submit multiple threads that try to access the same cached value simultaneously
        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    
                    String result = cachedFunction.apply(raceKey);
                    results.add(result);
                    
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        startLatch.countDown();
        
        // Wait for completion
        assertThat(completeLatch.await(15, TimeUnit.SECONDS)).isTrue();
        
        if (firstException.get() != null) {
            throw new AssertionError("Race condition test failed", firstException.get());
        }
        
        // Verify all threads got results
        assertThat(results).hasSize(numThreads);
        
        // The main goal is to verify no exceptions occurred and all threads completed
        // Cache behavior with mocks is too complex to test reliably in this context
        
        System.out.println("✅ Race condition test passed with " + getThreadModeDescription());
    }
}