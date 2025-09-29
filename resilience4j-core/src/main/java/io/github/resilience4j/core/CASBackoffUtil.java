package io.github.resilience4j.core;

import java.util.concurrent.locks.LockSupport;

/**
 * Utility class for CAS loop backoff strategies optimized for both Virtual and Platform threads.
 * 
 * Provides simple and efficient backoff mechanisms that:
 * - Use Thread.onSpinWait() for CPU-friendly spinning on platform threads
 * - Use LockSupport.parkNanos() for virtual thread-friendly yielding
 * - Prevent carrier thread pinning with ThreadLocal-free implementation
 * - Use deterministic Thread ID-based jittering to prevent thundering herd
 * - Maintain simplicity and predictable performance characteristics
 *
 * @author kanghyun.yang
 * @since 3.0.0
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
    private static final long MIN_PARK_NANOS = 1000L;
    
    /**
     * Maximum park time in nanoseconds for yield strategy.
     * 100µs provides upper bound to prevent excessive delays.
     */
    private static final long MAX_PARK_NANOS = 100_000L;
    
    private CASBackoffUtil() {
        // Utility class - prevent instantiation
    }
    
    
    /**
     * Performs backoff strategy for CAS loops optimized for both Virtual and Platform threads.
     * 
     * This method implements a simple thread ID-based backoff strategy:
     * 1. For low contention: Uses Thread.onSpinWait() for CPU-efficient spinning
     * 2. For high contention: Uses deterministic Thread ID-based timing to prevent thundering herd
     * 3. No ThreadLocal usage - fully Virtual Thread compatible
     * 4. Deterministic but distributed - prevents all threads from yielding simultaneously
     * 
     * Performance characteristics:
     * - Simple and Fast: Minimal computational overhead
     * - Virtual Thread Friendly: No ThreadLocal usage, no carrier thread pinning risk  
     * - Deterministic: Predictable behavior for testing and debugging
     * - Distributed: Thread ID-based jittering prevents thundering herd
     * 
     * @param spinCount current spin count (should start from 0)
     * @return new spin count after backoff (resets to 0 after yield)
     */
    public static int performBackoff(int spinCount) {
        // Use thread ID for deterministic but distributed jittering
        // This prevents thundering herd without ThreadLocal overhead
        int threadOffset = Math.abs(Thread.currentThread().hashCode()) % 21 - 10; // -10 to +10
        int maxSpin = MAX_SPIN_COUNT + threadOffset;
        
        if (spinCount < maxSpin) {
            Thread.onSpinWait(); // CPU-friendly spin waiting
            return spinCount + 1;
        } else {
            // Thread-specific park time: 1μs to 11μs based on thread ID
            long parkTime = MIN_PARK_NANOS + Math.abs(threadOffset) * 1000L;
            LockSupport.parkNanos(parkTime); // Virtual thread friendly yield
            return 0;
        }
    }
    
    /**
     * Gets the maximum spin count threshold.
     * @return maximum spin count before yielding
     */
    public static int getMaxSpinCount() {
        return MAX_SPIN_COUNT;
    }
}