package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.TestContextPropagators.TestThreadLocalContextPropagator;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.matches;
import static com.jayway.awaitility.Awaitility.waitAtMost;
import static org.assertj.core.api.Assertions.assertThat;

public class ContextPropagatorTest {

    @Test
    public void contextPropagationFailureSingleTest() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShould_NOT_CrossThreadBoundary");

        Supplier<String> supplier = threadLocal::get;
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue(null)));
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
    public void contextPropagationSupplierSingleTest() {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("SingleValueShouldCrossThreadBoundary");

        Supplier<String> supplier = ContextPropagator.decorateSupplier(
            new TestThreadLocalContextPropagator(threadLocal),
            threadLocal::get);
        //Thread boundary
        final CompletableFuture<String> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(() ->
            assertThat(future).isCompletedWithValue("SingleValueShouldCrossThreadBoundary")));
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
}
