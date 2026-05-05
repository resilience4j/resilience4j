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
package io.github.resilience4j.hedge;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Phase 3 scheduler-model comparison for Hedge, measured across both
 * {@code virtual} and {@code platform} thread modes.
 *
 * <p>Variants compare three one-shot delayed-task primitives that could back
 * a Hedge trigger:
 * <ul>
 *   <li>optionA — {@link CompletableFuture#delayedExecutor(long, TimeUnit, Executor)}.</li>
 *   <li>optionB — {@link Thread#ofVirtual()} / {@link Thread#ofPlatform()} per timer
 *       + {@link LockSupport#parkNanos(long)} (one dedicated thread per delayed event).</li>
 *   <li>optionC — {@link Executors#newScheduledThreadPool(int, ThreadFactory)} reused
 *       across timers — this mirrors what Hedge used before the {@code OneShotDelayedScheduler}
 *       migration.</li>
 * </ul>
 *
 * <p>The {@code threadMode} parameter selects whether virtual or platform threads
 * back the option. Together the 3x2 matrix shows which pattern wins in each mode
 * and supports the asymmetric strategy the shipped helper adopts:
 * <ul>
 *   <li>virtual mode → optionB (per-timer virtual thread)</li>
 *   <li>platform mode → optionC (shared STPE)</li>
 * </ul>
 *
 * <p>Two workloads model the hedge flow:
 * <ul>
 *   <li>{@code scheduleAndCancel} — schedule a 10ms timer and immediately cancel
 *       (the fast-primary hedge path: typical production case, cancellation dominates).</li>
 *   <li>{@code scheduleAndFire} — schedule a 1ms timer and wait for it to fire
 *       (the slow-primary hedge path: hedge actually triggers secondary).</li>
 * </ul>
 *
 * Run with:
 * <pre>
 *   ./gradlew :resilience4j-hedge:jmh -PjmhIncludes=HedgeSchedulerBenchmark
 * </pre>
 */
@State(Scope.Benchmark)
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 2)
@Measurement(iterations = 3, time = 3)
@Fork(value = 1, jvmArgs = {"-Xmx2g", "-Xms2g"})
@Threads(4)
public class HedgeSchedulerBenchmark {

    // optionA (CompletableFuture.delayedExecutor) was dropped after preliminary
    // runs: cancel() does not release the pending Delayer dispatch, producing
    // unbounded memory growth that OOMs the JVM in both virtual and platform
    // modes under sustained cancel-heavy load. Evidence is retained in
    // docs/pr-description-2451.md; the actionable comparison is B vs C.
    @Param({"optionB_perTimerThread", "optionC_sharedStpe"})
    private String variant;

    @Param({"virtual", "platform"})
    private String threadMode;

    private ScheduledExecutorService sharedStpe;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
            .include(".*" + HedgeSchedulerBenchmark.class.getSimpleName() + ".*")
            .build();
        new Runner(options).run();
    }

    @Setup(Level.Trial)
    public void setUp() {
        int poolSize = Math.max(1, Runtime.getRuntime().availableProcessors());
        ThreadFactory factory = threadFactory("bench-" + variant + "-");
        if ("optionC_sharedStpe".equals(variant)) {
            sharedStpe = Executors.newScheduledThreadPool(poolSize, factory);
        }
        // optionB has no shared state.
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (sharedStpe != null) {
            sharedStpe.shutdownNow();
        }
    }

    /**
     * Fast-primary scenario: schedule a 10ms timer, immediately cancel.
     * Measures the overhead of scheduling + cancelling when the timer never fires.
     */
    @Benchmark
    public void scheduleAndCancel(Blackhole bh) {
        switch (variant) {
            case "optionC_sharedStpe" -> {
                ScheduledFuture<?> sf = sharedStpe.schedule(
                    () -> { /* no-op */ }, 10, TimeUnit.MILLISECONDS);
                sf.cancel(true);
                bh.consume(sf);
            }
            case "optionB_perTimerThread" -> {
                Thread t = newPerTimerThread(() -> LockSupport.parkNanos(10_000_000L));
                t.start();
                t.interrupt();
                bh.consume(t);
            }
            default -> throw new IllegalArgumentException(variant);
        }
    }

    /**
     * Slow-primary scenario: schedule a 1ms timer and wait for it to fire.
     * Dominated by the 1ms delay — measures end-to-end latency consistency
     * across primitives.
     */
    @Benchmark
    public void scheduleAndFire() throws InterruptedException {
        CountDownLatch fired = new CountDownLatch(1);

        switch (variant) {
            case "optionC_sharedStpe" -> sharedStpe.schedule(
                fired::countDown, 1, TimeUnit.MILLISECONDS);
            case "optionB_perTimerThread" -> newPerTimerThread(() -> {
                LockSupport.parkNanos(1_000_000L);
                fired.countDown();
            }).start();
            default -> throw new IllegalArgumentException(variant);
        }
        fired.await();
    }

    private Thread newPerTimerThread(Runnable body) {
        if ("virtual".equals(threadMode)) {
            return Thread.ofVirtual().unstarted(body);
        }
        return Thread.ofPlatform().daemon(true).unstarted(body);
    }

    private ThreadFactory threadFactory(String prefix) {
        if ("virtual".equals(threadMode)) {
            return Thread.ofVirtual().name(prefix, 0).factory();
        }
        return Thread.ofPlatform().name(prefix, 0).daemon(true).factory();
    }
}
