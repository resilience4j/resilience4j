package io.github.resilience4j.bulkhead.internal;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.GenericBulkhead;
import io.github.resilience4j.bulkhead.ThreadPoolBulkhead;
import io.github.resilience4j.bulkhead.event.BulkheadEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallFinishedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallPermittedEvent;
import io.github.resilience4j.bulkhead.event.BulkheadOnCallRejectedEvent;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * An ExecutorService-based Bulkhead implementation.
 */
public abstract class AbstractExecutorServiceBulkhead<T extends ExecutorService> implements GenericBulkhead {

    private static final String TAGS_MUST_NOT_BE_NULL = "Tags must not be null";

    protected final T executorService;
    protected final Map<String, String> tags;
    private final BulkheadEventProcessor eventProcessor;
    protected final String name;

    public AbstractExecutorServiceBulkhead(T executorService, String name, Map<String, String> tags) {
        this.executorService = executorService;
        this.eventProcessor = new BulkheadEventProcessor();
        this.name = name;
        this.tags = requireNonNull(tags, TAGS_MUST_NOT_BE_NULL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T2> CompletableFuture<T2> submit(Callable<T2> callable) {
        final CompletableFuture<T2> promise = new CompletableFuture<>();
        try {
            CompletableFuture.supplyAsync(decorate(() -> {
                try {
                    publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
                    return callable.call();
                } catch (CompletionException e) {
                    throw e;
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }), executorService).whenComplete((result, throwable) -> {
                publishBulkheadEvent(() -> new BulkheadOnCallFinishedEvent(name));
                if (throwable != null) {
                    promise.completeExceptionally(throwable);
                } else {
                    promise.complete(result);
                }
            });
        } catch (RejectedExecutionException rejected) {
            publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
            throw BulkheadFullException.createBulkheadFullException(name, isWritableStackTraceEnabled());
        }
        return promise;
    }

    protected <R> @Nonnull Supplier<R> decorate(Supplier<R> supplier) {
        return supplier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> submit(Runnable runnable) {
        final CompletableFuture<Void> promise = new CompletableFuture<>();
        try {
            CompletableFuture.runAsync(decorate(() -> {
                try {
                    publishBulkheadEvent(() -> new BulkheadOnCallPermittedEvent(name));
                    runnable.run();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }), executorService).whenComplete((result, throwable) -> {
                publishBulkheadEvent(() -> new BulkheadOnCallFinishedEvent(name));
                if (throwable != null) {
                    promise.completeExceptionally(throwable);
                } else {
                    promise.complete(result);
                }
            });
        } catch (RejectedExecutionException rejected) {
            publishBulkheadEvent(() -> new BulkheadOnCallRejectedEvent(name));
            throw BulkheadFullException.createBulkheadFullException(name, isWritableStackTraceEnabled());
        }
        return promise;
    }

    /**
     * Decorates the runnable, if necessary.
     *
     * @param runnable the runnable to decorate
     * @return the decorated runnable
     */
    protected Runnable decorate(Runnable runnable) {
        return runnable;
    }

    /**
     * Indicates if writable stack traces should be enabled on thrown exceptions.
     *
     * @return true if writable stack traces should be enabled on thrown exceptions
     */
    protected abstract boolean isWritableStackTraceEnabled();

    /**
     * Publishes a bulkhead event to an internal processor.
     *
     * @param eventSupplier the event supplier
     */
    protected void publishBulkheadEvent(Supplier<BulkheadEvent> eventSupplier) {
        if (eventProcessor.hasConsumers()) {
            eventProcessor.consumeEvent(eventSupplier.get());
        }
    }

    @Override
    public void close() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            if (!executorService.isTerminated()) {
                executorService.shutdownNow();
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ThreadPoolBulkhead.ThreadPoolBulkheadEventPublisher getEventPublisher() {
        return eventProcessor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getTags() {
        return tags;
    }
}
