package io.github.resilience4j.core.metrics;

import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for lock-free metrics under high contention scenarios.
 * Tests that CAS backoff strategies work correctly with both platform and virtual threads.
 */
@ExtendWith(ThreadModeExtension.class)
class LockFreeMetricsContentionTest {

    private static final Logger LOG = LoggerFactory.getLogger(LockFreeMetricsContentionTest.class);

    @TestTemplate
    void shouldMaintainSingleCallWindowUnderContention(ThreadType threadType) throws Exception {
        LockFreeFixedSizeSlidingWindowMetrics metrics = new LockFreeFixedSizeSlidingWindowMetrics(1);
        metrics.record(100, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);

        int writerCount = 8;
        int readerCount = 2;
        int operations = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger recordedCalls = new AtomicInteger();
        AtomicInteger snapshotsRead = new AtomicInteger();
        ExecutorService executor = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(writerCount + readerCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < writerCount; i++) {
                final int writer = i;
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    for (int j = 0; j < operations; j++) {
                        boolean error = (writer + j) % 2 == 0;
                        Snapshot snapshot = metrics.record(100, TimeUnit.MILLISECONDS,
                            error ? Metrics.Outcome.SLOW_ERROR : Metrics.Outcome.SUCCESS);
                        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
                        assertThat(snapshot.getNumberOfFailedCalls()).isEqualTo(error ? 1 : 0);
                        assertThat(snapshot.getTotalNumberOfSlowCalls()).isEqualTo(error ? 1 : 0);
                        assertThat(snapshot.getTotalDuration()).hasMillis(100);
                        recordedCalls.incrementAndGet();
                    }
                    return null;
                }));
            }
            for (int i = 0; i < readerCount; i++) {
                futures.add(executor.submit(() -> {
                    startLatch.await();
                    for (int j = 0; j < operations; j++) {
                        Snapshot snapshot = metrics.getSnapshot();
                        assertThat(snapshot.getTotalNumberOfCalls()).isOne();
                        assertThat(snapshot.getNumberOfSuccessfulCalls()
                            + snapshot.getNumberOfFailedCalls()).isOne();
                        assertThat(snapshot.getTotalDuration()).hasMillis(100);
                        snapshotsRead.incrementAndGet();
                    }
                    return null;
                }));
            }

            startLatch.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
            assertThat(recordedCalls.get()).isEqualTo(writerCount * operations);
            assertThat(snapshotsRead.get()).isEqualTo(readerCount * operations);
            assertThat(metrics.getSnapshot().getTotalNumberOfCalls()).isOne();
            LOG.info("Single-call window test passed in {}: {} writes, {} reads",
                threadType, recordedCalls.get(), snapshotsRead.get());
        } finally {
            startLatch.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @TestTemplate
    void shouldHandleHighContentionWithoutStarvation(ThreadType threadType) throws Exception {
        LOG.info("Running shouldHandleHighContentionWithoutStarvation in {}", threadType);

        int windowSize = 10;
        LockFreeFixedSizeSlidingWindowMetrics metrics = new LockFreeFixedSizeSlidingWindowMetrics(windowSize);

        int threadCount = 50;
        int operationsPerThread = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
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
                }));
            }

            startLatch.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS))
                .as("All threads should complete within timeout in %s", threadType)
                .isTrue();

            for (Future<?> future : futures) {
                future.get(1, TimeUnit.SECONDS);
            }

            assertThat(metrics.getSnapshot()).isNotNull();
            assertThat(successCount.get() + failureCount.get())
                .as("Total recorded operations should match expected count")
                .isEqualTo(threadCount * operationsPerThread);

            LOG.info("High contention test passed in {} - Total: {}, Success: {}, Failures: {}",
                threadType, successCount.get() + failureCount.get(), successCount.get(), failureCount.get());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @TestTemplate
    void shouldMaintainCorrectMetricsUnderContention(ThreadType threadType) throws Exception {
        LOG.info("Running shouldMaintainCorrectMetricsUnderContention in {}", threadType);

        int windowSize = 100;
        LockFreeFixedSizeSlidingWindowMetrics metrics = new LockFreeFixedSizeSlidingWindowMetrics(windowSize);

        int threadCount = 20;
        int successPerThread = 50;
        int errorPerThread = 50;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < successPerThread; j++) {
                            metrics.record(50, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
                        }
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
            assertThat(doneLatch.await(20, TimeUnit.SECONDS))
                .as("All threads should complete in %s", threadType)
                .isTrue();

            Snapshot snapshot = metrics.getSnapshot();
            assertThat(snapshot.getTotalNumberOfCalls())
                .as("Should have recorded operations up to window size")
                .isGreaterThan(0)
                .isLessThanOrEqualTo(windowSize);

            LOG.info("Metrics correctness test passed in {} - Total calls: {}, Failure rate: {}",
                threadType, snapshot.getTotalNumberOfCalls(), snapshot.getFailureRate());
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @TestTemplate
    void shouldNotExperienceStarvationWithCASBackoff(ThreadType threadType) throws Exception {
        LOG.info("Running shouldNotExperienceStarvationWithCASBackoff in {}", threadType);

        LockFreeFixedSizeSlidingWindowMetrics metrics = new LockFreeFixedSizeSlidingWindowMetrics(50);

        int threadCount = 30;
        int operationsPerThread = 500;
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger[] completedOps = new AtomicInteger[threadCount];
        for (int i = 0; i < threadCount; i++) {
            completedOps[i] = new AtomicInteger(0);
        }

        ExecutorService executor = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                final int threadIndex = i;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operationsPerThread; j++) {
                            metrics.record(10, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
                            completedOps[threadIndex].incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }

            startLatch.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            for (int i = 0; i < threadCount; i++) {
                assertThat(completedOps[i].get())
                    .as("Thread %d should complete all operations in %s", i, threadType)
                    .isEqualTo(operationsPerThread);
            }

            LOG.info("No starvation test passed in {} - All {} threads completed {} operations",
                threadType, threadCount, operationsPerThread);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @TestTemplate
    void shouldHandleMixedReadWriteContention(ThreadType threadType) throws Exception {
        LOG.info("Running shouldHandleMixedReadWriteContention in {}", threadType);

        LockFreeFixedSizeSlidingWindowMetrics metrics = new LockFreeFixedSizeSlidingWindowMetrics(100);

        int writerCount = 20;
        int readerCount = 20;
        int operations = 1000;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(writerCount + readerCount);

        ExecutorService executor = threadType == ThreadType.VIRTUAL
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(writerCount + readerCount);
        try {
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

            for (int i = 0; i < readerCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < operations; j++) {
                            assertThat(metrics.getSnapshot()).isNotNull();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS))
                .as("Mixed read/write should complete in %s", threadType)
                .isTrue();

            LOG.info("Mixed read/write contention test passed in {}", threadType);
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
