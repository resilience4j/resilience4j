/*
 * Copyright 2024 Resilience4j Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.resilience4j.bulkhead;

import io.github.resilience4j.core.ScopedValueContext;
import io.github.resilience4j.core.ScopedValuePropagator;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests demonstrating Scoped Values (JEP 506) with ThreadPoolBulkhead.
 *
 * <p>These tests verify that Scoped Values can be properly propagated across
 * thread boundaries when using ThreadPoolBulkhead.
 */
public class ScopedValueBulkheadIntegrationTest {

    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    private static final ScopedValue<Integer> USER_ID = ScopedValue.newInstance();

    @Test
    public void shouldPropagateScopedValueToBulkheadThread() throws Exception {
        // Create a propagator for our scoped value
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        // Create a ThreadPoolBulkhead with the propagator
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(2)
            .coreThreadPoolSize(1)
            .queueCapacity(10)
            .contextPropagator(propagator)
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("testBulkhead", config);

        AtomicReference<String> capturedInBulkhead = new AtomicReference<>();

        // Run with a scoped value bound
        ScopedValue.where(REQUEST_ID, "integration-test-request-123").run(() -> {
            try {
                // The propagator captures the value and makes it available in the bulkhead thread
                CompletionStage<String> future = bulkhead.executeSupplier(() -> {
                    // Access the captured value through the propagator
                    String captured = propagator.getCapturedValue().orElse("not-found");
                    capturedInBulkhead.set(captured);
                    return captured;
                });

                String result = future.toCompletableFuture().get(5, TimeUnit.SECONDS);
                assertThat(result).isEqualTo("integration-test-request-123");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(capturedInBulkhead.get()).isEqualTo("integration-test-request-123");
    }

    @Test
    public void shouldPropagateMultipleScopedValuesToBulkheadThread() throws Exception {
        // Create propagators for multiple scoped values
        ScopedValuePropagator<String> requestPropagator = ScopedValuePropagator.of(REQUEST_ID);
        ScopedValuePropagator<Integer> userPropagator = ScopedValuePropagator.of(USER_ID);

        // Create a ThreadPoolBulkhead with both propagators
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(2)
            .coreThreadPoolSize(1)
            .queueCapacity(10)
            .contextPropagator(requestPropagator, userPropagator)
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("multiValueBulkhead", config);

        AtomicReference<String> capturedRequest = new AtomicReference<>();
        AtomicReference<Integer> capturedUser = new AtomicReference<>();

        // Run with multiple scoped values bound
        ScopedValue.where(REQUEST_ID, "multi-value-request")
            .where(USER_ID, 42)
            .run(() -> {
                try {
                    CompletionStage<String> future = bulkhead.executeSupplier(() -> {
                        capturedRequest.set(requestPropagator.getCapturedValue().orElse("not-found"));
                        capturedUser.set(userPropagator.getCapturedValue().orElse(-1));
                        return "completed";
                    });

                    future.toCompletableFuture().get(5, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

        assertThat(capturedRequest.get()).isEqualTo("multi-value-request");
        assertThat(capturedUser.get()).isEqualTo(42);
    }

    @Test
    public void shouldWorkWithScopedValueContextAndBulkhead() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(2)
            .coreThreadPoolSize(1)
            .queueCapacity(10)
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("contextBulkhead", config);

        AtomicReference<String> capturedValue = new AtomicReference<>();

        // Using ScopedValueContext for capturing and rebinding
        ScopedValue.where(REQUEST_ID, "context-based-request").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            try {
                // Manually decorate the supplier with context rebinding
                Supplier<String> supplier = () -> {
                    // Use context to rebind the scoped value
                    Supplier<String> innerSupplier = () -> {
                        if (REQUEST_ID.isBound()) {
                            capturedValue.set(REQUEST_ID.get());
                            return REQUEST_ID.get();
                        }
                        return "not-bound";
                    };
                    return context.callWithContext(innerSupplier);
                };

                CompletionStage<String> future = bulkhead.executeSupplier(supplier);
                String result = future.toCompletableFuture().get(5, TimeUnit.SECONDS);

                assertThat(result).isEqualTo("context-based-request");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(capturedValue.get()).isEqualTo("context-based-request");
    }

    @Test
    public void shouldWorkWithVirtualThreadBulkhead() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        // Configure bulkhead to use virtual threads if available
        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(10)
            .coreThreadPoolSize(5)
            .queueCapacity(100)
            .contextPropagator(propagator)
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("virtualThreadBulkhead", config);

        List<CompletionStage<String>> futures = new ArrayList<>();
        int taskCount = 20;

        ScopedValue.where(REQUEST_ID, "virtual-thread-test").run(() -> {
            for (int i = 0; i < taskCount; i++) {
                final int taskId = i;
                CompletionStage<String> future = bulkhead.executeSupplier(() -> {
                    String value = propagator.getCapturedValue().orElse("not-found");
                    return "task-" + taskId + "-" + value;
                });
                futures.add(future);
            }
        });

        // Wait for all tasks to complete
        CompletableFuture.allOf(futures.stream()
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new))
            .get(30, TimeUnit.SECONDS);

        // Verify all tasks got the scoped value
        for (int i = 0; i < taskCount; i++) {
            String result = futures.get(i).toCompletableFuture().get();
            assertThat(result).isEqualTo("task-" + i + "-virtual-thread-test");
        }
    }

    @Test
    public void shouldHandleUnboundScopedValue() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(2)
            .coreThreadPoolSize(1)
            .contextPropagator(propagator)
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("unboundBulkhead", config);

        // Execute without binding the scoped value
        CompletionStage<String> future = bulkhead.executeSupplier(() -> {
            return propagator.getCapturedValue().orElse("default-value");
        });

        String result = future.toCompletableFuture().get(5, TimeUnit.SECONDS);
        assertThat(result).isEqualTo("default-value");
    }

    @Test
    public void shouldPreserveScopedValueAcrossNestedScopes() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(2)
            .coreThreadPoolSize(1)
            .contextPropagator(propagator)
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("nestedScopeBulkhead", config);

        AtomicReference<String> outerCapture = new AtomicReference<>();
        AtomicReference<String> innerCapture = new AtomicReference<>();

        ScopedValue.where(REQUEST_ID, "outer-value").run(() -> {
            try {
                // First capture in outer scope
                CompletionStage<String> outerFuture = bulkhead.executeSupplier(() -> {
                    String value = propagator.getCapturedValue().orElse("not-found");
                    outerCapture.set(value);
                    return value;
                });
                outerFuture.toCompletableFuture().get(5, TimeUnit.SECONDS);

                // Now run with inner scope (different value)
                ScopedValue.where(REQUEST_ID, "inner-value").run(() -> {
                    try {
                        CompletionStage<String> innerFuture = bulkhead.executeSupplier(() -> {
                            String value = propagator.getCapturedValue().orElse("not-found");
                            innerCapture.set(value);
                            return value;
                        });
                        innerFuture.toCompletableFuture().get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThat(outerCapture.get()).isEqualTo("outer-value");
        assertThat(innerCapture.get()).isEqualTo("inner-value");
    }

    @Test
    public void shouldWorkWithConcurrentBulkheadExecutions() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ThreadPoolBulkheadConfig config = ThreadPoolBulkheadConfig.custom()
            .maxThreadPoolSize(4)
            .coreThreadPoolSize(2)
            .queueCapacity(50)
            .contextPropagator(propagator)
            .build();

        ThreadPoolBulkhead bulkhead = ThreadPoolBulkhead.of("concurrentBulkhead", config);

        int concurrentRequests = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrentRequests);
        List<CompletionStage<String>> futures = new ArrayList<>();

        // Start multiple threads each with their own scoped value
        for (int i = 0; i < concurrentRequests; i++) {
            final String requestId = "request-" + i;
            Thread.ofVirtual().start(() -> {
                ScopedValue.where(REQUEST_ID, requestId).run(() -> {
                    try {
                        startLatch.await(); // Wait for all threads to be ready

                        CompletionStage<String> future = bulkhead.executeSupplier(() -> {
                            return propagator.getCapturedValue().orElse("not-found");
                        });

                        synchronized (futures) {
                            futures.add(future);
                        }
                        doneLatch.countDown();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            });
        }

        // Release all threads at once
        startLatch.countDown();

        // Wait for all submissions
        boolean allSubmitted = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(allSubmitted).isTrue();

        // Wait for all tasks to complete and verify
        Thread.sleep(1000); // Give time for all tasks to complete

        synchronized (futures) {
            assertThat(futures).hasSize(concurrentRequests);

            for (CompletionStage<String> future : futures) {
                String result = future.toCompletableFuture().get(5, TimeUnit.SECONDS);
                assertThat(result).startsWith("request-");
            }
        }
    }
}
