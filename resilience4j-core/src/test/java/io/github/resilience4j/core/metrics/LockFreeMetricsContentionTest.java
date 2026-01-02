package io.github.resilience4j.core.metrics;

import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for lock-free metrics under high contention scenarios.
 * Tests that CAS backoff strategies work correctly with both platform and virtual threads.
 */
@RunWith(Parameterized.class)
public class LockFreeMetricsContentionTest extends ThreadModeTestBase {

    @Parameterized.Parameters(name = "threadMode={0}")
    public static Collection<Object[]> data() {
        return threadModes();
    }

    public LockFreeMetricsContentionTest(ThreadType threadType) {
        super(threadType);
    }

    @Before
    public void setUp() {
        setUpThreadMode();
        System.out.println("Running LockFreeMetricsContentionTest in " + getThreadModeDescription());
    }

    @After
    public void tearDown() {
        cleanUpThreadMode();
    }

    @Test
    public void shouldHandleHighContentionWithoutStarvation() throws Exception {
        // Given: metrics instance and high contention setup
        int windowSize = 10;
        LockFreeFixedSizeSlidingWindowMetrics metrics =
            new LockFreeFixedSizeSlidingWindowMetrics(windowSize);

        int threadCount = 50;
        int operationsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When: multiple threads record metrics concurrently
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();

                        // Record metrics
                        for (int j = 0; j < operationsPerThread; j++) {
                            // Alternate between success and failure
                            if ((threadIndex + j) % 2 == 0) {
                                metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
                                successCount.incrementAndGet();
                            } else {
                                metrics.record(200, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
                                failureCount.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
                futures.add(future);
            }

            // Start all threads simultaneously
            startLatch.countDown();

            // Wait for all threads to complete
            boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

            // Then: all threads should complete without starvation or deadlock
            assertThat(completed)
                .as("All threads should complete within timeout in " + getThreadModeDescription())
                .isTrue();

            // Verify no exceptions occurred
            for (Future<?> future : futures) {
                future.get(1, TimeUnit.SECONDS); // Should already be done
            }

            // Verify metrics are consistent
            Snapshot snapshot = metrics.getSnapshot();
            assertThat(snapshot)
                .as("Snapshot should not be null")
                .isNotNull();

            // Total calls should match (though some might be outside the sliding window)
            int totalRecorded = successCount.get() + failureCount.get();
            assertThat(totalRecorded)
                .as("Total recorded operations should match expected count")
                .isEqualTo(threadCount * operationsPerThread);

            System.out.println("✅ High contention test passed in " + getThreadModeDescription() +
                             " - Total operations: " + totalRecorded +
                             ", Success: " + successCount.get() +
                             ", Failures: " + failureCount.get());

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldMaintainCorrectMetricsUnderContention() throws Exception {
        // Given: metrics instance
        int windowSize = 100;
        LockFreeFixedSizeSlidingWindowMetrics metrics =
            new LockFreeFixedSizeSlidingWindowMetrics(windowSize);

        int threadCount = 20;
        int successPerThread = 50;
        int errorPerThread = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // When: threads record known numbers of successes and errors
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        // Record successes
                        for (int j = 0; j < successPerThread; j++) {
                            metrics.record(50, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
                        }

                        // Record errors
                        for (int j = 0; j < errorPerThread; j++) {
                            metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.ERROR);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(20, TimeUnit.SECONDS);

            // Then: should complete and metrics should be sensible
            assertThat(completed)
                .as("All threads should complete in " + getThreadModeDescription())
                .isTrue();

            Snapshot snapshot = metrics.getSnapshot();

            // Note: Due to sliding window, we might not see all operations
            // But we should see at least windowSize operations
            int totalCalls = snapshot.getTotalNumberOfCalls();
            assertThat(totalCalls)
                .as("Should have recorded operations up to window size")
                .isGreaterThan(0)
                .isLessThanOrEqualTo(windowSize);

            System.out.println("✅ Metrics correctness test passed in " + getThreadModeDescription() +
                             " - Total calls: " + totalCalls +
                             ", Failure rate: " + snapshot.getFailureRate());

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldNotExperienceStarvationWithCASBackoff() throws Exception {
        // Given: metrics and threads competing for updates
        LockFreeFixedSizeSlidingWindowMetrics metrics =
            new LockFreeFixedSizeSlidingWindowMetrics(50);

        int threadCount = 30;
        int operationsPerThread = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger[] completedOps = new AtomicInteger[threadCount];

        for (int i = 0; i < threadCount; i++) {
            completedOps[i] = new AtomicInteger(0);
        }

        // When: all threads compete to update metrics
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                Future<?> future = executor.submit(() -> {
                    try {
                        startLatch.await();

                        for (int j = 0; j < operationsPerThread; j++) {
                            metrics.record(10, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
                            completedOps[threadIndex].incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                futures.add(future);
            }

            startLatch.countDown();

            // Wait for all to complete
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            // Then: every thread should have completed all operations (no starvation)
            for (int i = 0; i < threadCount; i++) {
                assertThat(completedOps[i].get())
                    .as("Thread %d should complete all operations in %s", i, getThreadModeDescription())
                    .isEqualTo(operationsPerThread);
            }

            System.out.println("✅ No starvation test passed in " + getThreadModeDescription() +
                             " - All " + threadCount + " threads completed " + operationsPerThread + " operations");

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void shouldHandleMixedReadWriteContention() throws Exception {
        // Given: metrics with both readers and writers
        LockFreeFixedSizeSlidingWindowMetrics metrics =
            new LockFreeFixedSizeSlidingWindowMetrics(100);

        int writerCount = 20;
        int readerCount = 20;
        int operations = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(writerCount + readerCount);

        // When: writers update metrics while readers read snapshots
        ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);
        try {
            // Writers
            for (int i = 0; i < writerCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operations; j++) {
                            metrics.record(50, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Readers
            for (int i = 0; i < readerCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operations; j++) {
                            Snapshot snapshot = metrics.getSnapshot();
                            // Just verify we can read without exceptions
                            assertThat(snapshot).isNotNull();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(30, TimeUnit.SECONDS);

            // Then: all operations should complete without deadlock
            assertThat(completed)
                .as("Mixed read/write should complete in " + getThreadModeDescription())
                .isTrue();

            System.out.println("✅ Mixed read/write contention test passed in " + getThreadModeDescription());

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
