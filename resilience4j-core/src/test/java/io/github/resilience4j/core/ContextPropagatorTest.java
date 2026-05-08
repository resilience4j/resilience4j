/*
 *
 *  Copyright 2020 krnsaurabh
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
package io.github.resilience4j.core;

import io.github.resilience4j.core.TestContextPropagators.TestThreadLocalContextPropagator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class ContextPropagatorTest extends ThreadModeTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(ContextPropagatorTest.class);

    @AfterEach
    void tearDown() {
        MDC.clear(); // Clean up any MDC values
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationFailureSingleTestInBothThreadModes(ThreadType threadType) {
        setUpThreadMode(threadType);
        LOG.info("Running contextPropagationFailureSingleTestInBothThreadModes in {}", getThreadModeDescription(threadType));

        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShould_NOT_CrossThreadBoundary-" + threadType);

        Supplier<String> supplier = threadLocal::get;
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(future).isCompletedWithValue(null));

        LOG.info("Context propagation failure test passed in {}", getThreadModeDescription(threadType));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationEmptyListShouldNotFail(ThreadType threadType) {
        setUpThreadMode(threadType);
        Supplier<String> supplier = () -> "Hello World";

        //Thread boundary
        Supplier<String> decoratedSupplier = ContextPropagator.decorateSupplier(Collections.emptyList(), supplier);
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(future).isCompletedWithValue("Hello World"));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationEmptyListShouldNotFailWithCallable(ThreadType threadType) {
        setUpThreadMode(threadType);
        //Thread boundary
        Callable<String> decorateCallable = ContextPropagator.decorateCallable(Collections.emptyList(), () -> "Hello World");

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(decorateCallable.call()).isEqualTo("Hello World"));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationFailureMultipleTest(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShould_NOT_CrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShould_NOT_CrossThreadBoundary");

        Supplier<List<String>> supplier = () -> Arrays.asList(
            threadLocalOne.get(),
            threadLocalTwo.get()
        );
        //Thread boundary
        final CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(supplier);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(future.get()).containsExactlyInAnyOrder(null, null));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationSupplierMultipleTest(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);

        Supplier<List<String>> supplier = ContextPropagator.decorateSupplier(
            Arrays.asList(propagatorOne, propagatorTwo),
            () -> Arrays.asList(threadLocalOne.get(), threadLocalTwo.get()));
        //Thread boundary
        final CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(supplier);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(future.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")
        );
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationSupplierMultipleTestWithCallable(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);

        Callable<List<String>> callable = ContextPropagator.decorateCallable(
            Arrays.asList(propagatorOne, propagatorTwo),
            () -> Arrays.asList(threadLocalOne.get(), threadLocalTwo.get()));
        //Thread boundary

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(callable.call()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")
        );
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationSupplierSingleTestInBothThreadModes(ThreadType threadType) {
        setUpThreadMode(threadType);
        LOG.info("Running contextPropagationSupplierSingleTestInBothThreadModes in {}", getThreadModeDescription(threadType));

        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        String expectedValue = "SingleValueShouldCrossThreadBoundary-" + threadType;
        threadLocal.set(expectedValue);

        Supplier<String> supplier = ContextPropagator.decorateSupplier(
            new TestThreadLocalContextPropagator(threadLocal),
            threadLocal::get);
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(future).isCompletedWithValue(expectedValue));

        LOG.info("Context propagation supplier test passed in {}", getThreadModeDescription(threadType));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationSupplierSingleTestWithCallable(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        Callable<String> callable = ContextPropagator.decorateCallable(
            new TestThreadLocalContextPropagator(threadLocal),
            threadLocal::get);

        await().atMost(200, TimeUnit.MILLISECONDS).untilAsserted(() ->
            assertThat(callable.call()).isEqualTo("SingleValueShouldCrossThreadBoundary"));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationRunnableFailureSingleTest(ThreadType threadType) {
        setUpThreadMode(threadType);
        AtomicReference<String> reference = new AtomicReference<>();
        //Thread boundary
        Runnable runnable = ContextPropagator.decorateRunnable(
            Collections.emptyList(),
            () -> reference.set("Hello World"));

        CompletableFuture.runAsync(runnable);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(reference).hasValue("Hello World"));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationRunnableEmptyListShouldNotFail(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShould_NOT_CrossThreadBoundary");

        AtomicReference<String> reference = new AtomicReference<>();
        Runnable runnable = () -> reference.set(threadLocal.get());
        //Thread boundary
        CompletableFuture.runAsync(runnable);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(reference).hasValue(null));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationRunnableSingleTest(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        AtomicReference<String> reference = new AtomicReference<>();
        Runnable runnable = ContextPropagator.decorateRunnable(
            new TestThreadLocalContextPropagator(threadLocal),
            () -> reference.set(threadLocal.get()));
        //Thread boundary
        CompletableFuture.runAsync(runnable);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(reference).hasValue("SingleValueShouldCrossThreadBoundary"));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationRunnableMultipleTest(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);

        AtomicReference<List<String>> reference = new AtomicReference<>();
        Runnable runnable = ContextPropagator.decorateRunnable(
            Arrays.asList(propagatorOne, propagatorTwo),
            () -> reference.set(Arrays.asList(
                threadLocalOne.get(),
                threadLocalTwo.get()
            )));

        //Thread boundary
        CompletableFuture.runAsync(runnable);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(reference.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary"));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationRunnableMultipleFailureTest(ThreadType threadType) {
        setUpThreadMode(threadType);
        ThreadLocal<String> threadLocalOne = new ThreadLocal<>();
        threadLocalOne.set("FirstValueShouldCross_NOT_ThreadBoundary");

        ThreadLocal<String> threadLocalTwo = new ThreadLocal<>();
        threadLocalTwo.set("SecondValueShould_NOT_CrossThreadBoundary");

        AtomicReference<List<String>> reference = new AtomicReference<>();
        Runnable runnable = () -> reference.set(Arrays.asList(
            threadLocalOne.get(),
            threadLocalTwo.get()
        ));

        //Thread boundary
        CompletableFuture.runAsync(runnable);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
            assertThat(reference.get()).containsExactlyInAnyOrder(null, null));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationWithMDCInBothThreadModes(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
        LOG.info("Running contextPropagationWithMDCInBothThreadModes in {}", getThreadModeDescription(threadType));

        // Test MDC (Mapped Diagnostic Context) propagation
        String testKey = "test-key-" + threadType;
        String testValue = "test-value-" + threadType;
        AtomicReference<String> mdcValueInChildThread = new AtomicReference<>();

        // Create MDC context propagator
        ContextPropagator<String> mdcPropagator = new ContextPropagator<String>() {
            @Override
            public Supplier<Optional<String>> retrieve() {
                return () -> Optional.ofNullable(MDC.get(testKey));
            }

            @Override
            public Consumer<Optional<String>> copy() {
                return optional -> optional.ifPresent(value -> MDC.put(testKey, value));
            }

            @Override
            public Consumer<Optional<String>> clear() {
                return optional -> MDC.remove(testKey);
            }
        };

        // Set MDC value in main thread
        MDC.put(testKey, testValue);

        // Create decorated runnable
        Runnable decoratedRunnable = ContextPropagator.decorateRunnable(
            mdcPropagator,
            () -> {
                // Store MDC value from child thread
                mdcValueInChildThread.set(MDC.get(testKey));
            }
        );

        // Run on appropriate thread type based on mode
        CompletableFuture<Void> future = CompletableFuture.runAsync(decoratedRunnable);
        future.get(5, TimeUnit.SECONDS);

        // Verify MDC was propagated correctly
        assertThat(mdcValueInChildThread.get())
            .as("MDC should be propagated correctly in " + getThreadModeDescription(threadType))
            .isEqualTo(testValue);

        // Clean up
        MDC.remove(testKey);

        LOG.info("MDC context propagation test passed in {}", getThreadModeDescription(threadType));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationWithMultiplePropagators(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
        LOG.info("Running contextPropagationWithMultiplePropagators in {}", getThreadModeDescription(threadType));

        // Create multiple ThreadLocals and propagators
        ThreadLocal<String> stringThreadLocal = new ThreadLocal<>();
        ThreadLocal<Integer> intThreadLocal = new ThreadLocal<>();

        String stringValue = "string-value-" + threadType;
        int intValue = 42 + threadType.hashCode(); // Different value per mode

        stringThreadLocal.set(stringValue);
        intThreadLocal.set(intValue);

        ContextPropagator<String> stringPropagator = new ContextPropagator<String>() {
            @Override
            public Supplier<Optional<String>> retrieve() {
                return () -> Optional.ofNullable(stringThreadLocal.get());
            }

            @Override
            public Consumer<Optional<String>> copy() {
                return optional -> optional.ifPresent(stringThreadLocal::set);
            }

            @Override
            public Consumer<Optional<String>> clear() {
                return optional -> stringThreadLocal.remove();
            }
        };

        ContextPropagator<Integer> intPropagator = new ContextPropagator<Integer>() {
            @Override
            public Supplier<Optional<Integer>> retrieve() {
                return () -> Optional.ofNullable(intThreadLocal.get());
            }

            @Override
            public Consumer<Optional<Integer>> copy() {
                return optional -> optional.ifPresent(intThreadLocal::set);
            }

            @Override
            public Consumer<Optional<Integer>> clear() {
                return optional -> intThreadLocal.remove();
            }
        };

        // Create list of propagators
        List<ContextPropagator<?>> propagators = Arrays.asList(stringPropagator, intPropagator);

        // Create a callable that verifies both values were propagated
        Callable<List<Object>> decoratedCallable = ContextPropagator.decorateCallable(
            propagators,
            () -> Arrays.asList(stringThreadLocal.get(), intThreadLocal.get())
        );

        // Execute on appropriate thread type
        CompletableFuture<List<Object>> future = CompletableFuture.supplyAsync(
            () -> {
                try {
                    return decoratedCallable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        );

        // Verify results
        List<Object> result = future.get(5, TimeUnit.SECONDS);
        assertThat(result)
            .as("Both context values should be propagated correctly in " + getThreadModeDescription(threadType))
            .containsExactly(stringValue, intValue);

        LOG.info("Multiple propagators test passed in {}", getThreadModeDescription(threadType));
    }

    @ParameterizedTest(name = "{0} thread mode")
    @EnumSource(ThreadType.class)
    void contextPropagationWithConcurrentThreadsInBothModes(ThreadType threadType) throws Exception {
        setUpThreadMode(threadType);
        LOG.info("Running contextPropagationWithConcurrentThreadsInBothModes in {}", getThreadModeDescription(threadType));

        // Reduced thread count for faster test execution
        int concurrentThreads = isVirtualThreadMode(threadType) ? 5 : 3;
        ThreadLocal<String> threadLocal = new ThreadLocal<>();

        AtomicInteger successCount = new AtomicInteger(0);
        String sharedValue = "shared-value-" + threadType;

        ContextPropagator<String> propagator = new TestThreadLocalContextPropagator(threadLocal);

        // Set initial value in main thread
        threadLocal.set(sharedValue);

        // Create simple concurrent test without complex synchronization
        CompletableFuture<?>[] futures = new CompletableFuture<?>[concurrentThreads];

        for (int i = 0; i < concurrentThreads; i++) {
            // Create decorated runnable to propagate context
            Runnable decoratedRunnable = ContextPropagator.decorateRunnable(
                propagator,
                () -> {
                    // Verify context was propagated correctly
                    String propagatedValue = threadLocal.get();
                    if (sharedValue.equals(propagatedValue)) {
                        successCount.incrementAndGet();
                    }
                }
            );

            // Run on separate thread
            futures[i] = CompletableFuture.runAsync(decoratedRunnable);
        }

        // Wait for all futures to complete with timeout
        CompletableFuture.allOf(futures).get(5, TimeUnit.SECONDS);

        // Verify all threads propagated context correctly
        assertThat(successCount.get())
            .as("All threads should successfully propagate context in " + getThreadModeDescription(threadType))
            .isEqualTo(concurrentThreads);

        LOG.info("Concurrent context propagation test passed in {} - Threads: {}, Successes: {}",
            getThreadModeDescription(threadType), concurrentThreads, successCount.get());
    }
}
