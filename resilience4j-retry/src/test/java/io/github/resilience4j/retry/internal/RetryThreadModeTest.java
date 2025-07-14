package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that verify Retry correctly uses both platform and virtual threads
 * based on the system property {@code resilience4j.thread.type} configuration.
 * 
 * @author kanghyun.yang
 * @since 3.0.0
 */
@RunWith(Parameterized.class)
public class RetryThreadModeTest extends ThreadModeTestBase {

    private static final Duration WAIT_DURATION = Duration.ofMillis(50);
    private ScheduledExecutorService scheduler;

    public RetryThreadModeTest(ThreadType threadType) {
        super(threadType);
    }

    @Parameterized.Parameters(name = "{0} thread mode")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    @Before
    public void setUp() {
        // Reset the scheduler for each test (ThreadModeTestBase handles thread mode setup)
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @After
    public void tearDown() {
        // Clean up scheduler (ThreadModeTestBase handles thread mode cleanup)
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void shouldUseCorrectThreadTypeForAsyncRetry() throws Exception {
        // Thread mode configured automatically by ThreadModeTestBase
        
        // Create scheduler via ExecutorServiceFactory which should use the configured thread type
        scheduler = ExecutorServiceFactory.newSingleThreadScheduledExecutor("retry-" + threadType + "-test");
        
        // Create Retry with configuration to retry 3 times
        RetryConfig config = RetryConfig.<String>custom()
            .maxAttempts(3)
            .waitDuration(WAIT_DURATION)
            .build();
        Retry retry = Retry.of(threadType + "Test", config);
        
        // Track retry attempts and thread types
        AtomicInteger attempts = new AtomicInteger(0);
        AtomicBoolean allCorrectThreadType = new AtomicBoolean(true);
        
        // Create a supplier that will fail twice then succeed, tracking thread types
        // Use appropriate executor based on thread mode
        ExecutorService taskExecutor = isVirtualThreadMode() ? 
            Executors.newVirtualThreadPerTaskExecutor() : 
            Executors.newSingleThreadExecutor();
        
        Supplier<CompletionStage<String>> supplier = () -> {
            // Submit the actual work to appropriate thread type
            return CompletableFuture.supplyAsync(() -> {
                int currentAttempt = attempts.incrementAndGet();
                
                // Track if we're running on the expected thread type
                boolean expectedThreadType = isVirtualThreadMode() ? 
                    Thread.currentThread().isVirtual() : 
                    !Thread.currentThread().isVirtual();
                if (!expectedThreadType) {
                    allCorrectThreadType.set(false);
                }
                
                // Return success or throw exception based on attempt count
                if (currentAttempt < 3) {
                    // First two attempts fail
                    throw new RuntimeException("Retry attempt: " + currentAttempt);
                } else {
                    // Third attempt succeeds
                    return "Success on attempt: " + currentAttempt;
                }
            }, taskExecutor);
        };
        
        // Decorate the supplier with Retry
        Supplier<CompletionStage<String>> decoratedSupplier = Retry.decorateCompletionStage(
            retry, scheduler, supplier);
        
        // Execute and get result
        CompletionStage<String> stage = decoratedSupplier.get();
        String result = stage.toCompletableFuture().get(5, TimeUnit.SECONDS);
        
        // Verify we retried the correct number of times
        assertThat(attempts.get()).isEqualTo(3);
        
        // Verify the result is correct
        assertThat(result).isEqualTo("Success on attempt: 3");
        
        // Verify all executions used the expected thread type
        assertThat(allCorrectThreadType.get())
            .as("All retry attempts should run on " + threadType + " threads")
            .isTrue();
            
        // Verify the scheduler is using the expected thread type
        CompletableFuture<Boolean> threadTypeFuture = new CompletableFuture<>();
        scheduler.execute(() -> {
            boolean isExpectedType = isVirtualThreadMode() ? 
                Thread.currentThread().isVirtual() : 
                !Thread.currentThread().isVirtual();
            threadTypeFuture.complete(isExpectedType);
        });
        
        Boolean usedExpectedThreadType = threadTypeFuture.get(1, TimeUnit.SECONDS);
        assertThat(usedExpectedThreadType)
            .as("Retry's scheduler should use " + threadType + " threads")
            .isTrue();
    }
    
    
    @Test
    public void shouldHandleHighConcurrencyInBothThreadModes() throws Exception {
        // Thread mode configured automatically by ThreadModeTestBase
        
        // Create scheduler via ExecutorServiceFactory which should use the configured thread type
        scheduler = ExecutorServiceFactory.newSingleThreadScheduledExecutor("retry-" + threadType + "-concurrency-test");
        
        // Create Retry with configuration for high concurrency testing
        RetryConfig config = RetryConfig.<String>custom()
            .maxAttempts(3)
            .waitDuration(WAIT_DURATION)
            .build();
        Retry retry = Retry.of(threadType + "ConcurrencyTest", config);
        
        // Number of concurrent operations to test
        final int CONCURRENT_TASKS = 100;
        
        // Track completion of all tasks
        CountDownLatch completionLatch = new CountDownLatch(CONCURRENT_TASKS);
        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);
        
        // Create and execute concurrent tasks using appropriate executor
        try (ExecutorService executor = isVirtualThreadMode() ? 
            Executors.newVirtualThreadPerTaskExecutor() : 
            Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < CONCURRENT_TASKS; i++) {
                final int taskId = i;
                
                executor.submit(() -> {
                    try {
                        // Each task retries up to 3 times with different outcomes based on taskId
                        AtomicInteger attemptCounter = new AtomicInteger(0);
                        
                        // Create a supplier with different behaviors based on taskId
                        Supplier<CompletionStage<String>> supplier = () -> {
                            int attempt = attemptCounter.incrementAndGet();
                            CompletableFuture<String> future = new CompletableFuture<>();
                            
                            // Different behavior based on task ID
                            // - Even tasks succeed on first try
                            // - Odd tasks < 50 succeed on second try
                            // - Odd tasks >= 50 succeed on third try
                            if (taskId % 2 == 0) {
                                // Even tasks succeed immediately
                                future.complete("Task " + taskId + " succeeded on attempt " + attempt);
                            } else if (taskId < 50) {
                                // Odd tasks < 50 succeed on second attempt
                                if (attempt >= 2) {
                                    future.complete("Task " + taskId + " succeeded on attempt " + attempt);
                                } else {
                                    future.completeExceptionally(new RuntimeException("Retry needed for task " + taskId));
                                }
                            } else {
                                // Odd tasks >= 50 succeed on third attempt
                                if (attempt >= 3) {
                                    future.complete("Task " + taskId + " succeeded on attempt " + attempt);
                                } else {
                                    future.completeExceptionally(new RuntimeException("Retry needed for task " + taskId));
                                }
                            }
                            
                            return future;
                        };
                        
                        // Decorate supplier with retry
                        Supplier<CompletionStage<String>> decoratedSupplier = Retry.decorateCompletionStage(
                            retry, scheduler, supplier);
                            
                        // Execute and wait for result
                        String result = decoratedSupplier.get().toCompletableFuture().get(5, TimeUnit.SECONDS);
                        
                        // Task succeeded
                        successCounter.incrementAndGet();
                    } catch (Exception e) {
                        // Task failed even after retries
                        failureCounter.incrementAndGet();
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            // Wait for all tasks to complete
            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);
            
            // Verify all tasks completed
            assertThat(completed)
                .as("All concurrent retry tasks should complete within timeout")
                .isTrue();
                
            // Verify all tasks succeeded
            assertThat(successCounter.get())
                .as("All tasks should eventually succeed with retries")
                .isEqualTo(CONCURRENT_TASKS);
                
            assertThat(failureCounter.get())
                .as("No tasks should fail")
                .isZero();
        }
    }
}