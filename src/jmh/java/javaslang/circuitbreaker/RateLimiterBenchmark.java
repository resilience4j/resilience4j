package javaslang.circuitbreaker;

import javaslang.ratelimiter.RateLimiter;
import javaslang.ratelimiter.RateLimiterConfig;
import javaslang.ratelimiter.internal.AtomicRateLimiter;
import javaslang.ratelimiter.internal.SemaphoreBasedRateLimiter;
import javaslang.ratelimiter.internal.TimeBasedRateLimiter;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.All)
public class RateLimiterBenchmark {

    public static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 10;
    private static final int ITERATION_COUNT = 5;
    private static final int THREAD_COUNT = 2;

    private RateLimiter semaphoreBasedRateLimiter;
    private RateLimiter timeBasedRateLimiter;
    private RateLimiter atomicRateLimiter;

    private Supplier<String> semaphoreGuardedSupplier;
    private Supplier<String> timeGuardedSupplier;
    private Supplier<String> atomicGuardedSupplier;

    @Setup
    public void setUp() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.builder()
            .limitForPeriod(Integer.MAX_VALUE)
            .limitRefreshPeriod(Duration.ofNanos(10))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        semaphoreBasedRateLimiter = new SemaphoreBasedRateLimiter("semaphoreBased", rateLimiterConfig);
        timeBasedRateLimiter = new TimeBasedRateLimiter("timeBased", rateLimiterConfig);
        atomicRateLimiter = new AtomicRateLimiter("atomicBased", rateLimiterConfig);

        Supplier<String> stringSupplier = () -> {
            Blackhole.consumeCPU(1);
            return "Hello Benchmark";
        };
        semaphoreGuardedSupplier = RateLimiter.decorateSupplier(semaphoreBasedRateLimiter, stringSupplier);
        timeGuardedSupplier = RateLimiter.decorateSupplier(timeBasedRateLimiter, stringSupplier);
        atomicGuardedSupplier = RateLimiter.decorateSupplier(atomicRateLimiter, stringSupplier);
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String semaphoreBasedPermission() {
        return semaphoreGuardedSupplier.get();
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String timeBasedPermission() {
        return timeGuardedSupplier.get();
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public String atomicPermission() {
        return atomicGuardedSupplier.get();
    }
}