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
package io.github.resilience4j.core;

import io.github.resilience4j.core.metrics.LockFreeFixedSizeSlidingWindowMetrics;
import io.github.resilience4j.core.metrics.LockFreeSlidingTimeWindowMetrics;
import io.github.resilience4j.core.metrics.Metrics;
import io.github.resilience4j.core.metrics.Snapshot;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * JMH benchmark comparing virtual thread vs platform thread performance
 * for lock-free metrics implementations under concurrent access.
 *
 * <p>Measures throughput and average time of metrics recording operations
 * across different thread types and concurrency levels.
 *
 * <p>Run with:
 * <pre>
 *   ./gradlew :resilience4j-core:jmh -Pjmh.includes='VirtualThreadMetricsBenchmark'
 * </pre>
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class VirtualThreadMetricsBenchmark {

    private static final int FORK_COUNT = 1;
    private static final int WARMUP_COUNT = 1;
    private static final int ITERATION_COUNT = 1;
    private static final int OPERATIONS_PER_TASK = 100;
    private static final int SLIDING_WINDOW_SIZE = 10;
    private static final int PLATFORM_POOL_SIZE = Runtime.getRuntime().availableProcessors();

    @Param({"cpu", "io"})
    private String workloadType;

    @Param({"virtual", "platform"})
    private String threadMode;

    @Param({"lockFreeFixed", "lockFreeTime"})
    private String metricsType;

    @Param({"4", "32", "128"})
    private int concurrency;

    private Metrics metrics;
    private ExecutorService executor;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(".*" + VirtualThreadMetricsBenchmark.class.getSimpleName() + ".*")
            .build();
        new Runner(options).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        metrics = switch (metricsType) {
            case "lockFreeFixed" -> new LockFreeFixedSizeSlidingWindowMetrics(SLIDING_WINDOW_SIZE);
            case "lockFreeTime" -> new LockFreeSlidingTimeWindowMetrics(SLIDING_WINDOW_SIZE);
            default -> throw new IllegalArgumentException("Unknown metricsType: " + metricsType);
        };

        // Platform: fixed pool (availableProcessors) — tasks > pool size causes thread starvation
        // Virtual: unbounded — all tasks run concurrently regardless of concurrency count
        executor = switch (threadMode) {
            case "virtual" -> Executors.newVirtualThreadPerTaskExecutor();
            case "platform" -> Executors.newFixedThreadPool(PLATFORM_POOL_SIZE);
            default -> throw new IllegalArgumentException("Unknown threadMode: " + threadMode);
        };
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        if (executor != null) {
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

    /**
     * Benchmarks concurrent metrics recording by submitting {@code concurrency} tasks
     * to the executor. Each task performs {@value OPERATIONS_PER_TASK} record operations.
     *
     * <p>Platform threads use a fixed pool of {@code availableProcessors()} threads.
     * When {@code concurrency > pool size}, platform threads queue tasks while
     * virtual threads run all tasks concurrently — revealing the I/O-bound advantage.
     */
    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public void concurrentMetricsRecord(Blackhole bh) throws Exception {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);
        AtomicInteger taskIndex = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    int idx = taskIndex.getAndIncrement();
                    for (int j = 0; j < OPERATIONS_PER_TASK; j++) {
                        long duration = simulateWork();
                        Metrics.Outcome outcome = (idx + j) % 2 == 0
                            ? Metrics.Outcome.SUCCESS
                            : Metrics.Outcome.ERROR;
                        Snapshot snapshot = metrics.record(duration, TimeUnit.MILLISECONDS, outcome);
                        bh.consume(snapshot);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Start all tasks simultaneously
        startLatch.countDown();
        doneLatch.await();
    }

    /**
     * Simulates work based on the configured workload type.
     * <ul>
     *   <li>{@code cpu}: Math.sqrt loop — tests CAS contention under CPU load</li>
     *   <li>{@code io}: LockSupport.parkNanos — simulates blocking I/O where virtual threads excel</li>
     * </ul>
     */
    private long simulateWork() {
        return switch (workloadType) {
            case "cpu" -> simulateCpuWork();
            case "io" -> simulateIoWork();
            default -> throw new IllegalArgumentException("Unknown workloadType: " + workloadType);
        };
    }

    private long simulateCpuWork() {
        long result = 0;
        for (long i = 0; i < 5000; i++) {
            result += (long) Math.sqrt(i);
        }
        return result;
    }

    private long simulateIoWork() {
        LockSupport.parkNanos(100_000); // 100μs blocking I/O simulation
        return 100;
    }
}
