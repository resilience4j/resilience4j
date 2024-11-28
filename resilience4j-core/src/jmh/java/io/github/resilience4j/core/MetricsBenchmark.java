package io.github.resilience4j.core;

import io.github.resilience4j.core.metrics.*;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.time.Clock;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@BenchmarkMode(Mode.Throughput)
public class MetricsBenchmark {
    public static final int FORK_COUNT = 1;
    private static final int WARMUP_COUNT = 3;
    private static final int ITERATION_COUNT = 3;

    private final int slidingWindowSize = 5;

    private LockFreeFixedSizeSlidingWindowMetrics lockFreeSlidingWindow;
    private FixedSizeSlidingWindowMetrics slidingWindow;

    private LockFreeSlidingTimeWindowMetrics lockFreeSlidingTimeWindow;
    private SlidingTimeWindowMetrics slidingTimeWindow;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(".*" + MetricsBenchmark.class.getSimpleName() + ".*")
                .build();
        new Runner(options).run();
    }

    @Setup(Level.Iteration)
    public void setUp() {
        lockFreeSlidingWindow = new LockFreeFixedSizeSlidingWindowMetrics(slidingWindowSize);
        slidingWindow = new FixedSizeSlidingWindowMetrics(slidingWindowSize);

        lockFreeSlidingTimeWindow = new LockFreeSlidingTimeWindowMetrics(slidingWindowSize);
        slidingTimeWindow = new SlidingTimeWindowMetrics(slidingWindowSize, Clock.systemUTC());
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(1)
    public Snapshot benchmarkLFSW1Thread() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(4)
    public Snapshot benchmarkLFSW4Threads() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(8)
    public Snapshot benchmarkLFSW8Threads() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(16)
    public Snapshot benchmarkLFSW16Threads() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(1)
    public Snapshot benchmarkSW1Thread() throws InterruptedException {
        long duration = simulateWork();
        return slidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(4)
    public Snapshot benchmarkSW4Threads() throws InterruptedException {
        long duration = simulateWork();
        return slidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(8)
    public Snapshot benchmarkSW8Threads() throws InterruptedException {
        long duration = simulateWork();
        return slidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(16)
    public Snapshot benchmarkSW16Threads() throws InterruptedException {
        long duration = simulateWork();
        return slidingWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(1)
    public Snapshot benchmarkLFSTW1Thread() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(4)
    public Snapshot benchmarkLFSTW4Threads() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(8)
    public Snapshot benchmarkLFSTW8Threads() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(16)
    public Snapshot benchmarkLFSTW16Threads() throws InterruptedException {
        long duration = simulateWork();
        return lockFreeSlidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(1)
    public Snapshot benchmarkSTW1Thread() throws InterruptedException {
        long duration = simulateWork();
        return slidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(4)
    public Snapshot benchmarkSTW4Threads() throws InterruptedException {
        long duration = simulateWork();
        return slidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(8)
    public Snapshot benchmarkSTW8Threads() throws InterruptedException {
        long duration = simulateWork();
        return slidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    @Benchmark
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    @Threads(16)
    public Snapshot benchmarkSTW16Threads() throws InterruptedException {
        long duration = simulateWork();
        return slidingTimeWindow.record(duration, TimeUnit.MILLISECONDS, Metrics.Outcome.SUCCESS);
    }

    public long simulateWork() {
        long result = 0;

        for (long i = 0; i < 5000; i++) {
            result += (long) Math.sqrt(i);
        }

        return result;
    }
}
