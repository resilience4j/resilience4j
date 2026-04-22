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
package io.github.resilience4j.core;

import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ScopedValuePropagator}.
 *
 * <p>These tests verify that Scoped Values (JEP 506) are properly propagated
 * across thread boundaries using the ScopedValuePropagator.
 */
public class ScopedValuePropagatorTest {

    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    private static final ScopedValue<Integer> USER_ID = ScopedValue.newInstance();

    @Test
    public void shouldCreatePropagatorWithScopedValue() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        assertThat(propagator).isNotNull();
        assertThat(propagator.getScopedValue()).isEqualTo(REQUEST_ID);
    }

    @Test
    public void shouldThrowNullPointerExceptionForNullScopedValue() {
        assertThatThrownBy(() -> ScopedValuePropagator.of(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("ScopedValue cannot be null");
    }

    @Test
    public void shouldRetrieveValueWhenBound() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "test-request-123").run(() -> {
            Optional<String> retrieved = propagator.retrieve().get();
            assertThat(retrieved).isPresent();
            assertThat(retrieved.get()).isEqualTo("test-request-123");
        });
    }

    @Test
    public void shouldReturnEmptyWhenNotBound() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        Optional<String> retrieved = propagator.retrieve().get();
        assertThat(retrieved).isEmpty();
    }

    @Test
    public void shouldCheckIfBound() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        assertThat(propagator.isBound()).isFalse();

        ScopedValue.where(REQUEST_ID, "test").run(() -> {
            assertThat(propagator.isBound()).isTrue();
        });

        assertThat(propagator.isBound()).isFalse();
    }

    @Test
    public void shouldGetCurrentValue() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        assertThat(propagator.getCurrentValue()).isEmpty();

        ScopedValue.where(REQUEST_ID, "current-value").run(() -> {
            assertThat(propagator.getCurrentValue()).isPresent();
            assertThat(propagator.getCurrentValue().get()).isEqualTo("current-value");
        });
    }

    @Test
    public void shouldCopyAndClearValue() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "value-to-copy").run(() -> {
            Optional<String> value = propagator.retrieve().get();

            // Copy the value
            propagator.copy().accept(value);

            // Verify it's captured
            assertThat(propagator.getCapturedValue()).isPresent();
            assertThat(propagator.getCapturedValue().get()).isEqualTo("value-to-copy");

            // Clear the value
            propagator.clear().accept(value);

            // Verify it's cleared
            assertThat(propagator.getCapturedValue()).isEmpty();
        });
    }

    @Test
    public void shouldDecorateSupplierWithScope() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> capturedInThread = new AtomicReference<>();

        Supplier<String> supplier = () -> {
            capturedInThread.set(REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound");
            return REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound";
        };

        ScopedValue.where(REQUEST_ID, "decorated-value").run(() -> {
            Supplier<String> decoratedSupplier = propagator.decorateSupplierWithScope(supplier);

            // Execute in a different thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<String> future = executor.submit(decoratedSupplier::get);
                String result = future.get(5, TimeUnit.SECONDS);
                assertThat(result).isEqualTo("decorated-value");
                assertThat(capturedInThread.get()).isEqualTo("decorated-value");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    @Test
    public void shouldDecorateRunnableWithScope() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> capturedInThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        Runnable runnable = () -> {
            capturedInThread.set(REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound");
            latch.countDown();
        };

        ScopedValue.where(REQUEST_ID, "runnable-value").run(() -> {
            Runnable decoratedRunnable = propagator.decorateRunnableWithScope(runnable);

            // Execute in a different thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(decoratedRunnable);
                boolean completed = latch.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
                assertThat(capturedInThread.get()).isEqualTo("runnable-value");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    @Test
    public void shouldDecorateCallableWithScope() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> capturedInThread = new AtomicReference<>();

        Callable<String> callable = () -> {
            capturedInThread.set(REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound");
            return REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound";
        };

        ScopedValue.where(REQUEST_ID, "callable-value").run(() -> {
            Callable<String> decoratedCallable = propagator.decorateCallableWithScope(callable);

            // Execute in a different thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<String> future = executor.submit(decoratedCallable);
                String result = future.get(5, TimeUnit.SECONDS);
                assertThat(result).isEqualTo("callable-value");
                assertThat(capturedInThread.get()).isEqualTo("callable-value");
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    @Test
    public void shouldHandleUnboundValueInDecoratedSupplier() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        Supplier<String> supplier = () -> "default-result";
        Supplier<String> decorated = propagator.decorateSupplierWithScope(supplier);

        // Should work without any scoped value bound
        String result = decorated.get();
        assertThat(result).isEqualTo("default-result");
    }

    @Test
    public void shouldCreateMultiplePropagators() {
        List<ContextPropagator<?>> propagators = ScopedValuePropagator.ofAll(REQUEST_ID, USER_ID);

        assertThat(propagators).hasSize(2);
        assertThat(propagators.get(0)).isInstanceOf(ScopedValuePropagator.class);
        assertThat(propagators.get(1)).isInstanceOf(ScopedValuePropagator.class);
    }

    @Test
    public void shouldPropagateMultipleScopedValues() {
        ScopedValuePropagator<String> requestPropagator = ScopedValuePropagator.of(REQUEST_ID);
        ScopedValuePropagator<Integer> userPropagator = ScopedValuePropagator.of(USER_ID);

        ScopedValue.where(REQUEST_ID, "multi-request")
            .where(USER_ID, 42)
            .run(() -> {
                assertThat(requestPropagator.getCurrentValue()).hasValue("multi-request");
                assertThat(userPropagator.getCurrentValue()).hasValue(42);
            });
    }

    @Test
    public void shouldWorkWithNestedScopes() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "outer").run(() -> {
            assertThat(propagator.getCurrentValue()).hasValue("outer");

            ScopedValue.where(REQUEST_ID, "inner").run(() -> {
                assertThat(propagator.getCurrentValue()).hasValue("inner");
            });

            assertThat(propagator.getCurrentValue()).hasValue("outer");
        });

        assertThat(propagator.getCurrentValue()).isEmpty();
    }

    @Test
    public void shouldHaveCorrectEqualsAndHashCode() {
        ScopedValuePropagator<String> propagator1 = ScopedValuePropagator.of(REQUEST_ID);
        ScopedValuePropagator<String> propagator2 = ScopedValuePropagator.of(REQUEST_ID);
        ScopedValuePropagator<Integer> propagator3 = ScopedValuePropagator.of(USER_ID);

        assertThat(propagator1).isEqualTo(propagator2);
        assertThat(propagator1.hashCode()).isEqualTo(propagator2.hashCode());
        assertThat(propagator1).isNotEqualTo(propagator3);
    }

    @Test
    public void shouldHaveDescriptiveToString() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        String toString = propagator.toString();
        assertThat(toString).contains("ScopedValuePropagator");
        assertThat(toString).contains("isBound=false");
    }

    @Test
    public void shouldWorkWithVirtualThreads() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> capturedInVirtualThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ScopedValue.where(REQUEST_ID, "virtual-thread-value").run(() -> {
            Runnable decoratedRunnable = propagator.decorateRunnableWithScope(() -> {
                capturedInVirtualThread.set(REQUEST_ID.get());
                latch.countDown();
            });

            // Execute in a virtual thread
            Thread.startVirtualThread(decoratedRunnable);

            try {
                boolean completed = latch.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
                assertThat(capturedInVirtualThread.get()).isEqualTo("virtual-thread-value");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void shouldIntegrateWithContextPropagatorDecorateSupplier() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "context-propagator-test").run(() -> {
            // Use the ContextPropagator.decorateSupplier static method
            Supplier<String> supplier = () -> propagator.getCapturedValue().orElse("not-captured");
            Supplier<String> decorated = ContextPropagator.decorateSupplier(propagator, supplier);

            // The decorated supplier should have captured the value
            String result = decorated.get();
            assertThat(result).isEqualTo("context-propagator-test");
        });
    }

    @Test
    public void shouldIntegrateWithContextPropagatorDecorateRunnable() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ScopedValue.where(REQUEST_ID, "runnable-context-test").run(() -> {
            Runnable runnable = () -> {
                captured.set(propagator.getCapturedValue().orElse("not-captured"));
                latch.countDown();
            };
            Runnable decorated = ContextPropagator.decorateRunnable(propagator, runnable);

            // Execute in another thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(decorated);
                boolean completed = latch.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
                assertThat(captured.get()).isEqualTo("runnable-context-test");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });
    }

    /**
     * Verifies that the value survives a second hop through
     * {@link ContextPropagator#decorateSupplier}. The first decoration captures
     * the value on the source thread; the second decoration is performed on the
     * worker thread (where the ScopedValue itself is not bound) and must still
     * observe the same value via {@link ScopedValuePropagator#retrieve()}.
     */
    @Test
    public void shouldPropagateValueAcrossMultipleAsyncHops() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicReference<Optional<String>> secondHopValue = new AtomicReference<>(Optional.empty());
            AtomicBoolean secondHopRan = new AtomicBoolean(false);

            ScopedValue.where(REQUEST_ID, "req-A").run(() -> {
                Supplier<Void> outer = ContextPropagator.decorateSupplier(
                    List.<ContextPropagator>of(propagator),
                    () -> {
                        // Running on the worker thread; ScopedValue is NOT bound here,
                        // but copy() has populated the capturedValues side-channel.
                        Supplier<Optional<String>> inner = ContextPropagator.decorateSupplier(
                            List.<ContextPropagator>of(propagator),
                            () -> propagator.retrieve().get()
                        );
                        secondHopValue.set(inner.get());
                        secondHopRan.set(true);
                        return null;
                    }
                );
                try {
                    executor.submit(outer::get).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw new RuntimeException(e);
                }
            });

            assertThat(secondHopRan).isTrue();
            assertThat(secondHopValue.get()).contains("req-A");
        } finally {
            executor.shutdown();
        }
    }
}
