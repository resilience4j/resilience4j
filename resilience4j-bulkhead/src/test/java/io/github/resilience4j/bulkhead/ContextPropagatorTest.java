package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.TestThreadLocalContextPropagator.TestThreadLocalContextHolder;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static com.jayway.awaitility.Awaitility.waitAtMost;

public class ContextPropagatorTest {

    @Test
    public void contextPropagationFailureTest() {

        TestThreadLocalContextHolder.put("ValueShouldNotCrossThreadBoundary");

        Supplier<Object> supplier = () -> TestThreadLocalContextHolder.get().orElse(null);

        //Thread boundary
        final CompletableFuture<Object> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> null == future.get());
    }

    @Test
    public void contextPropagationSupplierTest() {

        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");

        Supplier<Object> supplier = ContextPropagator
            .decorateSupplier(new TestThreadLocalContextPropagator(),
                () -> TestThreadLocalContextHolder.get().orElse(null));

        //Thread boundary
        final CompletableFuture<Object> future = CompletableFuture.supplyAsync(supplier);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> "ValueShouldCrossThreadBoundary" == future.get());
    }

    @Test
    public void contextPropagationRunnableTest() {

        TestThreadLocalContextHolder.put("ValueShouldCrossThreadBoundary");

        AtomicReference reference = new AtomicReference();

        Runnable runnable = ContextPropagator
            .decorateRunnable(new TestThreadLocalContextPropagator(),
                () -> reference.set(TestThreadLocalContextHolder.get().orElse(null)));

        //Thread boundary
        CompletableFuture<Void> future = CompletableFuture.runAsync(runnable);

        waitAtMost(5, TimeUnit.SECONDS)
            .until(() -> "ValueShouldCrossThreadBoundary" == reference.get());
    }
}
