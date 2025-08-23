package io.github.resilience4j.core;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CASBackoffUtil.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
public class CASBackoffUtilTest {

    @Test
    public void testSpinCountIncrement() {
        // Test that spin count increments and eventually yields (with randomization)
        int spinCount = 0;
        int maxIterations = CASBackoffUtil.getMaxSpinCount() + 50; // Allow for randomization
        boolean yielded = false;
        
        for (int i = 0; i < maxIterations && !yielded; i++) {
            int newSpinCount = CASBackoffUtil.performBackoff(spinCount);
            if (newSpinCount == 0) {
                // Yield occurred - this is expected behavior
                yielded = true;
            } else {
                // Spin count should increment by 1
                assertThat(newSpinCount).isEqualTo(spinCount + 1);
                spinCount = newSpinCount;
            }
        }
        
        // Should eventually yield due to randomized max spin count
        assertThat(yielded).isTrue();
    }

    @Test
    public void testSpinCountReset() {
        // Test that spin count resets to 0 after exceeding randomized threshold
        int spinCount = CASBackoffUtil.getMaxSpinCount() + 20; // Definitely over any randomized threshold
        
        int newSpinCount = CASBackoffUtil.performBackoff(spinCount);
        
        assertThat(newSpinCount).isEqualTo(0);
    }

    @Test
    public void testFullBackoffCycle() {
        // Test complete backoff cycle from 0 to reset (with randomization)
        int spinCount = 0;
        int maxIterations = CASBackoffUtil.getMaxSpinCount() + 50; // Allow for randomization
        boolean yielded = false;
        
        // Increment until yield occurs
        for (int i = 0; i < maxIterations && !yielded; i++) {
            spinCount = CASBackoffUtil.performBackoff(spinCount);
            if (spinCount == 0) {
                yielded = true;
            }
        }
        
        assertThat(yielded).isTrue();
        assertThat(spinCount).isEqualTo(0);
        
        // Should start incrementing again
        spinCount = CASBackoffUtil.performBackoff(spinCount);
        assertThat(spinCount).isEqualTo(1);
    }

    @Test
    public void testBackoffUnderConcurrency() throws Exception {
        // Test backoff behavior under concurrent access
        int threadCount = 10;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        AtomicInteger completedOperations = new AtomicInteger(0);
        
        try {
            CompletableFuture<?>[] futures = new CompletableFuture[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    int localSpinCount = 0;
                    
                    for (int j = 0; j < iterationsPerThread; j++) {
                        // Simulate CAS loop with backoff
                        localSpinCount = CASBackoffUtil.performBackoff(localSpinCount);
                        
                        // Reset randomly to simulate successful CAS operations
                        if (j % 50 == 0) {
                            localSpinCount = 0;
                        }
                        
                        completedOperations.incrementAndGet();
                    }
                }, executor);
            }
            
            CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
            
            // Verify all operations completed successfully
            assertThat(completedOperations.get()).isEqualTo(threadCount * iterationsPerThread);
            
        } finally {
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testVirtualThreadCompatibility() throws Exception {
        if (!supportsVirtualThreads()) {
            System.out.println("Virtual threads not supported, skipping test");
            return;
        }
        
        // Test that backoff works correctly with virtual threads
        ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
        AtomicInteger operations = new AtomicInteger(0);
        AtomicInteger yieldCount = new AtomicInteger(0);
        
        try {
            CompletableFuture<?>[] futures = new CompletableFuture[100];
            
            for (int i = 0; i < 100; i++) {
                futures[i] = CompletableFuture.runAsync(() -> {
                    int spinCount = 0;
                    
                    // Simulate contended CAS loop
                    for (int j = 0; j < 200; j++) {
                        int newSpinCount = CASBackoffUtil.performBackoff(spinCount);
                        
                        // With randomization, spin count should reset to 0 after yielding
                        if (newSpinCount == 0) {
                            yieldCount.incrementAndGet();
                            spinCount = 0;
                        } else {
                            // Spin count should increment within reasonable bounds
                            // Allow for randomized threshold (MAX_SPIN_COUNT ± 10)
                            assertThat(newSpinCount).isLessThanOrEqualTo(CASBackoffUtil.getMaxSpinCount() + 20);
                            spinCount = newSpinCount;
                        }
                        
                        operations.incrementAndGet();
                    }
                }, virtualExecutor);
            }
            
            CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
            
            assertThat(operations.get()).isEqualTo(100 * 200);
            // Should have some yield operations due to randomized backoff
            assertThat(yieldCount.get()).isGreaterThan(0);
            
        } finally {
            virtualExecutor.shutdown();
            virtualExecutor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void testMaxSpinCountConstant() {
        // Test that MAX_SPIN_COUNT is a reasonable value
        int maxSpinCount = CASBackoffUtil.getMaxSpinCount();
        
        assertThat(maxSpinCount).isGreaterThan(0);
        assertThat(maxSpinCount).isLessThanOrEqualTo(1000); // Reasonable upper bound
        assertThat(maxSpinCount).isEqualTo(100); // Current expected value
    }

    @Test
    public void testBackoffWithZeroSpinCount() {
        // Test backoff starting from 0
        int result = CASBackoffUtil.performBackoff(0);
        assertThat(result).isEqualTo(1);
    }

    @Test
    public void testBackoffWithNegativeSpinCount() {
        // Test backoff with negative spin count (edge case)
        int result = CASBackoffUtil.performBackoff(-1);
        assertThat(result).isEqualTo(0); // Should increment to 0
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
}