package io.github.resilience4j.bulkhead;

import io.github.resilience4j.bulkhead.internal.ExecutorServiceBulkhead;
import io.github.resilience4j.core.lang.NonNull;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Generic Bulkhead contract and factory interface
 */
public interface GenericBulkhead extends AutoCloseable {

    static GenericBulkhead of(String name,
                              @NonNull ExecutorService executor,
                              @NonNull GenericBulkheadConfig config) {
        return new ExecutorServiceBulkhead(name, executor, config.isWritableStackTraceEnabled());
    }

    static GenericBulkhead of(String name,
                              Map<String, String> tags,
                              @NonNull ExecutorService executor,
                              @NonNull GenericBulkheadConfig config) {
        return new ExecutorServiceBulkhead(name, tags, executor, config.isWritableStackTraceEnabled());
    }

    /**
     * Returns a supplier which submits a value-returning task for execution and
     * returns a {@link CompletionStage} representing the pending results of the task.
     *
     * @param bulkhead the bulkhead
     * @param callable the value-returning task to submit
     * @param <T>      the result type of the callable
     * @return a supplier which submits a value-returning task for execution and returns a CompletionStage representing the pending
     * results of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     */
    static <T> Supplier<CompletionStage<T>> decorateCallable(GenericBulkhead bulkhead,
                                                             Callable<T> callable) {
        return () -> bulkhead.submit(callable);
    }

    /**
     * Returns a supplier which submits a value-returning task for execution
     * and returns a {@link CompletionStage} representing the pending results of the task.
     *
     * @param bulkhead the bulkhead
     * @param supplier the value-returning task to submit
     * @param <T>      the result type of the supplier
     * @return a supplier which submits a value-returning task for execution and returns a CompletionStage representing the pending
     * results of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     */
    static <T> Supplier<CompletionStage<T>> decorateSupplier(GenericBulkhead bulkhead,
                                                             Supplier<T> supplier) {
        return () -> bulkhead.submit(supplier::get);
    }

    /**
     * Returns a supplier which submits a task for execution and returns a {@link CompletionStage} representing the state of the task.
     *
     * @param bulkhead the bulkhead
     * @param runnable the to submit
     * @return a supplier which submits a task for execution to the ThreadPoolBulkhead
     * and returns a CompletionStage representing the state of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     */
    static Supplier<CompletionStage<Void>> decorateRunnable(GenericBulkhead bulkhead, Runnable runnable) {
        return () -> bulkhead.submit(runnable);
    }

    /**
     * Submits a value-returning task for execution and returns a {@link CompletionStage} representing the
     * asynchronous computation  of the task.
     *
     * @param task the value-returning task to submit
     * @param <T> the type of the task's result
     * @return CompletionStage representing the asynchronous computation of the task. The CompletionStage is completed exceptionally with a {@link BulkheadFullException}
     * when the task could not be submitted, because the Bulkhead was full
     * @throws BulkheadFullException if the task cannot be submitted, because the Bulkhead is full
     */
    <T> CompletionStage<T> submit(Callable<T> task);

    /**
     * Submits a task for execution to the ThreadPoolBulkhead and returns a {@link CompletionStage} representing the
     * asynchronous computation  of the task.
     *
     *
     * @param task the task to submit
     * @return CompletionStage representing the asynchronous computation of the task.
     * @throws BulkheadFullException if the task cannot be submitted, because the Bulkhead is full
     */
    CompletionStage<Void> submit(Runnable task);

    /**
     * Returns the name of this bulkhead.
     *
     * @return the name of this bulkhead
     */
    String getName();

    /**
     * Returns an unmodifiable map with tags assigned to this Retry.
     *
     * @return the tags assigned to this Retry in an unmodifiable map
     */
    Map<String, String> getTags();

    /**
     * Returns an EventPublisher which subscribes to the reactive stream of BulkheadEvent and can be
     * used to register event consumers.
     *
     * @return an EventPublisher
     */
    ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher getEventPublisher();

    /**
     * Returns a supplier which submits a value-returning task for execution and
     * returns a CompletionStage representing the asynchronous computation of the task.
     *
     * @param supplier the value-returning task to submit
     * @param <T>      the result type of the callable
     * @return a supplier which submits a value-returning task for execution and returns a CompletionStage representing
     * the asynchronous computation of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     */
    default <T> Supplier<CompletionStage<T>> decorateSupplier(Supplier<T> supplier) {
        return decorateSupplier(this, supplier);
    }

    /**
     * Returns a supplier which submits a value-returning task for execution and
     * returns a CompletionStage representing the asynchronous computation of the task.
     *
     * @param callable the value-returning task to submit
     * @param <T>      the result type of the callable
     * @return a supplier which submits a value-returning task for execution and returns a CompletionStage representing
     * the asynchronous computation of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     */
    default <T> Supplier<CompletionStage<T>> decorateCallable(Callable<T> callable) {
        return decorateCallable(this, callable);
    }

    /**
     * Returns a supplier which submits a task for execution and returns a {@link CompletionStage} representing the
     * asynchronous computation of the task.
     *
     * @param runnable the task to submit
     * @return a supplier which submits a task for execution and returns a CompletionStage representing
     * the asynchronous computation of the task
     * @throws BulkheadFullException if the task cannot be submitted because the Bulkhead is full
     */
    default Supplier<CompletionStage<Void>> decorateRunnable(Runnable runnable) {
        return decorateRunnable(this, runnable);
    }

    /**
     * Submits a value-returning task for execution and returns a {@link CompletionStage} representing the
     * asynchronous computation of the task.
     *
     * @param supplier the value-returning task to submit
     * @param <T> the type of the task's result
     * @return a CompletionStage representing the asynchronous computation of the task.
     * @throws BulkheadFullException if the task cannot be submitted, because the Bulkhead is full
     */
    default <T> CompletionStage<T> executeSupplier(Supplier<T> supplier) {
        return decorateSupplier(this, supplier).get();
    }

    /**
     * Submits a value-returning task for execution and returns a {@link CompletionStage} representing the
     * asynchronous computation  of the task.
     *
     * @param callable the value-returning task to submit
     * @param <T>      the result type of the Callable
     * @return a {@link CompletionStage} representing the asynchronous computation of the task.
     * @throws BulkheadFullException if the task cannot be submitted, because the Bulkhead is full
     */
    default <T> CompletionStage<T> executeCallable(Callable<T> callable) {
        return decorateCallable(this, callable).get();
    }

    /**
     * Submits a task for execution and returns a {@link CompletionStage} representing the
     * asynchronous computation  of the task.
     *
     * @param runnable the task to submit
     * @return CompletionStage representing the asynchronous computation of the task.
     * @throws BulkheadFullException if the task cannot be submitted, because the Bulkhead is full
     */
    default CompletionStage<Void> executeRunnable(Runnable runnable) {
        return decorateRunnable(this, runnable).get();
    }
}
