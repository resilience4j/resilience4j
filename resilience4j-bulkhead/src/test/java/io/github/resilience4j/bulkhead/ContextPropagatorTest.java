package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.TestContextPropagators.TestThreadLocalContextPropagator;
import org.assertj.core.api.Assertions;
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

        ThreadLocal threadlocal = new ThreadLocal();
        threadlocal.set("SingleValueShould_NOT_CrossThreadBoundary");

        Supplier<Object> supplier = () -> threadlocal.get();

        //Thread boundary
        final CompletableFuture<Object> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> null == future.get());
    }

    @Test
    public void contextPropagationEmptyListShouldNotFail() {

        Supplier<Object> supplier = () -> "Hello World";

        //Thread boundary
        Supplier<Object> decoratedSupplier = ContextPropagator.decorateSupplier(Collections.emptyList(), supplier);
        final CompletableFuture<Object> future = CompletableFuture.supplyAsync(decoratedSupplier);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> "Hello World" == future.get());
    }

    @Test
    public void contextPropagationFailureMultipleTest() throws Exception {

        ThreadLocal threadLocalOne = new ThreadLocal();
        threadLocalOne.set("FirstValueShould_NOT_CrossThreadBoundary");

        ThreadLocal threadLocalTwo = new ThreadLocal();
        threadLocalTwo.set("SecondValueShould_NOT_CrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);

        Supplier<List<Object>> supplier = () -> Arrays.asList(
            threadLocalOne.get(),
            threadLocalTwo.get()
        );

        //Thread boundary
        final CompletableFuture<List<Object>> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS).until(matches(
            () -> assertThat(future.get()).containsExactlyInAnyOrder(null, null)
        ));
    }

    @Test
    public void contextPropagationSupplierMultipleTest() {

        ThreadLocal threadLocalOne = new ThreadLocal();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal threadLocalTwo = new ThreadLocal();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);


        Supplier<List<Object>> supplier = ContextPropagator.decorateSupplier(Arrays.asList(propagatorOne, propagatorTwo), () -> Arrays.asList(
            threadLocalOne.get(),
            threadLocalTwo.get()
        ));

        //Thread boundary
        final CompletableFuture<List<Object>> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(matches(() -> Assertions.assertThat(future.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")
            ));
    }

    @Test
    public void contextPropagationSupplierSingleTest() {

        ThreadLocal threadlocal = new ThreadLocal();
        threadlocal.set("SingleValueShouldCrossThreadBoundary");

        Supplier<Object> supplier = ContextPropagator
            .decorateSupplier(new TestThreadLocalContextPropagator(threadlocal),
                () -> threadlocal.get());

        //Thread boundary
        final CompletableFuture<Object> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> "SingleValueShouldCrossThreadBoundary" == future.get());
    }

    @Test
    public void contextPropagationRunnableFailureSingleTest() {

        AtomicReference reference = new AtomicReference();

        //Thread boundary
        Runnable runnable = ContextPropagator
            .decorateRunnable(Collections.emptyList(),
                () -> reference.set("Hello World"));

        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> "Hello World" == reference.get());
    }

    @Test
    public void contextPropagationRunnableEmptyListShouldNotFail() {

        ThreadLocal threadlocal = new ThreadLocal();
        threadlocal.set("SingleValueShould_NOT_CrossThreadBoundary");

        AtomicReference reference = new AtomicReference();

        Runnable runnable = () -> reference.set(threadlocal.get());

        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> null == reference.get());
    }

    @Test
    public void contextPropagationRunnableSingleTest() {

        ThreadLocal threadlocal = new ThreadLocal();
        threadlocal.set("SingleValueShouldCrossThreadBoundary");

        AtomicReference reference = new AtomicReference();

        Runnable runnable = ContextPropagator
            .decorateRunnable(new TestThreadLocalContextPropagator(threadlocal),
                () -> reference.set(threadlocal.get()));

        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> "SingleValueShouldCrossThreadBoundary" == reference.get());
    }

    @Test
    public void contextPropagationRunnableMultipleTest() {

        ThreadLocal threadLocalOne = new ThreadLocal();
        threadLocalOne.set("FirstValueShouldCrossThreadBoundary");

        ThreadLocal threadLocalTwo = new ThreadLocal();
        threadLocalTwo.set("SecondValueShouldCrossThreadBoundary");

        TestThreadLocalContextPropagator propagatorOne = new TestThreadLocalContextPropagator(threadLocalOne);
        TestThreadLocalContextPropagator propagatorTwo = new TestThreadLocalContextPropagator(threadLocalTwo);


        AtomicReference<List> reference = new AtomicReference();

        Runnable runnable = ContextPropagator
            .decorateRunnable(Arrays.asList(propagatorOne, propagatorTwo),
                () -> reference.set(Arrays.asList(
                    threadLocalOne.get(),
                    threadLocalTwo.get()
                )));


        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(matches(() -> assertThat(reference.get()).containsExactlyInAnyOrder(
                "FirstValueShouldCrossThreadBoundary",
                "SecondValueShouldCrossThreadBoundary")));
    }

    @Test
    public void contextPropagationRunnableMultipleFailureTest() {

        ThreadLocal threadLocalOne = new ThreadLocal();
        threadLocalOne.set("FirstValueShouldCross_NOT_ThreadBoundary");

        ThreadLocal threadLocalTwo = new ThreadLocal();
        threadLocalTwo.set("SecondValueShould_NOT_CrossThreadBoundary");


        AtomicReference<List> reference = new AtomicReference();

        Runnable runnable = () -> reference.set(Arrays.asList(
            threadLocalOne.get(),
            threadLocalTwo.get()
        ));


        //Thread boundary
        CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(matches(() -> assertThat(reference.get()).containsExactlyInAnyOrder(
                null, null)));
    }
}
