package io.github.resilience4j.circuitbreaker.internal;

import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for concurrent access to SchedulerFactory with both virtual and platform threads.
 * Focuses on thread safety of the AtomicStampedReference implementation.
 * Tests run in both platform and virtual thread modes.
 *
 * @author kanghyun.yang
 * @since 3.0.0
 */
@ExtendWith(ThreadModeExtension.class)
class SchedulerFactoryConcurrencyTest {

    private static final Logger LOG = LoggerFactory.getLogger(SchedulerFactoryConcurrencyTest.class);

    @BeforeEach
    void setUp() {
        SchedulerFactory.getInstance().reset();
    }

    @AfterEach
    void tearDown() {
        SchedulerFactory.getInstance().reset();
    }

    @TestTemplate
    void shouldHandleConcurrentAccess(ThreadType threadType) throws Exception {
        LOG.info("Testing concurrent access with {}", threadType);

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
                        assertThat(scheduler).as("Scheduler should never be null").isNotNull();

                        CountDownLatch taskLatch = new CountDownLatch(1);
                        scheduler.execute(() -> {
                            boolean expectedVirtual = threadType == ThreadType.VIRTUAL;
                            boolean actualVirtual = Thread.currentThread().isVirtual();
                            assertThat(actualVirtual)
                                .as("Thread type should match configuration")
                                .isEqualTo(expectedVirtual);
                            taskLatch.countDown();
                        });

                        assertThat(taskLatch.await(1, TimeUnit.SECONDS))
                            .as("Task should complete")
                            .isTrue();
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();

        assertThat(completeLatch.await(30, TimeUnit.SECONDS))
            .as("All threads should complete")
            .isTrue();
        executor.shutdown();

        if (firstException.get() != null) {
            throw new AssertionError("Concurrency test failed", firstException.get());
        }

        assertThat(successCount.get())
            .as("All operations should succeed")
            .isEqualTo(numThreads * operationsPerThread);

        LOG.info("Concurrent access test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleConsistentThreadTypeUsage(ThreadType threadType) throws Exception {
        LOG.info("Testing thread type consistency with {}", threadType);

        final int numOperations = 20;
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final CountDownLatch completeLatch = new CountDownLatch(numOperations);
        final AtomicReference<Exception> firstException = new AtomicReference<>();
        final AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < numOperations; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(5);

                    ScheduledExecutorService scheduler = SchedulerFactory.getInstance().getScheduler();
                    assertThat(scheduler).as("Scheduler should never be null").isNotNull();

                    CountDownLatch taskLatch = new CountDownLatch(1);
                    try {
                        scheduler.execute(() -> {
                            boolean expectedVirtual = threadType == ThreadType.VIRTUAL;
                            boolean actualVirtual = Thread.currentThread().isVirtual();
                            assertThat(actualVirtual)
                                .as("Thread type should match configured mode")
                                .isEqualTo(expectedVirtual);
                            taskLatch.countDown();
                        });

                        assertThat(taskLatch.await(3, TimeUnit.SECONDS))
                            .as("Task should complete")
                            .isTrue();
                        successCount.incrementAndGet();
                    } catch (RejectedExecutionException e) {
                        // Rare but acceptable during test execution
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        assertThat(completeLatch.await(60, TimeUnit.SECONDS))
            .as("All operations should complete")
            .isTrue();
        executor.shutdown();

        if (firstException.get() != null) {
            throw new AssertionError("Thread type consistency test failed", firstException.get());
        }

        assertThat(successCount.get())
            .as("Most operations should succeed")
            .isGreaterThan((int) (numOperations * 0.8));

        LOG.info("Thread type consistency test passed with {}", threadType);
    }

    @TestTemplate
    void shouldHandleConcurrentResets(ThreadType threadType) throws Exception {
        LOG.info("Testing concurrent resets with {}", threadType);

        final int numThreads = 3;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch completeLatch = new CountDownLatch(numThreads);
        final AtomicReference<Exception> firstException = new AtomicReference<>();

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    for (int j = 0; j < 10; j++) {
                        if (threadIndex == 0) {
                            // Only one thread does reset
                            SchedulerFactory.getInstance().reset();
                            Thread.sleep(5);
                        } else {
                            ScheduledExecutorService scheduler = SchedulerFactory.getInstance().getScheduler();
                            assertThat(scheduler).as("Scheduler should never be null").isNotNull();

                            CountDownLatch taskLatch = new CountDownLatch(1);
                            try {
                                scheduler.execute(() -> {
                                    boolean expectedVirtual = threadType == ThreadType.VIRTUAL;
                                    boolean actualVirtual = Thread.currentThread().isVirtual();
                                    assertThat(actualVirtual)
                                        .as("Thread type should match configuration")
                                        .isEqualTo(expectedVirtual);
                                    taskLatch.countDown();
                                });
                                taskLatch.await(2, TimeUnit.SECONDS);
                            } catch (RejectedExecutionException e) {
                                // During rapid resets, this is acceptable behavior
                            }
                        }

                        Thread.sleep(2);
                    }
                } catch (Exception e) {
                    firstException.compareAndSet(null, e);
                } finally {
                    completeLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(completeLatch.await(30, TimeUnit.SECONDS))
            .as("All reset operations should complete")
            .isTrue();
        executor.shutdown();

        if (firstException.get() != null) {
            throw new AssertionError("Reset concurrency test failed", firstException.get());
        }

        LOG.info("Concurrent resets test passed with {}", threadType);
    }
}
