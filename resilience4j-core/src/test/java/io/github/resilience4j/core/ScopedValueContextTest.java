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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ScopedValueContext}.
 */
public class ScopedValueContextTest {

    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();
    private static final ScopedValue<Integer> USER_ID = ScopedValue.newInstance();
    private static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    @Test
    public void shouldCaptureEmptyContext() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValueContext context = ScopedValueContext.capture(propagator);

        assertThat(context.hasValues()).isFalse();
        assertThat(context.size()).isZero();
    }

    @Test
    public void shouldCaptureSingleScopedValue() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "test-request").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            assertThat(context.hasValues()).isTrue();
            assertThat(context.size()).isEqualTo(1);
        });
    }

    @Test
    public void shouldCaptureMultipleScopedValues() {
        ScopedValuePropagator<String> requestPropagator = ScopedValuePropagator.of(REQUEST_ID);
        ScopedValuePropagator<Integer> userPropagator = ScopedValuePropagator.of(USER_ID);

        ScopedValue.where(REQUEST_ID, "test-request")
            .where(USER_ID, 123)
            .run(() -> {
                ScopedValueContext context = ScopedValueContext.capture(
                    Arrays.asList(requestPropagator, userPropagator)
                );

                assertThat(context.hasValues()).isTrue();
                assertThat(context.size()).isEqualTo(2);
            });
    }

    @Test
    public void shouldCreateEmptyContext() {
        ScopedValueContext context = ScopedValueContext.empty();

        assertThat(context.hasValues()).isFalse();
        assertThat(context.size()).isZero();
    }

    @Test
    public void shouldThrowForNullPropagators() {
        assertThatThrownBy(() -> ScopedValueContext.capture((List<ScopedValuePropagator<?>>) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("Propagators list cannot be null");
    }

    @Test
    public void shouldRunWithContext() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> captured = new AtomicReference<>();

        ScopedValue.where(REQUEST_ID, "context-value").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            context.runWithContext(() -> {
                captured.set(REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound");
            });
        });

        assertThat(captured.get()).isEqualTo("context-value");
    }

    @Test
    public void shouldRunWithMultipleScopedValuesInContext() {
        ScopedValuePropagator<String> requestPropagator = ScopedValuePropagator.of(REQUEST_ID);
        ScopedValuePropagator<Integer> userPropagator = ScopedValuePropagator.of(USER_ID);
        ScopedValuePropagator<String> tracePropagator = ScopedValuePropagator.of(TRACE_ID);

        AtomicReference<String> capturedRequest = new AtomicReference<>();
        AtomicReference<Integer> capturedUser = new AtomicReference<>();
        AtomicReference<String> capturedTrace = new AtomicReference<>();

        ScopedValue.where(REQUEST_ID, "multi-request")
            .where(USER_ID, 456)
            .where(TRACE_ID, "trace-123")
            .run(() -> {
                ScopedValueContext context = ScopedValueContext.capture(
                    requestPropagator, userPropagator, tracePropagator
                );

                context.runWithContext(() -> {
                    capturedRequest.set(REQUEST_ID.get());
                    capturedUser.set(USER_ID.get());
                    capturedTrace.set(TRACE_ID.get());
                });
            });

        assertThat(capturedRequest.get()).isEqualTo("multi-request");
        assertThat(capturedUser.get()).isEqualTo(456);
        assertThat(capturedTrace.get()).isEqualTo("trace-123");
    }

    @Test
    public void shouldCallWithContext() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        String result = ScopedValue.where(REQUEST_ID, "call-value").call(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            return context.callWithContext((Supplier<String>) () ->
                REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound"
            );
        });

        assertThat(result).isEqualTo("call-value");
    }

    @Test
    public void shouldCallWithContextCallable() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        String result = ScopedValue.where(REQUEST_ID, "callable-value").call(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            return context.callWithContext((Callable<String>) () ->
                REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound"
            );
        });

        assertThat(result).isEqualTo("callable-value");
    }

    @Test
    public void shouldDecorateRunnable() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ScopedValue.where(REQUEST_ID, "decorated-runnable").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            Runnable decorated = context.decorateRunnable(() -> {
                captured.set(REQUEST_ID.get());
                latch.countDown();
            });

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                executor.submit(decorated);
                boolean completed = latch.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                executor.shutdown();
            }
        });

        assertThat(captured.get()).isEqualTo("decorated-runnable");
    }

    @Test
    public void shouldDecorateSupplier() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        String result = ScopedValue.where(REQUEST_ID, "decorated-supplier").call(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            Supplier<String> decorated = context.decorateSupplier(() ->
                REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound"
            );

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<String> future = executor.submit(decorated::get);
                return future.get(5, TimeUnit.SECONDS);
            } finally {
                executor.shutdown();
            }
        });

        assertThat(result).isEqualTo("decorated-supplier");
    }

    @Test
    public void shouldDecorateCallable() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        String result = ScopedValue.where(REQUEST_ID, "decorated-callable").call(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            Callable<String> decorated = context.decorateCallable(() ->
                REQUEST_ID.isBound() ? REQUEST_ID.get() : "not-bound"
            );

            ExecutorService executor = Executors.newSingleThreadExecutor();
            try {
                Future<String> future = executor.submit(decorated);
                return future.get(5, TimeUnit.SECONDS);
            } finally {
                executor.shutdown();
            }
        });

        assertThat(result).isEqualTo("decorated-callable");
    }

    @Test
    public void shouldRunEmptyContextWithoutBinding() {
        ScopedValueContext context = ScopedValueContext.empty();
        AtomicReference<Boolean> isBound = new AtomicReference<>();

        context.runWithContext(() -> {
            isBound.set(REQUEST_ID.isBound());
        });

        assertThat(isBound.get()).isFalse();
    }

    @Test
    public void shouldReturnOriginalRunnableForEmptyContext() {
        ScopedValueContext context = ScopedValueContext.empty();
        Runnable original = () -> {};

        Runnable decorated = context.decorateRunnable(original);

        assertThat(decorated).isSameAs(original);
    }

    @Test
    public void shouldReturnOriginalSupplierForEmptyContext() {
        ScopedValueContext context = ScopedValueContext.empty();
        Supplier<String> original = () -> "test";

        Supplier<String> decorated = context.decorateSupplier(original);

        assertThat(decorated).isSameAs(original);
    }

    @Test
    public void shouldReturnOriginalCallableForEmptyContext() {
        ScopedValueContext context = ScopedValueContext.empty();
        Callable<String> original = () -> "test";

        Callable<String> decorated = context.decorateCallable(original);

        assertThat(decorated).isSameAs(original);
    }

    @Test
    public void shouldUseStaticRunWithScopedValues() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> captured = new AtomicReference<>();

        ScopedValue.where(REQUEST_ID, "static-method-test").run(() -> {
            ScopedValueContext.runWithScopedValues(
                List.of(propagator),
                () -> captured.set(REQUEST_ID.get())
            );
        });

        assertThat(captured.get()).isEqualTo("static-method-test");
    }

    @Test
    public void shouldUseStaticCallWithScopedValues() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "static-call-test").run(() -> {
            String result = ScopedValueContext.callWithScopedValues(
                List.of(propagator),
                () -> REQUEST_ID.get()
            );
            assertThat(result).isEqualTo("static-call-test");
        });
    }

    @Test
    public void shouldWorkWithVirtualThreads() throws Exception {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);
        AtomicReference<String> captured = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        ScopedValue.where(REQUEST_ID, "virtual-thread-context").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            Thread.startVirtualThread(() -> {
                context.runWithContext(() -> {
                    captured.set(REQUEST_ID.get());
                    latch.countDown();
                });
            });

            try {
                boolean completed = latch.await(5, TimeUnit.SECONDS);
                assertThat(completed).isTrue();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        assertThat(captured.get()).isEqualTo("virtual-thread-context");
    }

    @Test
    public void shouldHaveDescriptiveToString() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "test").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);
            String toString = context.toString();

            assertThat(toString).contains("ScopedValueContext");
            assertThat(toString).contains("1 entries");
        });
    }

    @Test
    public void shouldPropagateExceptionFromCallable() {
        ScopedValuePropagator<String> propagator = ScopedValuePropagator.of(REQUEST_ID);

        ScopedValue.where(REQUEST_ID, "exception-test").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(propagator);

            assertThatThrownBy(() -> context.callWithContext((Callable<String>) () -> {
                throw new IllegalStateException("Test exception");
            }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Test exception");
        });
    }

    @Test
    public void shouldOnlyCapturePartiallyBoundValues() {
        ScopedValuePropagator<String> requestPropagator = ScopedValuePropagator.of(REQUEST_ID);
        ScopedValuePropagator<Integer> userPropagator = ScopedValuePropagator.of(USER_ID);

        // Only bind REQUEST_ID, not USER_ID
        ScopedValue.where(REQUEST_ID, "partial-test").run(() -> {
            ScopedValueContext context = ScopedValueContext.capture(
                Arrays.asList(requestPropagator, userPropagator)
            );

            // Should only have 1 captured value
            assertThat(context.size()).isEqualTo(1);

            context.runWithContext(() -> {
                assertThat(REQUEST_ID.isBound()).isTrue();
                assertThat(REQUEST_ID.get()).isEqualTo("partial-test");
                assertThat(USER_ID.isBound()).isFalse();
            });
        });
    }
}
