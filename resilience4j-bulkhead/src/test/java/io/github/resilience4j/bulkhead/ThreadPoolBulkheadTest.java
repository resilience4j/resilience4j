/*
 *
 *  Copyright 2026 Robert Winkler, Lucas Lech, Mahmoud Romeh
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.core.ThreadModeExtension;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.test.HelloWorldService;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@ExtendWith(ThreadModeExtension.class)
class ThreadPoolBulkheadTest {

    private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolBulkheadTest.class);

    private HelloWorldService helloWorldService;
    private ThreadPoolBulkheadConfig config;

    @BeforeEach
    void setUp() {
        Awaitility.reset();
        helloWorldService = mock(HelloWorldService.class);
        config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(1)
            .coreThreadPoolSize(1)
            .queueCapacity(1)
            .build();
    }

    @Test
    void shouldExecuteRunnableAndFailWithBulkHeadFull() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeRunnable(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            } catch (Exception e) {
                exception.set(e);
            }
        });
        first.start();

        Thread second = new Thread(() -> {
            try {
                bulkhead.executeRunnable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        second.start();

        Thread third = new Thread(() -> {
            try {
                bulkhead.executeRunnable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        third.start();

        first.join(100);
        second.join(100);
        third.join(100);

        final Exception caughtException = exception.get();

        assertThat(caughtException).isInstanceOf(BulkheadFullException.class);
        assertThat(((BulkheadFullException) caughtException).getBulkheadName()).isEqualTo(bulkhead.getName());
    }

    @Test
    void shouldExecuteSupplierAndFailWithBulkHeadFull() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeSupplier(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            } catch (Exception e) {
                exception.set(e);
            }
        });
        first.start();

        Thread second = new Thread(() -> {
            try {
                bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        second.start();

        Thread third = new Thread(() -> {
            try {
                bulkhead.executeSupplier(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        third.start();

        first.join(100);
        second.join(100);
        third.join(100);

        final Exception caughtException = exception.get();

        assertThat(caughtException).isInstanceOf(BulkheadFullException.class);
        assertThat(((BulkheadFullException) caughtException).getBulkheadName()).isEqualTo(bulkhead.getName());
    }

    @Test
    void shouldExecuteCallableAndFailWithBulkHeadFull() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeCallable(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                });
            } catch (Exception e) {
                exception.set(e);
            }
        });
        first.start();

        Thread second = new Thread(() -> {
            try {
                bulkhead.executeCallable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        second.start();

        Thread third = new Thread(() -> {
            try {
                bulkhead.executeCallable(helloWorldService::returnHelloWorld);
            } catch (Exception e) {
                exception.set(e);
            }
        });
        third.start();

        first.join(100);
        second.join(100);
        third.join(100);

        final Exception caughtException = exception.get();

        assertThat(caughtException).isInstanceOf(BulkheadFullException.class);
        assertThat(((BulkheadFullException) caughtException).getBulkheadName()).isEqualTo(bulkhead.getName());
    }

    @Test
    void shouldExecuteSupplierAndReturnWithSuccess() throws Exception {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        CompletionStage<String> result = bulkhead
            .executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result.toCompletableFuture().get()).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    void createWithNullConfig() {
        assertThatThrownBy(() -> ThreadPoolBulkhead.of("test", (ThreadPoolBulkheadConfig) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Config must not be null");
    }

    @TestTemplate
    void createThreadsUsingNameForPrefix(ThreadType threadType) throws Exception {
        LOG.info("Running createThreadsUsingNameForPrefix in {}", threadType);

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("TEST", config);
        Supplier<String> getThreadName = () -> Thread.currentThread().getName();

        CompletionStage<String> result = bulkhead.executeSupplier(getThreadName);
        String actualThreadName = result.toCompletableFuture().get();

        LOG.info("Thread name in {}: {}", threadType, actualThreadName);

        if (threadType == ThreadType.VIRTUAL) {
            // Virtual threads use naming pattern like "bulkhead-TEST-v-0"
            assertThat(actualThreadName).matches("bulkhead-TEST-.*");
            assertThat(actualThreadName).contains("TEST");
        } else {
            // Platform threads use traditional naming pattern like "bulkhead-TEST-1"
            assertThat(actualThreadName).isEqualTo("bulkhead-TEST-1");
        }
    }

    @Test
    void withSynchronousQueue() {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead
            .of("test", ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(2)
                .coreThreadPoolSize(1)
                .queueCapacity(0)
                .build());
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CountDownLatch latch = new CountDownLatch(1);

        bulkhead.executeRunnable(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        bulkhead.executeRunnable(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertThatThrownBy(() -> bulkhead.executeCallable(helloWorldService::returnHelloWorld))
            .isInstanceOf(BulkheadFullException.class)
            .hasFieldOrPropertyWithValue("bulkheadName", bulkhead.getName());

        assertThat(bulkhead.getMetrics().getQueueDepth()).isZero();
        assertThat(bulkhead.getMetrics().getRemainingQueueCapacity()).isZero();
        assertThat(bulkhead.getMetrics().getQueueCapacity()).isZero();
        assertThat(bulkhead.getMetrics().getActiveThreadCount()).isEqualTo(2);
        assertThat(bulkhead.getMetrics().getThreadPoolSize()).isEqualTo(2);

        latch.countDown();
    }

    @TestTemplate
    void shouldUseVirtualThreadsWhenConfigured(ThreadType threadType) throws Exception {
        LOG.info("Running shouldUseVirtualThreadsWhenConfigured in {}", threadType);

        assumeTrue(threadType == ThreadType.VIRTUAL,
            "Virtual thread specific test only runs in virtual thread mode");

        ThreadPoolBulkheadConfig testConfig = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(5)
            .coreThreadPoolSize(1)
            .queueCapacity(5)
            .keepAliveDuration(Duration.ofMillis(100))
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("virtualThreadTest-" + threadType, testConfig);

        try {
            AtomicBoolean ranOnVirtualThread = new AtomicBoolean(false);

            CompletableFuture<Boolean> future = bulkhead.submit(() -> {
                ranOnVirtualThread.set(Thread.currentThread().isVirtual());
                return true;
            }).toCompletableFuture();

            boolean result = future.get(1, TimeUnit.SECONDS);

            assertThat(result)
                .as("Task should complete successfully in %s", threadType)
                .isTrue();
            assertThat(ranOnVirtualThread.get())
                .as("Task should execute on a virtual thread when configured in %s", threadType)
                .isTrue();

            LOG.info("Virtual thread configuration test passed in {}", threadType);
        } finally {
            bulkhead.close();
        }
    }

    @TestTemplate
    void shouldHandleConcurrentTasksCorrectlyInBothThreadModes(ThreadType threadType) throws Exception {
        LOG.info("Running shouldHandleConcurrentTasksCorrectlyInBothThreadModes in {}", threadType);

        ThreadPoolBulkheadConfig testConfig = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(5)
            .coreThreadPoolSize(1)
            .queueCapacity(5)
            .keepAliveDuration(Duration.ofMillis(100))
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("concurrencyTest-" + threadType, testConfig);

        try {
            int numberOfTasks = 15; // 5 (maxThreadPoolSize) + 5 (queueCapacity) + 5 (excess)
            CompletableFuture<?>[] futures = new CompletableFuture<?>[numberOfTasks];
            AtomicInteger rejectedTasks = new AtomicInteger(0);
            AtomicInteger completedTasks = new AtomicInteger(0);
            CountDownLatch tasksLatch = new CountDownLatch(numberOfTasks);

            for (int i = 0; i < numberOfTasks; i++) {
                final int taskId = i;
                try {
                    futures[i] = bulkhead.submit(() -> {
                        try {
                            Thread.sleep(50);
                            completedTasks.incrementAndGet();
                            return taskId;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return -1;
                        }
                    }).toCompletableFuture();
                    futures[i].whenComplete((result, error) -> tasksLatch.countDown());
                } catch (BulkheadFullException e) {
                    rejectedTasks.incrementAndGet();
                    futures[i] = CompletableFuture.completedFuture(-1);
                    tasksLatch.countDown();
                }
            }

            assertThat(tasksLatch.await(10, TimeUnit.SECONDS))
                .as("All tasks should be completed or rejected within timeout in %s", threadType)
                .isTrue();
            assertThat(completedTasks.get())
                .as("Number of completed tasks should match thread pool and queue capacity in %s", threadType)
                .isLessThanOrEqualTo(10);
            assertThat(rejectedTasks.get())
                .as("Some tasks should be rejected when exceeding capacity in %s", threadType)
                .isGreaterThan(0);
            assertThat(completedTasks.get() + rejectedTasks.get())
                .as("Total tasks (completed + rejected) should equal the number of submitted tasks in %s", threadType)
                .isEqualTo(numberOfTasks);

            LOG.info("Concurrency test passed in {} - Completed: {}, Rejected: {}",
                threadType, completedTasks.get(), rejectedTasks.get());
        } finally {
            bulkhead.close();
        }
    }

    @TestTemplate
    void shouldHaveCorrectMetricsInBothThreadModes(ThreadType threadType) throws Exception {
        LOG.info("Running shouldHaveCorrectMetricsInBothThreadModes in {}", threadType);

        ThreadPoolBulkheadConfig testConfig = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(5)
            .coreThreadPoolSize(1)
            .queueCapacity(5)
            .keepAliveDuration(Duration.ofMillis(100))
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("metricsTest-" + threadType, testConfig);

        try {
            ThreadPoolBulkhead.Metrics metrics = bulkhead.getMetrics();

            assertThat(metrics.getCoreThreadPoolSize())
                .as("Core thread pool size should be correct in %s", threadType)
                .isEqualTo(1);
            assertThat(metrics.getMaximumThreadPoolSize())
                .as("Maximum thread pool size should be correct in %s", threadType)
                .isEqualTo(5);
            assertThat(metrics.getQueueCapacity())
                .as("Queue capacity should be correct in %s", threadType)
                .isEqualTo(5);

            CountDownLatch latch = new CountDownLatch(1);
            bulkhead.submit(() -> {
                try {
                    Thread.sleep(100);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                } finally {
                    latch.countDown();
                }
            });

            assertThat(latch.await(1, TimeUnit.SECONDS))
                .as("Task should complete within timeout in %s", threadType)
                .isTrue();

            LOG.info("Metrics test passed in {}", threadType);
        } finally {
            bulkhead.close();
        }
    }
}
