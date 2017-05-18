package io.github.resilience4j.retry;

import io.github.resilience4j.retry.event.RetryEvent;
import io.github.resilience4j.retry.internal.AsyncRetryContext;
import io.reactivex.Flowable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A AsyncRetry instance is thread-safe can be used to decorate multiple requests.
 */
public interface AsyncRetry {

    /**
     * Returns the ID of this Retry.
     *
     * @return the ID of this Retry
     */
    String getName();

    /**
     * Returns a reactive stream of RetryEvents.
     *
     * @return a reactive stream of RetryEvents
     */
    Flowable<RetryEvent> getEventStream();

    /**
     * Creates a retry Context.
     *
     * @return the retry Context
     */
    AsyncRetry.Context context();

    /**
     * Returns the RetryConfig of this Retry.
     *
     * @return the RetryConfig of this Retry
     */
    RetryConfig getRetryConfig();

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param id the ID of the Retry
     * @param retryConfig a custom Retry configuration
     *
     * @return a Retry with a custom Retry configuration.
     */
    static AsyncRetry of(String id, RetryConfig retryConfig){
        return new AsyncRetryContext(id, retryConfig);
    }

    /**
     * Creates a Retry with a custom Retry configuration.
     *
     * @param id the ID of the Retry
     * @param retryConfigSupplier a supplier of a custom Retry configuration
     *
     * @return a Retry with a custom Retry configuration.
     */
    static AsyncRetry of(String id, Supplier<RetryConfig> retryConfigSupplier){
        return of(id, retryConfigSupplier.get());
    }

    /**
     * Creates a Retry with default configuration.
     *
     * @param id the ID of the Retry
     * @return a Retry with default configuration
     */
    static AsyncRetry ofDefaults(String id){
        return of(id, RetryConfig.ofDefaults());
    }

    /**
     * Decorates CompletionStageSupplier with Retry
     *
     * @param retry the retry context
     * @param scheduler execution service to use to schedule retries
     * @param supplier completion stage supplier
     * @param <T> type of completion stage result
     * @return decorated supplier
     */
    static <T> Supplier<CompletionStage<T>> decorateCompletionStage(
        AsyncRetry retry,
        ScheduledExecutorService scheduler,
        Supplier<CompletionStage<T>> supplier
    ) {
        return () -> {

            final CompletableFuture<T> promise = new CompletableFuture<>();
            final Runnable block = new AsyncRetryBlock<>(scheduler, retry.context(), supplier, promise);
            block.run();

            return promise;
        };
    }

    /**
     * Get the Metrics of this RateLimiter.
     *
     * @return the Metrics of this RateLimiter
     */
    Metrics getMetrics();

    interface Metrics {

        /**
         * Returns the number of successful calls without a retry attempt.
         *
         * @return the number of successful calls without a retry attempt
         */
        long getNumberOfSuccessfulCallsWithoutRetryAttempt();

        /**
         * Returns the number of failed calls without a retry attempt.
         *
         * @return the number of failed calls without a retry attempt
         */
        long getNumberOfFailedCallsWithoutRetryAttempt();

        /**
         * Returns the number of successful calls after a retry attempt.
         *
         * @return the number of successful calls after a retry attempt
         */
        long getNumberOfSuccessfulCallsWithRetryAttempt();

        /**
         * Returns the number of failed calls after all retry attempts.
         *
         * @return the number of failed calls after all retry attempts
         */
        long getNumberOfFailedCallsWithRetryAttempt();
    }

    interface Context {

        /**
         *  Records a successful call.
         */
        void onSuccess();

        /**
         * Records an failed call.
         * @param throwable the exception to handle
         * @return delay in milliseconds until the next try
         */
        long onError(Throwable throwable);
    }
}

class AsyncRetryBlock<T> implements Runnable {
    private final ScheduledExecutorService scheduler;
    private final AsyncRetry.Context retryContext;
    private final Supplier<CompletionStage<T>> supplier;
    private final CompletableFuture<T> promise;

    AsyncRetryBlock(
            ScheduledExecutorService scheduler,
            AsyncRetry.Context retryContext,
            Supplier<CompletionStage<T>> supplier,
            CompletableFuture<T> promise
    ) {
        this.scheduler = scheduler;
        this.retryContext = retryContext;
        this.supplier = supplier;
        this.promise = promise;
    }

    @Override
    public void run() {
        final CompletionStage<T> stage;

        try {
            stage = supplier.get();
        } catch (Throwable t) {
            onError(t);
            return;
        }

        stage.whenComplete((result, t) -> {
            if (t != null) {
                onError(t);
            } else {
                promise.complete(result);
                retryContext.onSuccess();
            }
        });
    }

    private void onError(Throwable t) {
        final long delay = retryContext.onError(t);

        if (delay < 1) {
            promise.completeExceptionally(t);
        } else {
            scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
        }
    }
}
