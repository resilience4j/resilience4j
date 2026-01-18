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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Benchmark test for CAS loop optimizations comparing performance between
 * Virtual Threads and Platform Threads.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
public class CASLoopBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 10000;
    private static final int THREAD_COUNTS[] = {1, 2, 4, 8, 16, 32};

    @Test
    public void benchmarkFixedSizeSlidingWindowMetrics() throws Exception {
        System.out.println("=== LockFreeFixedSizeSlidingWindowMetrics Benchmark ===");
        System.out.printf("%-8s %-15s %-15s %-15s %-18s %-18s%n", 
                         "Threads", "Platform(ops/s)", "Virtual(ops/s)", "Speedup", "Platform P99(µs)", "Virtual P99(µs)");
        
        for (int threadCount : THREAD_COUNTS) {
            BenchmarkResult platformResult = benchmarkMetrics(
                () -> new LockFreeFixedSizeSlidingWindowMetrics(10),
                threadCount, false, "FixedSize"
            );
            
            BenchmarkResult virtualResult = null;
            if (supportsVirtualThreads()) {
                virtualResult = benchmarkMetrics(
                    () -> new LockFreeFixedSizeSlidingWindowMetrics(10),
                    threadCount, true, "FixedSize"
                );
            }
            
            printBenchmarkResults(threadCount, platformResult, virtualResult);
        }
    }

    @Test
    public void benchmarkSlidingTimeWindowMetrics() throws Exception {
        System.out.println("\n=== LockFreeSlidingTimeWindowMetrics Benchmark ===");
        System.out.printf("%-8s %-15s %-15s %-15s %-18s %-18s%n", 
                         "Threads", "Platform(ops/s)", "Virtual(ops/s)", "Speedup", "Platform P99(µs)", "Virtual P99(µs)");
        
        for (int threadCount : THREAD_COUNTS) {
            BenchmarkResult platformResult = benchmarkMetrics(
                () -> {
                    MockClock clock = MockClock.at(2024, 1, 1, 12, 0, 0, ZoneId.of("UTC"));
                    return new LockFreeSlidingTimeWindowMetrics(5, new JavaClockWrapper(clock));
                },
                threadCount, false, "TimeWindow"
            );
            
            BenchmarkResult virtualResult = null;
            if (supportsVirtualThreads()) {
                virtualResult = benchmarkMetrics(
                    () -> {
                        MockClock clock = MockClock.at(2024, 1, 1, 12, 0, 0, ZoneId.of("UTC"));
                        return new LockFreeSlidingTimeWindowMetrics(5, new JavaClockWrapper(clock));
                    },
                    threadCount, true, "TimeWindow"
                );
            }
            
            printBenchmarkResults(threadCount, platformResult, virtualResult);
        }
    }

    @Test
    public void measureContentionUnderLoad() throws Exception {
        if (!supportsVirtualThreads()) {
            System.out.println("Virtual threads not supported, skipping contention test");
            return;
        }

        System.out.println("\n=== Contention Analysis ===");
        
        LockFreeFixedSizeSlidingWindowMetrics metrics = new LockFreeFixedSizeSlidingWindowMetrics(5);
        
        // High contention scenario: many threads competing for same metrics instance
        int highContentionThreads = Runtime.getRuntime().availableProcessors() * 8;
        
        BenchmarkResult platformContention = benchmarkMetrics(
            () -> metrics, highContentionThreads, false, "HighContention"
        );
        
        BenchmarkResult virtualContention = benchmarkMetrics(
            () -> metrics, highContentionThreads, true, "HighContention"
        );
        
        System.out.printf("High Contention (%d threads):%n", highContentionThreads);
        System.out.printf("  Platform: %.0f ops/sec, P99: %.1f µs%n", 
                         platformContention.throughput, platformContention.p99LatencyMicros);
        System.out.printf("  Virtual:  %.0f ops/sec, P99: %.1f µs%n", 
                         virtualContention.throughput, virtualContention.p99LatencyMicros);
        System.out.printf("  Speedup:  %.2fx%n", virtualContention.throughput / platformContention.throughput);
    }

    private BenchmarkResult benchmarkMetrics(MetricsFactory factory, int threadCount, 
                                           boolean useVirtualThreads, String testName) throws Exception {
        Metrics metrics = factory.create();
        
        // Warmup
        runBenchmark(metrics, threadCount, useVirtualThreads, WARMUP_ITERATIONS);
        
        // Force GC
        System.gc();
        Thread.sleep(100);
        
        // Actual benchmark
        return runBenchmark(metrics, threadCount, useVirtualThreads, BENCHMARK_ITERATIONS);
    }

    private BenchmarkResult runBenchmark(Metrics metrics, int threadCount, 
                                       boolean useVirtualThreads, int iterations) throws Exception {
        ExecutorService executor = useVirtualThreads 
            ? Executors.newVirtualThreadPerTaskExecutor()
            : Executors.newFixedThreadPool(threadCount);

        AtomicLong totalOperationTime = new AtomicLong(0);
        long[] latencies = new long[threadCount * iterations];
        AtomicLong latencyIndex = new AtomicLong(0);

        long startTime = System.nanoTime();

        try {
            CompletableFuture<?>[] futures = new CompletableFuture[threadCount];

            for (int i = 0; i < threadCount; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    for (int j = 0; j < iterations; j++) {
                        long operationStart = System.nanoTime();
                        
                        metrics.record(100 + j % 1000, TimeUnit.MICROSECONDS, 
                                     j % 2 == 0 ? Metrics.Outcome.SUCCESS : Metrics.Outcome.ERROR);
                        
                        long operationEnd = System.nanoTime();
                        long latency = operationEnd - operationStart;
                        
                        totalOperationTime.addAndGet(latency);
                        
                        int index = (int) latencyIndex.getAndIncrement();
                        if (index < latencies.length) {
                            latencies[index] = latency;
                        }
                    }
                }, executor);
            }

            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);

        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }

        long endTime = System.nanoTime();
        long totalTime = endTime - startTime;
        int totalOperations = threadCount * iterations;
        
        // Calculate percentiles
        java.util.Arrays.sort(latencies, 0, (int) Math.min(latencyIndex.get(), latencies.length));
        long p99Latency = latencies[(int) (latencyIndex.get() * 0.99)];
        
        double throughput = totalOperations / (totalTime / 1_000_000_000.0);
        double avgLatency = totalOperationTime.get() / (double) totalOperations;
        
        return new BenchmarkResult(throughput, avgLatency / 1000.0, p99Latency / 1000.0);
    }

    private void printBenchmarkResults(int threadCount, BenchmarkResult platform, BenchmarkResult virtual) {
        if (virtual != null && platform != null) {
            double speedup = virtual.throughput / platform.throughput;
            System.out.printf("%-8d %-15.0f %-15.0f %-15s %-18.1f %-18.1f%n", 
                             threadCount, platform.throughput, virtual.throughput, 
                             String.format("%.2fx", speedup),
                             platform.p99LatencyMicros, virtual.p99LatencyMicros);
        } else if (platform != null) {
            System.out.printf("%-8d %-15.0f %-15s %-15s %-18.1f %-18s%n", 
                             threadCount, platform.throughput, "N/A", "N/A", 
                             platform.p99LatencyMicros, "N/A");
        }
    }

    private boolean supportsVirtualThreads() {
        try {
            Runtime.Version version = Runtime.version();
            if (version.feature() < 21) {
                return false;
            }
            Thread.ofVirtual().start(() -> {}).join();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @FunctionalInterface
    private interface MetricsFactory {
        Metrics create();
    }

    private static class BenchmarkResult {
        final double throughput;
        final double avgLatencyMicros;
        final double p99LatencyMicros;

        BenchmarkResult(double throughput, double avgLatencyMicros, double p99LatencyMicros) {
            this.throughput = throughput;
            this.avgLatencyMicros = avgLatencyMicros;
            this.p99LatencyMicros = p99LatencyMicros;
        }
    }
}