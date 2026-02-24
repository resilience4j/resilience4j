package io.github.resilience4j.core;

import java.util.concurrent.locks.LockSupport;

/**
 * Utility class for CAS loop backoff strategies optimized for both Virtual and Platform threads.
 *
 * <p><strong>Strategy:</strong>
 * <ul>
 *   <li><strong>Spin phase</strong>: Uses {@code Thread.onSpinWait()} for CPU-efficient spinning (1-100 spins)</li>
 *   <li><strong>Park phase</strong>: Uses {@code LockSupport.parkNanos()} with thread-specific jitter (1-11μs)</li>
 *   <li><strong>Jittering</strong>: Thread ID-based deterministic jitter prevents thundering herd</li>
 * </ul>
 *
 * <p><strong>Performance Characteristics:</strong>
 * <ul>
 *   <li>Simple and Fast: Minimal computational overhead</li>
 *   <li>Deterministic: Predictable behavior based on thread ID</li>
 *   <li>Distributed: Different threads get different backoff timings</li>
 * </ul>
 *
 * <p><strong>Virtual Thread Compatibility:</strong>
 * This implementation is optimized for Java 21+ virtual threads (Project Loom):
 * <ul>
 *   <li>{@code LockSupport.parkNanos()} allows virtual threads to park without blocking carrier threads.
 *       Virtual threads unmount from their carrier during park operations, enabling high concurrency.</li>
 *   <li>No {@code ThreadLocal} usage avoids memory overhead when running millions of virtual threads.
 *       Each ThreadLocal value is copied per virtual thread, leading to high memory consumption.</li>
 *   <li>{@code Thread.onSpinWait()} is a JVM hint that works safely with both virtual and platform threads.</li>
 *   <li>Thread ID-based jittering provides deterministic backoff without thread-local state.</li>
 * </ul>
 *
 * <p><strong>Performance Tuning:</strong>
 * Default parameters (100 spins, 1-11μs park) are tuned for typical Resilience4j lock-free metrics
 * workloads under moderate contention. For different workload characteristics:
 * <ul>
 *   <li><strong>High contention</strong>: Reduce {@code MAX_SPIN_COUNT} or increase {@code PARK_STEP_NANOS}
 *       to yield earlier and reduce CPU spinning overhead</li>
 *   <li><strong>Low contention</strong>: Increase {@code MAX_SPIN_COUNT} to reduce park overhead,
 *       as CAS operations are more likely to succeed quickly</li>
 *   <li><strong>Always profile before changing</strong>: Use JFR (Java Flight Recorder), async-profiler,
 *       or similar tools to measure actual contention and latency before tuning</li>
 * </ul>
 *
 * @author kanghyun.yang
 * @since 3.0.0
 * @see <a href="https://openjdk.org/jeps/444">JEP 444: Virtual Threads</a>
 */
public final class CASBackoffUtil {

    /**
     * Maximum number of spin attempts before yielding to scheduler.
     * Optimized for typical CAS contention scenarios.
     */
    private static final int MAX_SPIN_COUNT = 100;

    /**
     * Minimum park time in nanoseconds for yield strategy.
     * 1000ns = 1µs provides meaningful yield without excessive latency.
     */
    private static final long MIN_PARK_NANOS = 1_000L;

    /**
     * Park time step in nanoseconds for thread-specific jitter.
     * Each thread offset adds this amount to the park duration.
     */
    private static final long PARK_STEP_NANOS = 1_000L;

    /**
     * Range for thread ID-based jitter calculation.
     * Produces offsets from 0 to (JITTER_RANGE-1), distributing threads across different park durations.
     */
    private static final int JITTER_RANGE = 11;

    /**
     * Maximum park time in nanoseconds for yield strategy.
     * Calculated as MIN_PARK_NANOS + (JITTER_RANGE-1) * PARK_STEP_NANOS = 11µs.
     */
    private static final long MAX_PARK_NANOS =
        MIN_PARK_NANOS + (JITTER_RANGE - 1) * PARK_STEP_NANOS;
    
    private CASBackoffUtil() {
        // Utility class - prevent instantiation
    }
    
    
    /**
     * Performs backoff strategy for CAS loops optimized for both Virtual and Platform threads.
     *
     * <p>This method implements a two-phase backoff strategy:
     * <ol>
     *   <li><strong>Spin phase</strong>: When spin count is below threshold, uses {@code Thread.onSpinWait()}
     *       for CPU-efficient spinning without context switching</li>
     *   <li><strong>Park phase</strong>: When spin count exceeds threshold, uses {@code LockSupport.parkNanos()}
     *       with thread ID-based jitter (1-11μs) to prevent thundering herd</li>
     * </ol>
     *
     * <p>Thread ID-based jittering ensures:
     * <ul>
     *   <li>Deterministic behavior: Same thread always gets same backoff timing</li>
     *   <li>Distributed contention: Different threads park for different durations</li>
     *   <li>No ThreadLocal overhead: Uses thread ID directly for calculation</li>
     *   <li>Thundering herd prevention: Threads don't wake up simultaneously</li>
     * </ul>
     *
     * <p><strong>Usage Pattern:</strong>
     * <pre>{@code
     * int spinCount = 0;
     * while (!casSucceeded) {
     *     spinCount = CASBackoffUtil.performBackoff(spinCount);
     *     // Retry CAS operation
     *     casSucceeded = FIELD.compareAndSet(obj, expectedValue, newValue);
     * }
     * }</pre>
     *
     * @param spinCount current spin count (must start from 0)
     * @return new spin count after backoff (incremented if below threshold, or reset to 0 after park)
     */
    public static int performBackoff(int spinCount) {
        if (spinCount < MAX_SPIN_COUNT) {
            // Spin phase: CPU-friendly spinning without context switch
            Thread.onSpinWait();
            return spinCount + 1;
        } else {
            // Park phase: Thread ID-based jitter prevents thundering herd
            // Use thread ID for better distribution and less collision than hashCode()
            long threadId = Thread.currentThread().threadId();
            int threadOffset = (int) (Math.abs(threadId) % JITTER_RANGE); // 0-10

            // Actual park time: MIN_PARK_NANOS to MAX_PARK_NANOS based on thread ID
            long parkTime = MIN_PARK_NANOS + threadOffset * PARK_STEP_NANOS;
            LockSupport.parkNanos(parkTime); // Virtual thread friendly yield
            return 0; // Reset spin count after yielding
        }
    }
    
    /**
     * Gets the maximum spin count threshold.
     * @return maximum spin count before yielding
     */
    public static int getMaxSpinCount() {
        return MAX_SPIN_COUNT;
    }

    /**
     * Gets the maximum park time in nanoseconds.
     * This represents the upper bound of the thread ID-based jitter range.
     *
     * @return maximum park time in nanoseconds (11μs with default parameters)
     * @since 3.0.0
     */
    public static long getMaxParkNanos() {
        return MAX_PARK_NANOS;
    }
}