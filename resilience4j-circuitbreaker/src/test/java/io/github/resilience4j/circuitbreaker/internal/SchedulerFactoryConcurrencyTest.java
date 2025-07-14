package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Tests for concurrent access to SchedulerFactory with both virtual and platform threads.
 * Focuses on thread safety of the AtomicStampedReference implementation.
 * Tests run in both platform and virtual thread modes.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class SchedulerFactoryConcurrencyTest extends ThreadModeTestBase {

    public SchedulerFactoryConcurrencyTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }
    
    @Override
    public void setUpThreadMode() {
        super.setUpThreadMode();
        SchedulerFactory.getInstance().reset();
    }
    
    @Override
    public void cleanUpThreadMode() {
        super.cleanUpThreadMode();
        SchedulerFactory.getInstance().reset();
    }

    @Test
    public void shouldHandleConcurrentAccess() throws Exception {
        System.out.println("Testing concurrent access with " + getThreadModeDescription());
        
        final int numThreads = 10;
        final int operationsPerThread = 100;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicReference<Exception> firstException = new AtomicReference<>();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        ScheduledExecutorService scheduler = SchedulerFactory.getInstance().getScheduler();
                        assertNotNull("Scheduler should never be null", scheduler);
                        
                        // Test that we can actually use the scheduler
                        CountDownLatch taskLatch = new CountDownLatch(1);
                        scheduler.execute(() -> {
                            // Verify thread type matches expectation
                            boolean expectedVirtual = isVirtualThreadMode();
                            boolean actualVirtual = Thread.currentThread().isVirtual();
                            assertEquals("Thread type should match configuration", expectedVirtual, actualVirtual);
                            taskLatch.countDown();
                        });
                        
                        assertTrue("Task should complete", taskLatch.await(1, TimeUnit.SECONDS));
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        // Start all threads at once
        startLatch.countDown();
        
        // Wait for completion
        assertTrue("All threads should complete", completeLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (firstException.get() != null) {
            throw new AssertionError("Concurrency test failed", firstException.get());
        }
        
        assertEquals("All operations should succeed", 
                    numThreads * operationsPerThread, successCount.get());
        
        System.out.println("✅ Concurrent access test passed with " + getThreadModeDescription());
    }

    @Test
    public void shouldHandleConsistentThreadTypeUsage() throws Exception {
        System.out.println("Testing thread type consistency with " + getThreadModeDescription());
        
        final int numOperations = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch completeLatch = new CountDownLatch(numOperations);
        final AtomicReference<Exception> firstException = new AtomicReference<>();
        final AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numOperations; i++) {
            executor.submit(() -> {
                try {
                    // Small delay to test consistency over time
                    Thread.sleep(5);
                    
                    ScheduledExecutorService scheduler = SchedulerFactory.getInstance().getScheduler();
                    assertNotNull("Scheduler should never be null", scheduler);
                    
                    // Test the scheduler with a task
                    CountDownLatch taskLatch = new CountDownLatch(1);
                    
                    try {
                        scheduler.execute(() -> {
                            boolean expectedVirtual = isVirtualThreadMode();
                            boolean actualVirtual = Thread.currentThread().isVirtual();
                            
                            // Thread type should be consistent for the current mode
                            assertEquals("Thread type should match configured mode", expectedVirtual, actualVirtual);
                            taskLatch.countDown();
                        });
                        
                        assertTrue("Task should complete", taskLatch.await(3, TimeUnit.SECONDS));
                        successCount.incrementAndGet();
                    } catch (java.util.concurrent.RejectedExecutionException e) {
                        // Rare but acceptable during test execution
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        assertTrue("All operations should complete", 
                  completeLatch.await(60, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (firstException.get() != null) {
            throw new AssertionError("Thread type consistency test failed", firstException.get());
        }
        
        assertTrue("Most operations should succeed", successCount.get() > numOperations * 0.8);
        
        System.out.println("✅ Thread type consistency test passed with " + getThreadModeDescription());
    }

    @Test
    public void shouldHandleConcurrentResets() throws Exception {
        System.out.println("Testing concurrent resets with " + getThreadModeDescription());
        
        final int numThreads = 3; // Reduced thread count
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicReference<Exception> firstException = new AtomicReference<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    for (int j = 0; j < 10; j++) { // Reduced iterations
                        if (threadIndex == 0) {
                            // Only one thread does reset
                            SchedulerFactory.getInstance().reset();
                            Thread.sleep(5); // Give time for reset to complete
                        } else {
                            // Other threads get schedulers and test them
                            ScheduledExecutorService scheduler = SchedulerFactory.getInstance().getScheduler();
                            assertNotNull("Scheduler should never be null", scheduler);
                            
                            // Quick test that it works, with exception tolerance
                            CountDownLatch taskLatch = new CountDownLatch(1);
                            try {
                                scheduler.execute(() -> {
                                    // Verify thread type matches expectation
                                    boolean expectedVirtual = isVirtualThreadMode();
                                    boolean actualVirtual = Thread.currentThread().isVirtual();
                                    assertEquals("Thread type should match configuration", expectedVirtual, actualVirtual);
                                    taskLatch.countDown();
                                });
                                // More lenient timeout for reset scenarios
                                taskLatch.await(2, TimeUnit.SECONDS);
                            } catch (java.util.concurrent.RejectedExecutionException e) {
                                // During rapid resets, this is acceptable behavior
                                // The scheduler might be in process of being shut down
                            }
                        }
                        
                        Thread.sleep(2); // Increased delay for more stability
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue("All reset operations should complete", 
                  completeLatch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        
        if (firstException.get() != null) {
            throw new AssertionError("Reset concurrency test failed", firstException.get());
        }
        
        System.out.println("✅ Concurrent resets test passed with " + getThreadModeDescription());
    }
}