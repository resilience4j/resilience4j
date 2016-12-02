package javaslang.circuitbreaker;

import javaslang.ratelimiter.RateLimiter;
import javaslang.ratelimiter.RateLimiterConfig;
import javaslang.ratelimiter.internal.AtomicRateLimiter;
import javaslang.ratelimiter.internal.SemaphoreBasedRateLimiter;
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
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

@State(Scope.Benchmark)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@BenchmarkMode(Mode.Throughput)
public class RateLimiterBenchmark {

    public static final int FORK_COUNT = 2;
    private static final int WARMUP_COUNT = 10;
    private static final int ITERATION_COUNT = 5;
    private static final int THREAD_COUNT = 2;

    private RateLimiter semaphoreBasedRateLimiter;
    private AtomicRateLimiter atomicRateLimiter;
    private AtomicRateLimiter.State state;
    private static final Object mutex = new Object();

    private Supplier<String> semaphoreGuardedSupplier;
    private Supplier<String> atomicGuardedSupplier;

    @Setup
    public void setUp() {
        RateLimiterConfig rateLimiterConfig = RateLimiterConfig.builder()
            .limitForPeriod(Integer.MAX_VALUE)
            .limitRefreshPeriod(Duration.ofNanos(10))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
        semaphoreBasedRateLimiter = new SemaphoreBasedRateLimiter("semaphoreBased", rateLimiterConfig);
        atomicRateLimiter = new AtomicRateLimiter("atomicBased", rateLimiterConfig);
        state = atomicRateLimiter.state.get();

        Supplier<String> stringSupplier = () -> {
            Blackhole.consumeCPU(1);
            return "Hello Benchmark";
        };
        semaphoreGuardedSupplier = RateLimiter.decorateSupplier(semaphoreBasedRateLimiter, stringSupplier);
        atomicGuardedSupplier = RateLimiter.decorateSupplier(atomicRateLimiter, stringSupplier);
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public void mutex(Blackhole bh) {
        synchronized (mutex) {
            state = atomicRateLimiter.calculateNextState(Duration.ZERO.toNanos(), state);
        }
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public void atomic(Blackhole bh) {
        atomicRateLimiter.state.updateAndGet(state -> {
            return atomicRateLimiter.calculateNextState(Duration.ZERO.toNanos(), state);
        });
    }

    @Benchmark
    @Threads(value = THREAD_COUNT)
    @Warmup(iterations = WARMUP_COUNT)
    @Fork(value = FORK_COUNT)
    @Measurement(iterations = ITERATION_COUNT)
    public void atomicBackOf(Blackhole bh) {
        AtomicRateLimiter.State prev;
        AtomicRateLimiter.State next;
        do {
            prev = atomicRateLimiter.state.get();
            next = atomicRateLimiter.calculateNextState(Duration.ZERO.toNanos(), prev);
        } while (!compareAndSet(prev, next));
    }

    /*
    https://arxiv.org/abs/1305.5800  https://dzone.com/articles/wanna-get-faster-wait-bit
     */
    public boolean compareAndSet(final AtomicRateLimiter.State current, final AtomicRateLimiter.State next) {
        if (atomicRateLimiter.state.compareAndSet(current, next)) {
            return true;
        } else {
            LockSupport.parkNanos(1);
            return false;
        }
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
    public String atomicPermission() {
        return atomicGuardedSupplier.get();
    }
}