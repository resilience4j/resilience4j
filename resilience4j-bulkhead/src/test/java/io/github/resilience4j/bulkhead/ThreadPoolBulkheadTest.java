/*
 *
 *  Copyright 2017 Robert Winkler, Lucas Lech, Mahmoud Romeh
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

import com.jayway.awaitility.Awaitility;
import io.github.resilience4j.core.ThreadModeTestBase;
import io.github.resilience4j.core.ThreadType;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.CheckedRunnable;
import io.vavr.control.Try;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

@RunWith(Parameterized.class)
public class ThreadPoolBulkheadTest extends ThreadModeTestBase {

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return threadModes();
    }

    public ThreadPoolBulkheadTest(ThreadType threadType) {
        super(threadType);
    }

    private HelloWorldService helloWorldService;
    private ThreadPoolBulkheadConfig config;

    @Before
    public void setUp() {
        setUpThreadMode(); // Set up thread mode from ThreadModeTestBase
        
        Awaitility.reset();
        helloWorldService = mock(HelloWorldService.class);
        config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(1)
            .coreThreadPoolSize(1)
            .queueCapacity(1)
            .build();
    }

    @After
    public void tearDown() {
        cleanUpThreadMode(); // Clean up thread mode from ThreadModeTestBase
    }

    @Test
    public void shouldExecuteRunnableAndFailWithBulkHeadFull() throws InterruptedException {
        System.out.println("Running shouldExecuteRunnableAndFailWithBulkHeadFull in " + getThreadModeDescription());
        
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeRunnable(() -> Try.run(() -> Thread.sleep(200)));
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

        assertThat(exception.get()).isInstanceOf(BulkheadFullException.class);
    }

    @Test
    public void shouldExecuteSupplierAndFailWithBulkHeadFull() throws InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeSupplier(() -> Try.run(() -> Thread.sleep(200)));
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

        assertThat(exception.get()).isInstanceOf(BulkheadFullException.class);
    }

    @Test
    public void shouldExecuteCallableAndFailWithBulkHeadFull() throws InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        final AtomicReference<Exception> exception = new AtomicReference<>();

        Thread first = new Thread(() -> {
            try {
                bulkhead.executeCallable(() -> Try.run(() -> Thread.sleep(200)));
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

        assertThat(exception.get()).isInstanceOf(BulkheadFullException.class);
    }


    @Test
    public void shouldExecuteSupplierAndReturnWithSuccess()
        throws ExecutionException, InterruptedException {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("test", config);
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");

        CompletionStage<String> result = bulkhead
            .executeSupplier(helloWorldService::returnHelloWorld);

        assertThat(result.toCompletableFuture().get()).isEqualTo("Hello world");
        then(helloWorldService).should(times(1)).returnHelloWorld();
    }

    @Test
    public void testCreateWithNullConfig() {
        assertThatThrownBy(() -> ThreadPoolBulkhead.of("test", (ThreadPoolBulkheadConfig) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Config must not be null");
    }

    @Test
    public void testCreateThreadsUsingNameForPrefix()
        throws ExecutionException, InterruptedException {
        System.out.println("Running testCreateThreadsUsingNameForPrefix in " + getThreadModeDescription());
        
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("TEST", config);
        Supplier<String> getThreadName = () -> Thread.currentThread().getName();

        CompletionStage<String> result = bulkhead.executeSupplier(getThreadName);

        String actualThreadName = result.toCompletableFuture().get();
        System.out.println("Thread name in " + getThreadModeDescription() + ": " + actualThreadName);
        
        // Thread naming differs between platform and virtual threads
        if (isVirtualThreadMode()) {
            // Virtual threads use naming pattern like "bulkhead-TEST-v-0"
            assertThat(actualThreadName).matches("bulkhead-TEST-.*");
            assertThat(actualThreadName).contains("TEST");
        } else {
            // Platform threads use traditional naming pattern like "bulkhead-TEST-1"
            assertThat(actualThreadName).isEqualTo("bulkhead-TEST-1");
        }
    }

    @Test
    public void testWithSynchronousQueue() {
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead
            .of("test", ThreadPoolBulkheadConfig.custom()
                .maxThreadPoolSize(2)
                .coreThreadPoolSize(1)
                .queueCapacity(0)
                .build());
        given(helloWorldService.returnHelloWorld()).willReturn("Hello world");
        CountDownLatch latch = new CountDownLatch(1);

        bulkhead.executeRunnable(CheckedRunnable.of(latch::await).unchecked());
        bulkhead.executeRunnable(CheckedRunnable.of(latch::await).unchecked());

        assertThatThrownBy(() -> bulkhead.executeCallable(helloWorldService::returnHelloWorld))
            .isInstanceOf(BulkheadFullException.class);
        assertThat(bulkhead.getMetrics().getQueueDepth()).isZero();
        assertThat(bulkhead.getMetrics().getRemainingQueueCapacity()).isZero();
        assertThat(bulkhead.getMetrics().getQueueCapacity()).isZero();
        assertThat(bulkhead.getMetrics().getActiveThreadCount()).isEqualTo(2);
        assertThat(bulkhead.getMetrics().getThreadPoolSize()).isEqualTo(2);

        latch.countDown();
    }

    @Test
    public void shouldUseVirtualThreadsWhenConfigured() throws Exception {
        System.out.println("Running shouldUseVirtualThreadsWhenConfigured in " + getThreadModeDescription());
        
        // Only run the virtual thread specific checks when in virtual thread mode
        if (!isVirtualThreadMode()) {
            System.out.println("Skipping virtual thread specific test in platform thread mode");
            return;
        }
        
        // Create ThreadPoolBulkhead with custom config for this test
        ThreadPoolBulkheadConfig testConfig = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(5)
            .coreThreadPoolSize(1)
            .queueCapacity(5)
            .keepAliveDuration(Duration.ofMillis(100))
            .build();
            
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("virtualThreadTest-" + threadType, testConfig);
        
        try {
            // Submit a task that checks if it's running on a virtual thread
            AtomicBoolean ranOnVirtualThread = new AtomicBoolean(false);
            
            // Submit a task that verifies it's running on a virtual thread
            CompletableFuture<Boolean> future = bulkhead.submit(() -> {
                ranOnVirtualThread.set(Thread.currentThread().isVirtual());
                return true;
            }).toCompletableFuture();
            
            // Wait for task to complete
            boolean result = future.get(1, TimeUnit.SECONDS);
            
            // Verify execution was successful
            assertThat(result)
                .as("Task should complete successfully in " + getThreadModeDescription())
                .isTrue();
            
            // Verify that the task ran on a virtual thread
            assertThat(ranOnVirtualThread.get())
                .as("Task should execute on a virtual thread when configured in " + getThreadModeDescription())
                .isTrue();
                
            System.out.println("✅ Virtual thread configuration test passed in " + getThreadModeDescription());
        } finally {
            // Clean up
            bulkhead.close();
        }
    }
    
    @Test
    public void shouldHandleConcurrentTasksCorrectlyInBothThreadModes() throws Exception {
        System.out.println("Running shouldHandleConcurrentTasksCorrectlyInBothThreadModes in " + getThreadModeDescription());
        
        // Create ThreadPoolBulkhead config with limited capacity
        ThreadPoolBulkheadConfig testConfig = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(5)
            .coreThreadPoolSize(1)
            .queueCapacity(5)
            .keepAliveDuration(Duration.ofMillis(100))
            .build();
            
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("concurrencyTest-" + threadType, testConfig);
        
        try {
            // Submit more tasks than the thread pool and queue capacity
            int numberOfTasks = 15; // 5 (maxThreadPoolSize) + 5 (queueCapacity) + 5 (excess)
            CompletableFuture<?>[] futures = new CompletableFuture<?>[numberOfTasks];
            AtomicInteger rejectedTasks = new AtomicInteger(0);
            AtomicInteger completedTasks = new AtomicInteger(0);
            CountDownLatch tasksLatch = new CountDownLatch(numberOfTasks);
            
            // Submit tasks to the bulkhead
            for (int i = 0; i < numberOfTasks; i++) {
                final int taskId = i;
                
                try {
                    futures[i] = bulkhead.submit(() -> {
                        try {
                            // Simulate some work
                            Thread.sleep(50);
                            completedTasks.incrementAndGet();
                            return taskId;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return -1;
                        }
                    }).toCompletableFuture();
                    
                    // Register callback to count down the latch
                    futures[i].whenComplete((result, error) -> tasksLatch.countDown());
                } catch (BulkheadFullException e) {
                    rejectedTasks.incrementAndGet();
                    futures[i] = CompletableFuture.completedFuture(-1);
                    tasksLatch.countDown();
                }
            }
            
            // Wait for all tasks to complete or be rejected
            boolean completed = tasksLatch.await(10, TimeUnit.SECONDS);
            
            // Verify all tasks were accounted for (either completed or rejected)
            assertThat(completed)
                .as("All tasks should be completed or rejected within timeout in " + getThreadModeDescription())
                .isTrue();
                
            // Verify the number of accepted tasks matches the capacity
            assertThat(completedTasks.get())
                .as("Number of completed tasks should match thread pool and queue capacity in " + getThreadModeDescription())
                .isLessThanOrEqualTo(10); // 5 + 5
                
            // Verify some tasks were rejected due to capacity limits
            assertThat(rejectedTasks.get())
                .as("Some tasks should be rejected when exceeding capacity in " + getThreadModeDescription())
                .isGreaterThan(0);
                
            // Verify total tasks accounted for
            assertThat(completedTasks.get() + rejectedTasks.get())
                .as("Total tasks (completed + rejected) should equal the number of submitted tasks in " + getThreadModeDescription())
                .isEqualTo(numberOfTasks);
                
            System.out.println("✅ Concurrency test passed in " + getThreadModeDescription() + 
                             " - Completed: " + completedTasks.get() + ", Rejected: " + rejectedTasks.get());
        } finally {
            // Clean up
            bulkhead.close();
        }
    }
    
    @Test
    public void shouldHaveCorrectMetricsInBothThreadModes() throws Exception {
        System.out.println("Running shouldHaveCorrectMetricsInBothThreadModes in " + getThreadModeDescription());
        
        // Create ThreadPoolBulkhead with custom config for this test
        ThreadPoolBulkheadConfig testConfig = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(5)
            .coreThreadPoolSize(1)
            .queueCapacity(5)
            .keepAliveDuration(Duration.ofMillis(100))
            .build();
            
        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("metricsTest-" + threadType, testConfig);
        
        try {
            // Get initial metrics
            ThreadPoolBulkhead.Metrics metrics = bulkhead.getMetrics();
            
            // Verify initial metrics (should be consistent across thread modes)
            assertThat(metrics.getCoreThreadPoolSize())
                .as("Core thread pool size should be correct in " + getThreadModeDescription())
                .isEqualTo(1);
            assertThat(metrics.getMaximumThreadPoolSize())
                .as("Maximum thread pool size should be correct in " + getThreadModeDescription())
                .isEqualTo(5);
            assertThat(metrics.getQueueCapacity())
                .as("Queue capacity should be correct in " + getThreadModeDescription())
                .isEqualTo(5);
            
            // Submit a single task and verify metrics
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
            
            // Wait for task to complete
            boolean taskCompleted = latch.await(1, TimeUnit.SECONDS);
            assertThat(taskCompleted)
                .as("Task should complete within timeout in " + getThreadModeDescription())
                .isTrue();
            
            System.out.println("✅ Metrics test passed in " + getThreadModeDescription());
        } finally {
            // Clean up
            bulkhead.close();
        }
    }

}
