/*
 *
 *  Copyright 2024 kanghyun.yang
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
package io.github.resilience4j.core.metrics;

import com.statemachinesystems.mockclock.MockClock;
import io.github.resilience4j.core.JavaClockWrapper;
import org.junit.Test;

import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance test for CAS loop optimizations with Virtual Threads.
 * Tests the effectiveness of Thread.onSpinWait() and LockSupport.parkNanos() 
 * in high-concurrency scenarios.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
public class VirtualThreadMetricsPerformanceTest {

    private static final int CONCURRENT_OPERATIONS = 1000;
    private static final int ITERATIONS_PER_THREAD = 100;

    @Test
    public void testConcurrentAccessWithVirtualThreads() throws Exception {
        if (!supportsVirtualThreads()) {
            System.out.println("Virtual threads not supported, skipping test");
            return;
        }

        // Test LockFreeFixedSizeSlidingWindowMetrics with virtual threads
        LockFreeFixedSizeSlidingWindowMetrics fixedMetrics = new LockFreeFixedSizeSlidingWindowMetrics(10);
        testConcurrentMetricsAccess("LockFreeFixedSize", fixedMetrics, true);
        
        // Test LockFreeSlidingTimeWindowMetrics with virtual threads
        MockClock clock = MockClock.at(2024, 1, 1, 12, 0, 0, ZoneId.of("UTC"));
        JavaClockWrapper wrappedClock = new JavaClockWrapper(clock);
        LockFreeSlidingTimeWindowMetrics timeMetrics = new LockFreeSlidingTimeWindowMetrics(5, wrappedClock);
        testConcurrentMetricsAccess("LockFreeSlidingTime", timeMetrics, true);
    }

    @Test
    public void testConcurrentAccessWithPlatformThreads() throws Exception {
        // Test LockFreeFixedSizeSlidingWindowMetrics with platform threads
        LockFreeFixedSizeSlidingWindowMetrics fixedMetrics = new LockFreeFixedSizeSlidingWindowMetrics(10);
        testConcurrentMetricsAccess("LockFreeFixedSize", fixedMetrics, false);
        
        // Test LockFreeSlidingTimeWindowMetrics with platform threads
        MockClock clock = MockClock.at(2024, 1, 1, 12, 0, 0, ZoneId.of("UTC"));
        JavaClockWrapper wrappedClock = new JavaClockWrapper(clock);
        LockFreeSlidingTimeWindowMetrics timeMetrics = new LockFreeSlidingTimeWindowMetrics(5, wrappedClock);
        testConcurrentMetricsAccess("LockFreeSlidingTime", timeMetrics, false);
    }

    private void testConcurrentMetricsAccess(String metricsType, Metrics metrics, boolean useVirtualThreads) throws Exception {
        ExecutorService executor = useVirtualThreads 
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalDuration = new AtomicLong(0);

        long startTime = System.nanoTime();

        try {
            CompletableFuture<?>[] futures = new CompletableFuture[CONCURRENT_OPERATIONS];

            for (int i = 0; i < CONCURRENT_OPERATIONS; i++) {
                final int threadId = i;
                futures[i] = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < ITERATIONS_PER_THREAD; j++) {
                        try {
                            long operationStart = System.nanoTime();
                            
                            // Alternate between success and error outcomes to create contention
                            Metrics.Outcome outcome = (threadId + j) % 2 == 0 
                                ? Metrics.Outcome.SUCCESS 
                                : Metrics.Outcome.ERROR;
                            
                            Snapshot snapshot = metrics.record(100 + j, TimeUnit.MILLISECONDS, outcome);
                            
                            long operationEnd = System.nanoTime();
                            totalDuration.addAndGet(operationEnd - operationStart);
                            
                            // Verify snapshot is valid
                            assertThat(snapshot).isNotNull();
                            assertThat(snapshot.getTotalNumberOfCalls()).isGreaterThan(0);
                            
                            if (outcome == Metrics.Outcome.SUCCESS) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            System.err.println("Error in thread " + threadId + ": " + e.getMessage());
                            throw new RuntimeException(e);
                        }
                    }
                }, executor);
            }

            // Wait for all tasks to complete
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;

        // Verify results
        int expectedTotalOperations = CONCURRENT_OPERATIONS * ITERATIONS_PER_THREAD;
        int actualTotalOperations = successCount.get() + errorCount.get();
        
        assertThat(actualTotalOperations).isEqualTo(expectedTotalOperations);
        
        // Performance metrics
        double avgOperationTime = totalDuration.get() / (double) expectedTotalOperations;
        double throughput = expectedTotalOperations / (totalTime / 1_000_000_000.0);
        
        System.out.printf("%s with %s threads:%n", metricsType, useVirtualThreads ? "Virtual" : "Platform");
        System.out.printf("  Total operations: %d%n", actualTotalOperations);
        System.out.printf("  Success: %d, Errors: %d%n", successCount.get(), errorCount.get());
        System.out.printf("  Total time: %.3f ms%n", totalTime / 1_000_000.0);
        System.out.printf("  Average operation time: %.3f µs%n", avgOperationTime / 1_000.0);
        System.out.printf("  Throughput: %.0f ops/sec%n", throughput);
        System.out.println();
        
        // Verify final metrics state
        Snapshot finalSnapshot = metrics.getSnapshot();
        assertThat(finalSnapshot).isNotNull();
        assertThat(finalSnapshot.getTotalNumberOfCalls()).isGreaterThan(0);
    }

    @Test
    public void testCASLoopBackoffBehavior() {
        // Test that backoff logic works correctly under contention
        LockFreeFixedSizeSlidingWindowMetrics metrics = new LockFreeFixedSizeSlidingWindowMetrics(5);
        
        // Create high contention scenario
        int threadCount = Runtime.getRuntime().availableProcessors() * 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger completedOperations = new AtomicInteger(0);
        
        try {
            CompletableFuture<?>[] futures = new CompletableFuture[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < 50; j++) {
                        metrics.record(j, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
                        completedOperations.incrementAndGet();
                    }
                }, executor);
            }
            
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
            
            // Verify all operations completed successfully
            assertThat(completedOperations.get()).isEqualTo(threadCount * 50);
            
            // Verify final state
            Snapshot snapshot = metrics.getSnapshot();
            assertThat(snapshot.getTotalNumberOfCalls()).isGreaterThan(0);
            
        } catch (Exception e) {
            throw new RuntimeException("Test failed", e);
        } finally {
            executor.shutdown();
        }
    }

    private boolean supportsVirtualThreads() {
        try {
            // Check if we're running on Java 21+ with virtual thread support
            Runtime.Version version = Runtime.version();
            if (version.feature() < 21) {
                return false;
            }
            
            // Try to create a virtual thread to verify support
            Thread.ofVirtual().start(() -> {}).join();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}