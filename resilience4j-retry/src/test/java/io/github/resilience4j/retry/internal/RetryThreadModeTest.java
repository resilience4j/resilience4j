/*
 *
 * Copyright 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 *
 */
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.core.ExecutorServiceFactory;
import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
@ExtendWith(ThreadModeExtension.class)
class RetryThreadModeTest {

    private static final Duration WAIT_DURATION = Duration.ofMillis(50);
    private ScheduledExecutorService scheduler;

    @AfterEach
    void tearDown() {
        // Clean up scheduler
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @TestTemplate
    void shouldUseCorrectThreadTypeForAsyncRetry(ThreadType threadType) throws Exception {
        boolean isVirtual = threadType == ThreadType.VIRTUAL;

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
        ExecutorService taskExecutor = isVirtual ?
            Executors.newVirtualThreadPerTaskExecutor() :
            Executors.newSingleThreadExecutor();

        try {
            Supplier<CompletionStage<String>> supplier = () -> {
                // Submit the actual work to appropriate thread type
                return CompletableFuture.supplyAsync(() -> {
                    int currentAttempt = attempts.incrementAndGet();

                    // Track if we're running on the expected thread type
                    boolean expectedThreadType = isVirtual ?
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
                boolean isExpectedType = isVirtual ?
                    Thread.currentThread().isVirtual() :
                    !Thread.currentThread().isVirtual();
                threadTypeFuture.complete(isExpectedType);
            });

            Boolean usedExpectedThreadType = threadTypeFuture.get(1, TimeUnit.SECONDS);
            assertThat(usedExpectedThreadType)
                .as("Retry's scheduler should use " + threadType + " threads")
                .isTrue();
        } finally {
            taskExecutor.shutdownNow();
        }
    }

    @TestTemplate
    void shouldHandleHighConcurrencyInBothThreadModes(ThreadType threadType) throws Exception {
        boolean isVirtual = threadType == ThreadType.VIRTUAL;

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
        try (ExecutorService executor = isVirtual ?
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
                        decoratedSupplier.get().toCompletableFuture().get(5, TimeUnit.SECONDS);

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
