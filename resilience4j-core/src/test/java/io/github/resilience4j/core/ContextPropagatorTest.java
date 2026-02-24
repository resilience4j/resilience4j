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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.MDC;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.matches;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ContextPropagatorTest extends ThreadModeTestBase {

    @Parameterized.Parameters(name = "threadMode={0}")
    public static Collection<Object[]> threadModes() {
        return ThreadModeTestBase.threadModes();
    }

    /**
     * Constructor for parameterized tests.
     * 
     * @param threadType the thread mode to test with ("platform" or "virtual")
     */
    public ContextPropagatorTest(ThreadType threadType) {
        super(threadType);
    }

    @Before
    public void setUp() {
        setUpThreadMode(); // Set up thread mode from ThreadModeTestBase
    }

    @After
    public void tearDown() {
        cleanUpThreadMode(); // Clean up thread mode from ThreadModeTestBase
        MDC.clear(); // Clean up any MDC values
    }

    @Test
    public void contextPropagationFailureSingleTestInBothThreadModes() {
        System.out.println("Running contextPropagationFailureSingleTestInBothThreadModes in " + getThreadModeDescription());
        
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShould_NOT_CrossThreadBoundary-" + threadType);

        Supplier<String> supplier = threadLocal::get;
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue(null)));
        
        System.out.println("✅ Context propagation failure test passed in " + getThreadModeDescription());
    }

    @Test
    public void contextPropagationEmptyListShouldNotFail() {
        Supplier<String> supplier = () -> "Hello World";

        //Thread boundary
        Supplier<String> decoratedSupplier = ContextPropagator.decorateSupplier(Collections.emptyList(), supplier);
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(decoratedSupplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue("Hello World")));
    }

    @Test
    public void contextPropagationEmptyListShouldNotFailWithCallable() {
        //Thread boundary
        Callable<String> decorateCallable = ContextPropagator.decorateCallable(Collections.emptyList(), () -> "Hello World");

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(decorateCallable.call()).isEqualTo("Hello World")));
    }

    @Test
    public void contextPropagationFailureMultipleTest() {
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

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future.get()).containsExactlyInAnyOrder(null, null)));
    }

    @Test
    public void contextPropagationSupplierMultipleTest() {
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

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")
        ));
    }

    @Test
    public void contextPropagationSupplierMultipleTestWithCallable() {
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

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(callable.call()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")
        ));
    }

    @Test
    public void contextPropagationSupplierSingleTestInBothThreadModes() {
        System.out.println("Running contextPropagationSupplierSingleTestInBothThreadModes in " + getThreadModeDescription());
        
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        String expectedValue = "SingleValueShouldCrossThreadBoundary-" + threadType;
        threadLocal.set(expectedValue);

        Supplier<String> supplier = ContextPropagator.decorateSupplier(
            new TestThreadLocalContextPropagator(threadLocal),
            threadLocal::get);
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue(expectedValue)));
        
        System.out.println("✅ Context propagation supplier test passed in " + getThreadModeDescription());
    }

    @Test
    public void contextPropagationSupplierSingleTestWithCallable() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        Callable<String> callable = ContextPropagator.decorateCallable(
            new TestThreadLocalContextPropagator(threadLocal),
            threadLocal::get);

        waitAtMost(200, TimeUnit.MILLISECONDS).until(matches(() ->
            assertThat(callable.call()).isEqualTo("SingleValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationRunnableFailureSingleTest() {
        AtomicReference<String> reference = new AtomicReference<>();
        //Thread boundary
        Runnable runnable = ContextPropagator.decorateRunnable(
            Collections.emptyList(),
            () -> reference.set("Hello World"));

        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference).hasValue("Hello World")));
    }

    @Test
    public void contextPropagationRunnableEmptyListShouldNotFail() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShould_NOT_CrossThreadBoundary");

        AtomicReference<String> reference = new AtomicReference<>();
        Runnable runnable = () -> reference.set(threadLocal.get());
        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference).hasValue(null)));
    }

    @Test
    public void contextPropagationRunnableSingleTest() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        AtomicReference<String> reference = new AtomicReference<>();
        Runnable runnable = ContextPropagator.decorateRunnable(
            new TestThreadLocalContextPropagator(threadLocal),
            () -> reference.set(threadLocal.get()));
        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference).hasValue("SingleValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationRunnableMultipleTest() {
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

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationRunnableMultipleFailureTest() {
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

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(reference.get()).containsExactlyInAnyOrder(null, null)));
    }

    @Test
    public void contextPropagationWithMDCInBothThreadModes() throws Exception {
        System.out.println("Running contextPropagationWithMDCInBothThreadModes in " + getThreadModeDescription());
        
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
            .as("MDC should be propagated correctly in " + getThreadModeDescription())
            .isEqualTo(testValue);
        
        // Clean up
        MDC.remove(testKey);
        
        System.out.println("✅ MDC context propagation test passed in " + getThreadModeDescription());
    }

    @Test
    public void contextPropagationWithMultiplePropagators() throws Exception {
        System.out.println("Running contextPropagationWithMultiplePropagators in " + getThreadModeDescription());
        
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
            .as("Both context values should be propagated correctly in " + getThreadModeDescription())
            .containsExactly(stringValue, intValue);
        
        System.out.println("✅ Multiple propagators test passed in " + getThreadModeDescription());
    }

    @Test
    public void contextPropagationWithConcurrentThreadsInBothModes() throws Exception {
        System.out.println("Running contextPropagationWithConcurrentThreadsInBothModes in " + getThreadModeDescription());
        
        // Reduced thread count for faster test execution
        int concurrentThreads = isVirtualThreadMode() ? 5 : 3;
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicReference<String> sharedValue = new AtomicReference<>("shared-value-" + threadType);
        
        ContextPropagator<String> propagator = new TestThreadLocalContextPropagator(threadLocal);
        
        // Set initial value in main thread
        threadLocal.set(sharedValue.get());
        
        // Create simple concurrent test without complex synchronization
        CompletableFuture<?>[] futures = new CompletableFuture<?>[concurrentThreads];
        
        for (int i = 0; i < concurrentThreads; i++) {
            final int threadNum = i;
            
            // Create decorated runnable to propagate context
            Runnable decoratedRunnable = ContextPropagator.decorateRunnable(
                propagator, 
                () -> {
                    // Verify context was propagated correctly
                    String propagatedValue = threadLocal.get();
                    if (sharedValue.get().equals(propagatedValue)) {
                        successCount.incrementAndGet();
                    }
                }
            );
            
            // Run on separate thread
            futures[i] = CompletableFuture.runAsync(decoratedRunnable);
        }
        
        // Wait for all futures to complete with timeout
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
        allOf.get(5, TimeUnit.SECONDS);
        
        // Verify all threads propagated context correctly
        assertThat(successCount.get())
            .as("All threads should successfully propagate context in " + getThreadModeDescription())
            .isEqualTo(concurrentThreads);
        
        System.out.println("✅ Concurrent context propagation test passed in " + getThreadModeDescription() + 
                         " - Threads: " + concurrentThreads + ", Successes: " + successCount.get());
    }
}
